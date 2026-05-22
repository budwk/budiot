import { request } from '@/utils/request';
import type { VideoModuleOverview, VideoModuleSummary } from './typing';

const API_VIDEO_MODULE_SUMMARY = '/platform/video/module/summary';
const API_VIDEO_MODULE_OVERVIEW = '/platform/video/module/overview';

export const getVideoModuleSummary = async () =>
  request<VideoModuleSummary>(API_VIDEO_MODULE_SUMMARY, {
    method: 'GET',
  });

export const getVideoModuleOverview = async () =>
  request<VideoModuleOverview>(API_VIDEO_MODULE_OVERVIEW, {
    method: 'GET',
  });
