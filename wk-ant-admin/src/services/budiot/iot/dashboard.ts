import { request } from '@/utils/request';
import type { IotDashboardData } from './typing';

const API_IOT_DASHBOARD_DATA = '/platform/iot/dashboard/data';

export const getIotDashboardData = async (params?: Record<string, unknown>) =>
  request<IotDashboardData>(API_IOT_DASHBOARD_DATA, {
    method: 'GET',
    params,
  });
