import { request } from '@/utils/request';
import type { VideoMonitorDashboardData, VideoMonitorRealtimeData } from './typing';

const API_VIDEO_MONITOR_DASHBOARD_DATA = '/platform/video/monitor/dashboard/data';
const API_VIDEO_MONITOR_DASHBOARD_RUNTIME = '/platform/video/monitor/dashboard/runtime';

export const getVideoMonitorDashboardData = async () =>
  request<VideoMonitorDashboardData>(API_VIDEO_MONITOR_DASHBOARD_DATA, {
    method: 'GET',
  });

export const getVideoMonitorDashboardRuntime = async (cameraIds: string[]) =>
  request<VideoMonitorRealtimeData>(API_VIDEO_MONITOR_DASHBOARD_RUNTIME, {
    method: 'POST',
    data: { cameraIds },
  });
