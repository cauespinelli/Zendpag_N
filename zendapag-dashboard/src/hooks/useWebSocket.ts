// @ts-nocheck
import { useEffect, useRef, useState, useCallback } from 'react';
import { message } from 'antd';
import { useQueryClient } from '@tanstack/react-query';
import { useAppStore, useWebSocketStore } from '@/store/appStore';
import { queryKeys } from '@/config/queryClient';
import type { Payment, DashboardStats } from '@/types';

interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: string;
}

interface UseWebSocketOptions {
  enabled?: boolean;
  autoReconnect?: boolean;
  maxReconnectAttempts?: number;
  reconnectInterval?: number;
  onMessage?: (message: WebSocketMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Event) => void;
}

interface WebSocketState {
  connected: boolean;
  connecting: boolean;
  error: string | null;
  reconnectAttempts: number;
  lastMessage: WebSocketMessage | null;
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const {
    enabled = true,
    autoReconnect = true,
    maxReconnectAttempts = 5,
    reconnectInterval = 3000,
    onMessage,
    onConnect,
    onDisconnect,
    onError,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const { user, isAuthenticated } = useAppStore(state => ({
    user: state.user,
    isAuthenticated: state.isAuthenticated
  }));
  const {
    connected: wsConnected,
    setConnected,
    incrementReconnectAttempts,
    resetReconnectAttempts,
    updateHeartbeat,
    subscriptions,
  } = useWebSocketStore();
  const handleRealtimeUpdate = useAppStore(state => state.handleRealtimeUpdate);
  const queryClient = useQueryClient();

  const [state, setState] = useState<WebSocketState>({
    connected: false,
    connecting: false,
    error: null,
    reconnectAttempts: 0,
    lastMessage: null,
  });

  const getWebSocketUrl = useCallback(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = process.env.REACT_APP_WS_URL || window.location.host;
    return `${protocol}//${host}/api/v1/ws`;
  }, []);

  const clearTimeouts = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
  }, []);

  const startHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }

    heartbeatIntervalRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: 'ping' }));
      }
    }, 30000); // Send ping every 30 seconds
  }, []);

  const handleMessage = useCallback((event: MessageEvent) => {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);

      setState(prev => ({ ...prev, lastMessage: message }));

      // Handle different message types
      switch (message.type) {
        case 'payment.created':
        case 'payment.updated':
        case 'payment.completed':
        case 'payment.failed':
        case 'payment.cancelled':
          // Use global state handler
          handleRealtimeUpdate({
            type: 'payment_updated',
            data: message.data,
          });

          // Invalidate payments queries
          queryClient.invalidateQueries({ queryKey: queryKeys.payments.all });
          queryClient.invalidateQueries({ queryKey: queryKeys.analytics.dashboard('7d') });

          // Update specific payment if we have the ID
          if (message.data.id) {
            queryClient.setQueryData(
              queryKeys.payments.detail(message.data.id),
              (oldData: Payment | undefined) => {
                return oldData ? { ...oldData, ...message.data } : message.data;
              }
            );
          }

          // Show notification for completed payments
          if (message.type === 'payment.completed') {
            message.success(`Pagamento de R$ ${message.data.amount} concluído!`);
          }
          break;

        case 'dashboard.stats':
          handleRealtimeUpdate({
            type: 'analytics_update',
            data: message.data,
          });
          queryClient.setQueryData(queryKeys.analytics.dashboard('7d'), message.data);
          break;

        case 'webhook.event':
        case 'webhook.delivery':
          handleRealtimeUpdate({
            type: 'webhook_delivery',
            data: message.data,
          });
          queryClient.invalidateQueries({ queryKey: queryKeys.webhooks.all });
          break;

        case 'pong':
          // Heartbeat response, connection is alive
          updateHeartbeat();
          break;

        case 'auth_success':
          console.log('WebSocket authenticated successfully');
          break;

        case 'subscription_confirmed':
          console.log(`Subscribed to channel: ${message.data.channel}`);
          break;

        default:
          console.log('Unknown WebSocket message type:', message.type);
      }

      // Call custom message handler
      onMessage?.(message);

    } catch (error) {
      console.error('Error parsing WebSocket message:', error);
    }
  }, [queryClient, onMessage]);

  const connect = useCallback(() => {
    if (!enabled || !isAuthenticated || !user) {
      return;
    }

    if (wsRef.current?.readyState === WebSocket.CONNECTING) {
      return;
    }

    setState(prev => ({ ...prev, connecting: true, error: null }));

    try {
      const wsUrl = getWebSocketUrl();
      const token = localStorage.getItem('auth_token');
      wsRef.current = new WebSocket(`${wsUrl}?token=${token}`);

      wsRef.current.onopen = () => {
        setState(prev => ({
          ...prev,
          connected: true,
          connecting: false,
          reconnectAttempts: 0,
          error: null,
        }));

        setConnected(true);
        resetReconnectAttempts();
        startHeartbeat();

        // Authenticate with user info
        if (user && token) {
          wsRef.current?.send(JSON.stringify({
            type: 'authenticate',
            token,
            userId: user.id,
          }));
        }

        // Re-subscribe to all channels
        subscriptions.forEach(channel => {
          wsRef.current?.send(JSON.stringify({
            type: 'subscribe',
            channel,
          }));
        });

        onConnect?.();
        console.log('WebSocket connected');
      };

      wsRef.current.onmessage = handleMessage;

      wsRef.current.onclose = (event) => {
        setState(prev => ({ ...prev, connected: false, connecting: false }));
        setConnected(false);
        clearTimeouts();
        onDisconnect?.();

        console.log('WebSocket disconnected:', event.code, event.reason);

        // Attempt to reconnect if enabled and not a normal closure
        if (autoReconnect && event.code !== 1000 && state.reconnectAttempts < maxReconnectAttempts) {
          setState(prev => ({ ...prev, reconnectAttempts: prev.reconnectAttempts + 1 }));
          incrementReconnectAttempts();

          const delay = reconnectInterval * Math.pow(1.5, state.reconnectAttempts);
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, delay);
        }
      };

      wsRef.current.onerror = (error) => {
        setState(prev => ({
          ...prev,
          error: 'Erro de conexão WebSocket',
          connecting: false,
        }));
        setConnected(false);
        onError?.(error);
        console.error('WebSocket error:', error);
      };

    } catch (error) {
      setState(prev => ({
        ...prev,
        error: 'Falha ao conectar WebSocket',
        connecting: false,
      }));
      console.error('WebSocket connection error:', error);
    }
  }, [
    enabled,
    isAuthenticated,
    token,
    autoReconnect,
    maxReconnectAttempts,
    reconnectInterval,
    state.reconnectAttempts,
    getWebSocketUrl,
    handleMessage,
    startHeartbeat,
    onConnect,
    onDisconnect,
    onError,
  ]);

  const disconnect = useCallback(() => {
    clearTimeouts();

    if (wsRef.current) {
      wsRef.current.close(1000, 'User disconnected');
      wsRef.current = null;
    }

    setState({
      connected: false,
      connecting: false,
      error: null,
      reconnectAttempts: 0,
      lastMessage: null,
    });
    setConnected(false);
    resetReconnectAttempts();
  }, [clearTimeouts, setConnected, resetReconnectAttempts]);

  const reconnect = useCallback(() => {
    disconnect();
    setState(prev => ({ ...prev, reconnectAttempts: 0 }));
    setTimeout(connect, 100);
  }, [connect, disconnect]);

  const sendMessage = useCallback((message: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
      return true;
    }
    return false;
  }, []);

  // Add subscribe/unsubscribe functions
  const subscribe = useCallback((channel: string) => {
    const { addSubscription } = useWebSocketStore.getState();
    addSubscription(channel);

    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'subscribe',
        channel,
      }));
    }
  }, []);

  const unsubscribe = useCallback((channel: string) => {
    const { removeSubscription } = useWebSocketStore.getState();
    removeSubscription(channel);

    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        type: 'unsubscribe',
        channel,
      }));
    }
  }, []);

  // Connect when enabled and authenticated
  useEffect(() => {
    if (enabled && isAuthenticated && user) {
      connect();
    } else {
      disconnect();
    }

    return () => {
      disconnect();
    };
  }, [enabled, isAuthenticated, user]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      clearTimeouts();
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [clearTimeouts]);

  return {
    ...state,
    connected: state.connected || wsConnected,
    connect,
    disconnect,
    reconnect,
    sendMessage,
    subscribe,
    unsubscribe,
  };
};

// Hook for real-time payment updates
export const usePaymentUpdates = () => {
  const [recentUpdates, setRecentUpdates] = useState<Payment[]>([]);

  const { lastMessage } = useWebSocket({
    enabled: true,
    onMessage: (message) => {
      if (message.type.startsWith('payment.')) {
        setRecentUpdates(prev => [message.data, ...prev.slice(0, 9)]); // Keep last 10 updates
      }
    },
  });

  return {
    recentUpdates,
    clearUpdates: () => setRecentUpdates([]),
  };
};

// Hook for dashboard real-time stats
export const useDashboardUpdates = () => {
  const [statsUpdated, setStatsUpdated] = useState(false);

  useWebSocket({
    enabled: true,
    onMessage: (message) => {
      if (message.type === 'dashboard.stats') {
        setStatsUpdated(true);
        // Reset flag after 3 seconds
        setTimeout(() => setStatsUpdated(false), 3000);
      }
    },
  });

  return { statsUpdated };
};

export default useWebSocket;