import { request } from '@/utils/request';

const API_VIDEO_GB28181_STOP = '/platform/video/gb28181/stop';

export const stopVideoGb28181Live = async (params: { deviceId: string; channelId?: string }) =>
  request<unknown>(API_VIDEO_GB28181_STOP, {
    method: 'POST',
    params,
    showSuccessMessage: false,
  });
