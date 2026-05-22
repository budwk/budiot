import { request } from '@/utils/request';
import type { VideoModelRecord, VideoOption, VideoPagedList } from './typing';

const API_VIDEO_MODEL_DATA = '/platform/video/model/data';
const API_VIDEO_MODEL_LIST = '/platform/video/model/list';
const API_VIDEO_MODEL_GET = '/platform/video/model/get/';
const API_VIDEO_MODEL_CREATE = '/platform/video/model/create';
const API_VIDEO_MODEL_IMPORT = '/platform/video/model/import/';
const API_VIDEO_MODEL_UPDATE = '/platform/video/model/update';
const API_VIDEO_MODEL_DELETE = '/platform/video/model/delete/';

export const getVideoModelMeta = async () =>
  request<{ statuses: VideoOption[] }>(API_VIDEO_MODEL_DATA, {
    method: 'GET',
  });

export const getVideoModelPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoModelRecord>>(API_VIDEO_MODEL_LIST, {
    method: 'POST',
    data,
  });

export const getVideoModelDetail = async (id: string) =>
  request<VideoModelRecord>(`${API_VIDEO_MODEL_GET}${id}`, {
    method: 'GET',
  });

export const createVideoModel = async (data: Record<string, unknown>) =>
  request<VideoModelRecord>(API_VIDEO_MODEL_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoModel = async (data: Record<string, unknown>) =>
  request<VideoModelRecord>(API_VIDEO_MODEL_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const importVideoModelPackage = async (id: string, data: Record<string, unknown>) =>
  request<unknown>(`${API_VIDEO_MODEL_IMPORT}${id}`, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoModel = async (id: string) =>
  request<unknown>(`${API_VIDEO_MODEL_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
