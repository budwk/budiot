import { request } from '@/utils/request';
import type { VideoMediaFileQueryParams, VideoMediaFileRecord, VideoPagedList } from './typing';

const API_VIDEO_MEDIA_FILE_LIST = '/platform/video/media/file/list';
const API_VIDEO_MEDIA_FILE_GET = '/platform/video/media/file/get/';
const API_VIDEO_MEDIA_FILE_DELETE = '/platform/video/media/file/delete/';
const API_VIDEO_MEDIA_FILE_CLEAR = '/platform/video/media/file/clear';

export const getVideoMediaFilePage = async (data: VideoMediaFileQueryParams) =>
  request<VideoPagedList<VideoMediaFileRecord>>(API_VIDEO_MEDIA_FILE_LIST, {
    method: 'POST',
    data,
  });

export const getVideoMediaFileDetail = async (id: string) =>
  request<VideoMediaFileRecord>(`${API_VIDEO_MEDIA_FILE_GET}${id}`, {
    method: 'GET',
  });

export const deleteVideoMediaFile = async (id: string) =>
  request<unknown>(`${API_VIDEO_MEDIA_FILE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const clearVideoMediaFiles = async (data: VideoMediaFileQueryParams) =>
  request<unknown>(API_VIDEO_MEDIA_FILE_CLEAR, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
