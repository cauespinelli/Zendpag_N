// @ts-nocheck
import { useAppStore, useOffline } from '@/store/appStore';
import { syncPendingMutations, queryClient } from '@/config/queryClient';

// Service Worker registration
export const registerServiceWorker = async (): Promise<ServiceWorkerRegistration | null> => {
  if ('serviceWorker' in navigator) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js');
      console.log('Service Worker registered successfully:', registration);
      return registration;
    } catch (error) {
      console.error('Service Worker registration failed:', error);
      return null;
    }
  }
  return null;
};

// Online/Offline detection
export class NetworkMonitor {
  private static instance: NetworkMonitor;
  private listeners: Array<(online: boolean) => void> = [];
  private isOnline: boolean = navigator.onLine;

  public static getInstance(): NetworkMonitor {
    if (!NetworkMonitor.instance) {
      NetworkMonitor.instance = new NetworkMonitor();
    }
    return NetworkMonitor.instance;
  }

  constructor() {
    this.setupEventListeners();
  }

  private setupEventListeners(): void {
    window.addEventListener('online', this.handleOnline.bind(this));
    window.addEventListener('offline', this.handleOffline.bind(this));

    // Additional checks for mobile devices
    document.addEventListener('visibilitychange', this.handleVisibilityChange.bind(this));
  }

  private handleOnline(): void {
    this.isOnline = true;
    this.notifyListeners(true);
    this.handleConnectionRestored();
  }

  private handleOffline(): void {
    this.isOnline = false;
    this.notifyListeners(false);
    this.handleConnectionLost();
  }

  private handleVisibilityChange(): void {
    if (!document.hidden && navigator.onLine !== this.isOnline) {
      this.isOnline = navigator.onLine;
      this.notifyListeners(this.isOnline);

      if (this.isOnline) {
        this.handleConnectionRestored();
      } else {
        this.handleConnectionLost();
      }
    }
  }

  private notifyListeners(online: boolean): void {
    this.listeners.forEach(listener => listener(online));
  }

  private async handleConnectionRestored(): Promise<void> {
    const { setOfflineMode, addNotification } = useAppStore.getState();

    console.log('Connection restored - switching to online mode');

    // Set online mode
    setOfflineMode(false);

    // Show notification
    addNotification({
      type: 'success',
      title: 'Conexão restabelecida',
      message: 'Sincronizando dados...',
      duration: 3000,
    });

    // Sync pending mutations
    try {
      await syncPendingMutations();

      // Refetch important queries
      await queryClient.refetchQueries({
        predicate: (query) => {
          return query.meta?.refetchOnReconnect === true;
        }
      });

      addNotification({
        type: 'success',
        title: 'Sincronização concluída',
        message: 'Todos os dados foram sincronizados com sucesso.',
        duration: 3000,
      });
    } catch (error) {
      console.error('Failed to sync data after reconnection:', error);

      addNotification({
        type: 'warning',
        title: 'Erro na sincronização',
        message: 'Alguns dados podem não estar atualizados.',
        duration: 5000,
      });
    }
  }

  private handleConnectionLost(): void {
    const { setOfflineMode, addNotification } = useAppStore.getState();

    console.log('Connection lost - switching to offline mode');

    // Set offline mode
    setOfflineMode(true);

    // Show notification
    addNotification({
      type: 'warning',
      title: 'Conexão perdida',
      message: 'Modo offline ativado. Suas ações serão sincronizadas quando a conexão for restabelecida.',
      duration: 0, // Don't auto-hide
    });
  }

  public addListener(callback: (online: boolean) => void): () => void {
    this.listeners.push(callback);

    // Return unsubscribe function
    return () => {
      const index = this.listeners.indexOf(callback);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  public isOnlineNow(): boolean {
    return this.isOnline;
  }

  // Test network connectivity with actual API call
  public async testConnectivity(): Promise<boolean> {
    try {
      const response = await fetch('/api/v1/health', {
        method: 'GET',
        cache: 'no-cache',
        signal: AbortSignal.timeout(5000), // 5 second timeout
      });
      return response.ok;
    } catch {
      return false;
    }
  }
}

// Cache management
export class OfflineCacheManager {
  private static instance: OfflineCacheManager;
  private dbName = 'zendapag-cache';
  private dbVersion = 1;
  private db: IDBDatabase | null = null;

  public static getInstance(): OfflineCacheManager {
    if (!OfflineCacheManager.instance) {
      OfflineCacheManager.instance = new OfflineCacheManager();
    }
    return OfflineCacheManager.instance;
  }

  public async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.dbVersion);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;

        // Create object stores
        if (!db.objectStoreNames.contains('queries')) {
          const queryStore = db.createObjectStore('queries', { keyPath: 'key' });
          queryStore.createIndex('timestamp', 'timestamp');
          queryStore.createIndex('staleTime', 'staleTime');
        }

        if (!db.objectStoreNames.contains('mutations')) {
          const mutationStore = db.createObjectStore('mutations', { keyPath: 'id' });
          mutationStore.createIndex('timestamp', 'timestamp');
        }

        if (!db.objectStoreNames.contains('assets')) {
          const assetStore = db.createObjectStore('assets', { keyPath: 'url' });
          assetStore.createIndex('contentType', 'contentType');
          assetStore.createIndex('timestamp', 'timestamp');
        }
      };
    });
  }

  // Query caching
  public async cacheQuery(key: string, data: any, staleTime: number = 5 * 60 * 1000): Promise<void> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['queries'], 'readwrite');
    const store = transaction.objectStore('queries');

    await store.put({
      key,
      data,
      timestamp: Date.now(),
      staleTime: Date.now() + staleTime,
    });
  }

  public async getCachedQuery(key: string): Promise<any | null> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['queries'], 'readonly');
    const store = transaction.objectStore('queries');

    return new Promise((resolve, reject) => {
      const request = store.get(key);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const result = request.result;

        if (!result) {
          resolve(null);
          return;
        }

        // Check if data is still fresh
        if (Date.now() > result.staleTime) {
          resolve(null);
          return;
        }

        resolve(result.data);
      };
    });
  }

  // Pending mutation storage
  public async storePendingMutation(id: string, mutationFn: string, variables: any): Promise<void> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['mutations'], 'readwrite');
    const store = transaction.objectStore('mutations');

    await store.put({
      id,
      mutationFn,
      variables,
      timestamp: Date.now(),
      retryCount: 0,
    });
  }

  public async getPendingMutations(): Promise<any[]> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['mutations'], 'readonly');
    const store = transaction.objectStore('mutations');

    return new Promise((resolve, reject) => {
      const request = store.getAll();
      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);
    });
  }

  public async removePendingMutation(id: string): Promise<void> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['mutations'], 'readwrite');
    const store = transaction.objectStore('mutations');

    await store.delete(id);
  }

  // Asset caching
  public async cacheAsset(url: string, blob: Blob, contentType: string): Promise<void> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['assets'], 'readwrite');
    const store = transaction.objectStore('assets');

    await store.put({
      url,
      blob,
      contentType,
      timestamp: Date.now(),
    });
  }

  public async getCachedAsset(url: string): Promise<Blob | null> {
    if (!this.db) await this.init();

    const transaction = this.db!.transaction(['assets'], 'readonly');
    const store = transaction.objectStore('assets');

    return new Promise((resolve, reject) => {
      const request = store.get(url);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const result = request.result;
        resolve(result ? result.blob : null);
      };
    });
  }

  // Cleanup old data
  public async cleanup(maxAge: number = 7 * 24 * 60 * 60 * 1000): Promise<void> {
    if (!this.db) await this.init();

    const cutoff = Date.now() - maxAge;
    const transaction = this.db!.transaction(['queries', 'assets'], 'readwrite');

    // Clean queries
    const queryStore = transaction.objectStore('queries');
    const queryIndex = queryStore.index('timestamp');
    const queryRange = IDBKeyRange.upperBound(cutoff);
    await queryIndex.openCursor(queryRange)?.delete();

    // Clean assets
    const assetStore = transaction.objectStore('assets');
    const assetIndex = assetStore.index('timestamp');
    const assetRange = IDBKeyRange.upperBound(cutoff);
    await assetIndex.openCursor(assetRange)?.delete();
  }
}

// React hooks for offline functionality
export const useNetworkStatus = () => {
  const [isOnline, setIsOnline] = React.useState(navigator.onLine);
  const networkMonitor = NetworkMonitor.getInstance();

  React.useEffect(() => {
    const unsubscribe = networkMonitor.addListener(setIsOnline);
    return unsubscribe;
  }, [networkMonitor]);

  const testConnectivity = React.useCallback(async () => {
    const result = await networkMonitor.testConnectivity();
    setIsOnline(result);
    return result;
  }, [networkMonitor]);

  return {
    isOnline,
    isOffline: !isOnline,
    testConnectivity,
  };
};

export const useOfflineSync = () => {
  const { pendingActions, clearPendingActions } = useOffline();
  const [syncing, setSyncing] = React.useState(false);

  const sync = React.useCallback(async () => {
    if (syncing || pendingActions.length === 0) return;

    setSyncing(true);
    try {
      await syncPendingMutations();
      clearPendingActions();
    } catch (error) {
      console.error('Offline sync failed:', error);
    } finally {
      setSyncing(false);
    }
  }, [syncing, pendingActions, clearPendingActions]);

  return {
    pendingCount: pendingActions.length,
    syncing,
    sync,
  };
};

// Initialize offline support
export const initializeOfflineSupport = async (): Promise<void> => {
  try {
    // Register service worker
    await registerServiceWorker();

    // Initialize network monitor
    NetworkMonitor.getInstance();

    // Initialize cache manager
    const cacheManager = OfflineCacheManager.getInstance();
    await cacheManager.init();

    // Cleanup old cache data
    await cacheManager.cleanup();

    console.log('Offline support initialized successfully');
  } catch (error) {
    console.error('Failed to initialize offline support:', error);
  }
};

// PWA installation
export const usePWAInstall = () => {
  const [deferredPrompt, setDeferredPrompt] = React.useState<any>(null);
  const [canInstall, setCanInstall] = React.useState(false);

  React.useEffect(() => {
    const handleBeforeInstallPrompt = (event: any) => {
      event.preventDefault();
      setDeferredPrompt(event);
      setCanInstall(true);
    };

    const handleAppInstalled = () => {
      setDeferredPrompt(null);
      setCanInstall(false);
      console.log('PWA was installed');
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const install = React.useCallback(async () => {
    if (!deferredPrompt) return false;

    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;

    if (outcome === 'accepted') {
      setDeferredPrompt(null);
      setCanInstall(false);
      return true;
    }

    return false;
  }, [deferredPrompt]);

  return {
    canInstall,
    install,
  };
};

// React import fix
import React from 'react';