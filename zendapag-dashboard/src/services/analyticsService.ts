// @ts-nocheck
import apiService from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type { DashboardStats, AnalyticsData } from '@/types';

export interface AnalyticsFilters {
  period?: string;
  startDate?: string;
  endDate?: string;
  groupBy?: 'hour' | 'day' | 'week' | 'month';
  currency?: string;
  paymentMethod?: string;
  customerSegment?: string;
}

export interface RevenueMetrics {
  totalRevenue: number;
  netRevenue: number;
  fees: number;
  refunds: number;
  chargebacks: number;
  growth: {
    revenue: number;
    transactions: number;
    averageTicket: number;
  };
  monthlyRecurring: number;
  projectedRevenue: number;
}

export interface CustomerMetrics {
  totalCustomers: number;
  newCustomers: number;
  returningCustomers: number;
  customerLifetimeValue: number;
  acquisitionCost: number;
  retentionRate: number;
  churnRate: number;
  segments: Array<{
    segment: string;
    count: number;
    percentage: number;
    averageTicket: number;
  }>;
}

export interface PerformanceMetrics {
  averageResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  errorRate: number;
  successRate: number;
  uptime: number;
  throughput: number;
  peakThroughput: number;
  systemHealth: {
    api: 'healthy' | 'degraded' | 'down';
    database: 'healthy' | 'degraded' | 'down';
    payments: 'healthy' | 'degraded' | 'down';
    webhooks: 'healthy' | 'degraded' | 'down';
  };
}

export interface FraudMetrics {
  totalAttempts: number;
  blockedAttempts: number;
  falsePositives: number;
  falseNegatives: number;
  riskScore: {
    low: number;
    medium: number;
    high: number;
    critical: number;
  };
  topRiskFactors: Array<{
    factor: string;
    score: number;
    occurrences: number;
  }>;
  geographicDistribution: Array<{
    country: string;
    attempts: number;
    blocked: number;
    riskLevel: 'low' | 'medium' | 'high';
  }>;
}

export interface ComparisonData {
  current: AnalyticsData;
  previous: AnalyticsData;
  change: {
    transactions: number;
    revenue: number;
    successRate: number;
    averageTicket: number;
  };
  trends: {
    transactions: 'up' | 'down' | 'stable';
    revenue: 'up' | 'down' | 'stable';
    successRate: 'up' | 'down' | 'stable';
  };
}

class AnalyticsService {
  async getDashboardStats(period: string = '30d'): Promise<DashboardStats> {
    return apiService.get<DashboardStats>(API_ENDPOINTS.ANALYTICS.DASHBOARD, {
      params: { period },
    });
  }

  async getTransactionAnalytics(filters: AnalyticsFilters = {}): Promise<AnalyticsData> {
    return apiService.get<AnalyticsData>(API_ENDPOINTS.ANALYTICS.TRANSACTIONS, {
      params: filters,
    });
  }

  async getRevenueAnalytics(filters: AnalyticsFilters = {}): Promise<AnalyticsData> {
    return apiService.get<AnalyticsData>(API_ENDPOINTS.ANALYTICS.REVENUE, {
      params: filters,
    });
  }

  async getRevenueMetrics(period: string = '30d'): Promise<RevenueMetrics> {
    return apiService.get<RevenueMetrics>('/analytics/revenue/metrics', {
      params: { period },
    });
  }

  async getCustomerMetrics(period: string = '30d'): Promise<CustomerMetrics> {
    return apiService.get<CustomerMetrics>('/analytics/customers', {
      params: { period },
    });
  }

  async getPerformanceMetrics(period: string = '24h'): Promise<PerformanceMetrics> {
    return apiService.get<PerformanceMetrics>('/analytics/performance', {
      params: { period },
    });
  }

  async getFraudMetrics(period: string = '30d'): Promise<FraudMetrics> {
    return apiService.get<FraudMetrics>('/analytics/fraud', {
      params: { period },
    });
  }

  async getComparisonData(
    currentPeriod: string,
    comparisonPeriod: string
  ): Promise<ComparisonData> {
    return apiService.get<ComparisonData>('/analytics/comparison', {
      params: { currentPeriod, comparisonPeriod },
    });
  }

  async getPaymentMethodAnalytics(period: string = '30d'): Promise<{
    methods: Array<{
      method: string;
      count: number;
      percentage: number;
      revenue: number;
      successRate: number;
      averageTicket: number;
    }>;
    trends: Array<{
      date: string;
      methods: Record<string, number>;
    }>;
  }> {
    return apiService.get<any>('/analytics/payment-methods', {
      params: { period },
    });
  }

  async getGeographicAnalytics(period: string = '30d'): Promise<{
    countries: Array<{
      country: string;
      countryCode: string;
      transactions: number;
      revenue: number;
      percentage: number;
      growth: number;
    }>;
    regions: Array<{
      region: string;
      transactions: number;
      revenue: number;
      percentage: number;
    }>;
    mapData: Array<{
      country: string;
      value: number;
      color: string;
    }>;
  }> {
    return apiService.get<any>('/analytics/geographic', {
      params: { period },
    });
  }

  async getCohortAnalytics(
    cohortType: 'weekly' | 'monthly' = 'monthly',
    periods: number = 12
  ): Promise<{
    cohorts: Array<{
      cohort: string;
      size: number;
      retention: number[];
    }>;
    averageRetention: number[];
    insights: {
      bestPerformingCohort: string;
      worstPerformingCohort: string;
      overallTrend: 'improving' | 'declining' | 'stable';
    };
  }> {
    return apiService.get<any>('/analytics/cohorts', {
      params: { cohortType, periods },
    });
  }

  async getFunnelAnalytics(period: string = '30d'): Promise<{
    steps: Array<{
      step: string;
      users: number;
      dropoffRate: number;
      conversionRate: number;
    }>;
    totalConversionRate: number;
    bottlenecks: Array<{
      step: string;
      impact: 'high' | 'medium' | 'low';
      recommendation: string;
    }>;
  }> {
    return apiService.get<any>('/analytics/funnel', {
      params: { period },
    });
  }

  async getABTestResults(testId?: string): Promise<{
    tests: Array<{
      id: string;
      name: string;
      status: 'running' | 'completed' | 'paused';
      variants: Array<{
        name: string;
        traffic: number;
        conversions: number;
        conversionRate: number;
        revenue: number;
        significance: number;
        winner: boolean;
      }>;
      metrics: Array<{
        metric: string;
        improvement: number;
        confidence: number;
      }>;
    }>;
  }> {
    const params = testId ? { testId } : {};
    return apiService.get<any>('/analytics/ab-tests', { params });
  }

  async getRealtimeMetrics(): Promise<{
    liveTransactions: number;
    transactionsPerMinute: number;
    revenue: number;
    revenuePerMinute: number;
    activeUsers: number;
    errorRate: number;
    averageResponseTime: number;
    queueSize: number;
    systemLoad: number;
  }> {
    return apiService.get<any>('/analytics/realtime');
  }

  async getPredictiveAnalytics(
    metric: 'revenue' | 'transactions' | 'customers',
    horizon: number = 30
  ): Promise<{
    predictions: Array<{
      date: string;
      predicted: number;
      lower: number;
      upper: number;
      confidence: number;
    }>;
    accuracy: number;
    factors: Array<{
      factor: string;
      importance: number;
      trend: 'positive' | 'negative' | 'neutral';
    }>;
    recommendations: string[];
  }> {
    return apiService.get<any>('/analytics/predictions', {
      params: { metric, horizon },
    });
  }

  async getCustomReport(
    name: string,
    metrics: string[],
    dimensions: string[],
    filters: Record<string, any> = {},
    dateRange: { start: string; end: string }
  ): Promise<{
    data: Array<Record<string, any>>;
    summary: Record<string, number>;
    charts: Array<{
      type: 'line' | 'bar' | 'pie';
      data: any;
      config: any;
    }>;
  }> {
    return apiService.post<any>('/analytics/custom-report', {
      name,
      metrics,
      dimensions,
      filters,
      dateRange,
    });
  }

  async exportAnalytics(
    type: 'dashboard' | 'transactions' | 'revenue' | 'custom',
    format: 'CSV' | 'PDF' | 'EXCEL' = 'CSV',
    filters: AnalyticsFilters = {}
  ): Promise<void> {
    const params = { ...filters, format };
    return apiService.downloadFile(
      `/analytics/${type}/export`,
      `analytics_${type}_${new Date().toISOString().split('T')[0]}.${format.toLowerCase()}`,
      { params }
    );
  }

  async scheduleReport(config: {
    name: string;
    type: 'dashboard' | 'transactions' | 'revenue';
    recipients: string[];
    frequency: 'daily' | 'weekly' | 'monthly';
    format: 'PDF' | 'CSV';
    filters?: AnalyticsFilters;
    enabled?: boolean;
  }): Promise<{
    id: string;
    nextRun: string;
    status: 'active' | 'paused';
  }> {
    return apiService.post<any>('/analytics/scheduled-reports', config);
  }

  async getScheduledReports(): Promise<Array<{
    id: string;
    name: string;
    type: string;
    frequency: string;
    recipients: string[];
    lastRun?: string;
    nextRun: string;
    status: 'active' | 'paused';
  }>> {
    return apiService.get<any>('/analytics/scheduled-reports');
  }

  async updateScheduledReport(
    id: string,
    config: Partial<{
      name: string;
      recipients: string[];
      frequency: 'daily' | 'weekly' | 'monthly';
      enabled: boolean;
    }>
  ): Promise<void> {
    return apiService.put(`/analytics/scheduled-reports/${id}`, config);
  }

  async deleteScheduledReport(id: string): Promise<void> {
    return apiService.delete(`/analytics/scheduled-reports/${id}`);
  }
}

export const analyticsService = new AnalyticsService();
export default analyticsService;