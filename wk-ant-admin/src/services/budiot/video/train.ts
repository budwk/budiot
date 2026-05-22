import { request } from '@/utils/request';
import type {
  VideoModelInferenceTaskRecord,
  VideoModelDeploymentRecord,
  VideoModelTrainMeta,
  VideoModelTrainTaskRecord,
  VideoPagedList,
} from './typing';

const API_VIDEO_MODEL_TRAIN_DATA = '/platform/video/model/train/data';
const API_VIDEO_MODEL_TASK_LIST = '/platform/video/model/train/task/list';
const API_VIDEO_MODEL_TASK_GET = '/platform/video/model/train/task/get/';
const API_VIDEO_MODEL_TASK_CREATE = '/platform/video/model/train/task/create';
const API_VIDEO_MODEL_TASK_DELETE = '/platform/video/model/train/task/delete/';
const API_VIDEO_MODEL_TASK_LOG = '/platform/video/model/train/task/log/';
const API_VIDEO_MODEL_TASK_EXPORT = '/platform/video/model/train/task/export/';
const API_VIDEO_MODEL_DEPLOYMENT_LIST = '/platform/video/model/train/deployment/list';
const API_VIDEO_MODEL_DEPLOYMENT_GET = '/platform/video/model/train/deployment/get/';
const API_VIDEO_MODEL_DEPLOYMENT_CREATE = '/platform/video/model/train/deployment/create';
const API_VIDEO_MODEL_DEPLOYMENT_ACTION = '/platform/video/model/train/deployment/action';
const API_VIDEO_MODEL_DEPLOYMENT_DELETE = '/platform/video/model/train/deployment/delete/';
const API_VIDEO_MODEL_INFERENCE_LIST = '/platform/video/model/train/inference/list';
const API_VIDEO_MODEL_INFERENCE_GET = '/platform/video/model/train/inference/get/';
const API_VIDEO_MODEL_INFERENCE_CREATE = '/platform/video/model/train/inference/create';
const API_VIDEO_MODEL_INFERENCE_DELETE = '/platform/video/model/train/inference/delete/';
const API_VIDEO_MODEL_INFERENCE_RESULT = '/platform/video/model/train/inference/result/';

export const getVideoTrainMeta = async () =>
  request<VideoModelTrainMeta>(API_VIDEO_MODEL_TRAIN_DATA, {
    method: 'GET',
  });

export const getVideoTrainTaskPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoModelTrainTaskRecord>>(API_VIDEO_MODEL_TASK_LIST, {
    method: 'POST',
    data,
  });

export const getVideoTrainTaskDetail = async (id: string) =>
  request<VideoModelTrainTaskRecord>(`${API_VIDEO_MODEL_TASK_GET}${id}`, {
    method: 'GET',
  });

export const createVideoTrainTask = async (data: Record<string, unknown>) =>
  request<VideoModelTrainTaskRecord>(API_VIDEO_MODEL_TASK_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoTrainTask = async (id: string) =>
  request<unknown>(`${API_VIDEO_MODEL_TASK_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getVideoTrainTaskLog = async (id: string) =>
  request<{ log: string; status?: string; progress?: number; result?: Record<string, unknown> }>(
    `${API_VIDEO_MODEL_TASK_LOG}${id}`,
    {
      method: 'GET',
    },
  );

export const exportVideoTrainTask = async (id: string, data: Record<string, unknown>) =>
  request<{
    downloads?: Array<{ format?: string; url?: string; filename?: string }>;
    export_errors?: Record<string, string>;
    result?: Record<string, unknown>;
  }>(`${API_VIDEO_MODEL_TASK_EXPORT}${id}`, {
    method: 'POST',
    data,
  });

export const getVideoDeploymentPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoModelDeploymentRecord>>(API_VIDEO_MODEL_DEPLOYMENT_LIST, {
    method: 'POST',
    data,
  });

export const getVideoDeploymentDetail = async (id: string) =>
  request<VideoModelDeploymentRecord>(`${API_VIDEO_MODEL_DEPLOYMENT_GET}${id}`, {
    method: 'GET',
  });

export const createVideoDeployment = async (data: Record<string, unknown>) =>
  request<VideoModelDeploymentRecord>(API_VIDEO_MODEL_DEPLOYMENT_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const operateVideoDeployment = async (data: Record<string, unknown>) =>
  request<VideoModelDeploymentRecord>(API_VIDEO_MODEL_DEPLOYMENT_ACTION, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoDeployment = async (id: string) =>
  request<unknown>(`${API_VIDEO_MODEL_DEPLOYMENT_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getVideoInferencePage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoModelInferenceTaskRecord>>(API_VIDEO_MODEL_INFERENCE_LIST, {
    method: 'POST',
    data,
  });

export const getVideoInferenceDetail = async (id: string) =>
  request<VideoModelInferenceTaskRecord>(`${API_VIDEO_MODEL_INFERENCE_GET}${id}`, {
    method: 'GET',
  });

export const createVideoInferenceTask = async (data: Record<string, unknown>) =>
  request<VideoModelInferenceTaskRecord>(API_VIDEO_MODEL_INFERENCE_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoInferenceTask = async (id: string) =>
  request<unknown>(`${API_VIDEO_MODEL_INFERENCE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getVideoInferenceResult = async (id: string) =>
  request<{ result: string }>(`${API_VIDEO_MODEL_INFERENCE_RESULT}${id}`, {
    method: 'GET',
  });
