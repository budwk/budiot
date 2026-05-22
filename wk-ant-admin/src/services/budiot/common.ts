import { request } from '@/utils/request';
import type { PlatformInfo } from './typing';

export const PLATFORM_INFO_URL = '/platform/auth/conf';
export const PLATFORM_UPLOAD_URL = '/platform/pub/file/upload/';
export const PLATFORM_UPLOAD_IMAGE_URL = '/platform/pub/file/upload/image';
export const PLATFORM_UPLOAD_FILE_URL = '/platform/pub/file/upload/file';
export const PLATFORM_UPLOAD_VIDEO_URL = '/platform/pub/file/upload/video';

export async function getPlatformInfo(appId: string) {
  return request<PlatformInfo>(PLATFORM_INFO_URL, {
    method: 'GET',
    params: { appId },
    skipAuthFailureRedirect: true,
    skipErrorHandler: true,
    showCodeMessage: false,
  });
}
