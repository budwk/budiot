import { request } from '@/utils/request';
import type {
  VideoAlgoTaskMeta,
  VideoAlgoTaskQueryParams,
  VideoAlgoTaskLogResponse,
  VideoAlgoTaskRecord,
  VideoAlgoTaskRuntimeRecord,
  VideoPagedList,
} from './typing';

const API_VIDEO_TASK_DATA = '/platform/video/task/data';
const API_VIDEO_TASK_LIST = '/platform/video/task/list';
const API_VIDEO_TASK_GET = '/platform/video/task/get/';
const API_VIDEO_TASK_CREATE = '/platform/video/task/create';
const API_VIDEO_TASK_UPDATE = '/platform/video/task/update';
const API_VIDEO_TASK_REGION_CONFIG_SAVE = '/platform/video/task/region-config/save/';
const API_VIDEO_TASK_START = '/platform/video/task/start/';
const API_VIDEO_TASK_STOP = '/platform/video/task/stop/';
const API_VIDEO_TASK_DELETE = '/platform/video/task/delete/';
const API_VIDEO_TASK_RUNTIME = '/platform/video/task/runtime/';

export const getVideoTaskMeta = async () =>
  request<VideoAlgoTaskMeta>(API_VIDEO_TASK_DATA, {
    method: 'GET',
  });

export const getVideoTaskPage = async (data: VideoAlgoTaskQueryParams) =>
  request<VideoPagedList<VideoAlgoTaskRecord>>(API_VIDEO_TASK_LIST, {
    method: 'POST',
    data,
  });

export const getVideoTaskDetail = async (id: string) =>
  request<VideoAlgoTaskRecord>(`${API_VIDEO_TASK_GET}${id}`, {
    method: 'GET',
  });

export const createVideoTask = async (data: Record<string, unknown>) =>
  request<VideoAlgoTaskRecord>(API_VIDEO_TASK_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoTask = async (data: Record<string, unknown>) =>
  request<VideoAlgoTaskRecord>(API_VIDEO_TASK_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const saveVideoTaskRegionConfigs = async (id: string, data: Record<string, unknown>[]) =>
  request<VideoAlgoTaskRecord>(`${API_VIDEO_TASK_REGION_CONFIG_SAVE}${id}`, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const startVideoTask = async (id: string) =>
  request<VideoAlgoTaskRecord>(`${API_VIDEO_TASK_START}${id}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const stopVideoTask = async (id: string) =>
  request<VideoAlgoTaskRecord>(`${API_VIDEO_TASK_STOP}${id}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const getVideoTaskRuntime = async (id: string) =>
  request<VideoAlgoTaskRuntimeRecord>(`${API_VIDEO_TASK_RUNTIME}${id}`, {
    method: 'GET',
  });

export const getVideoTaskLogs = async (id: string, params?: { cameraId?: string; limit?: number }) =>
  request<VideoAlgoTaskLogResponse>(`${API_VIDEO_TASK_RUNTIME}${id}/logs`, {
    method: 'GET',
    params,
  });

export const deleteVideoTask = async (id: string) =>
  request<unknown>(`${API_VIDEO_TASK_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
