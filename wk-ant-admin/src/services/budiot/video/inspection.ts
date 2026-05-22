import { request } from '@/utils/request';
import type { VideoCameraQueryParams, VideoInspectionRecord, VideoPagedList } from './typing';

const API_VIDEO_INSPECTION_LIST = '/platform/video/monitor/inspection/list';

export const getVideoInspectionPage = async (data: VideoCameraQueryParams) =>
  request<VideoPagedList<VideoInspectionRecord>>(API_VIDEO_INSPECTION_LIST, {
    method: 'POST',
    data,
  });
