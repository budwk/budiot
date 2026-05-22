import { request } from '@/utils/request';
import type { VideoAlarmRecord, VideoPagedList } from './typing';

const API_VIDEO_ALARM_LIST = '/platform/video/alarm/list';
const API_VIDEO_ALARM_GET = '/platform/video/alarm/get/';
const API_VIDEO_ALARM_HANDLE = '/platform/video/alarm/handle/';

export const getVideoAlarmPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoAlarmRecord>>(API_VIDEO_ALARM_LIST, {
    method: 'POST',
    data,
  });

export const getVideoAlarmDetail = async (id: string) =>
  request<VideoAlarmRecord>(`${API_VIDEO_ALARM_GET}${id}`, {
    method: 'GET',
  });

export const handleVideoAlarm = async (id: string, data: Record<string, unknown>) =>
  request<VideoAlarmRecord>(`${API_VIDEO_ALARM_HANDLE}${id}`, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
