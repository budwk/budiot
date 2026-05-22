import { request } from '@/utils/request';
import type { VideoPagedList, VideoWorkOrderRecord } from './typing';

const API_VIDEO_WORKORDER_LIST = '/platform/video/workorder/list';
const API_VIDEO_WORKORDER_GET = '/platform/video/workorder/get/';
const API_VIDEO_WORKORDER_REPLY = '/platform/video/workorder/reply/';

export const getVideoWorkOrderPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoWorkOrderRecord>>(API_VIDEO_WORKORDER_LIST, {
    method: 'POST',
    data,
  });

export const getVideoWorkOrderDetail = async (id: string) =>
  request<VideoWorkOrderRecord>(`${API_VIDEO_WORKORDER_GET}${id}`, {
    method: 'GET',
  });

export const replyVideoWorkOrder = async (id: string, reply: string) =>
  request<VideoWorkOrderRecord>(`${API_VIDEO_WORKORDER_REPLY}${id}`, {
    method: 'POST',
    params: { reply },
    showSuccessMessage: true,
  });
