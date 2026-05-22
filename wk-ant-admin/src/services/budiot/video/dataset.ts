import { request } from '@/utils/request';
import type {
  VideoDatasetAnnotationRecord,
  VideoDatasetAiConfigRecord,
  VideoDatasetAiTaskRecord,
  VideoDatasetItemMeta,
  VideoDatasetItemRecord,
  VideoDatasetLabelRecord,
  VideoOption,
  VideoDatasetRecord,
  VideoPagedList,
} from './typing';

const API_VIDEO_DATASET_DATA = '/platform/video/dataset/data';
const API_VIDEO_DATASET_LIST = '/platform/video/dataset/list';
const API_VIDEO_DATASET_GET = '/platform/video/dataset/get/';
const API_VIDEO_DATASET_CREATE = '/platform/video/dataset/create';
const API_VIDEO_DATASET_UPDATE = '/platform/video/dataset/update';
const API_VIDEO_DATASET_DELETE = '/platform/video/dataset/delete/';
const API_VIDEO_DATASET_LABEL_LIST = '/platform/video/dataset/label/list/';
const API_VIDEO_DATASET_LABEL_GET = '/platform/video/dataset/label/get/';
const API_VIDEO_DATASET_LABEL_CREATE = '/platform/video/dataset/label/create';
const API_VIDEO_DATASET_LABEL_UPDATE = '/platform/video/dataset/label/update';
const API_VIDEO_DATASET_LABEL_DELETE = '/platform/video/dataset/label/delete/';
const API_VIDEO_DATASET_ANNOTATION_DATA = '/platform/video/dataset/annotation/data/';
const API_VIDEO_DATASET_ANNOTATION_LIST = '/platform/video/dataset/annotation/list';
const API_VIDEO_DATASET_ANNOTATION_GET = '/platform/video/dataset/annotation/get/';
const API_VIDEO_DATASET_ANNOTATION_CREATE = '/platform/video/dataset/annotation/create';
const API_VIDEO_DATASET_ANNOTATION_UPDATE = '/platform/video/dataset/annotation/update';
const API_VIDEO_DATASET_ANNOTATION_DELETE = '/platform/video/dataset/annotation/delete/';
const API_VIDEO_DATASET_ITEM_DATA = '/platform/video/dataset/item/data/';
const API_VIDEO_DATASET_ITEM_LIST = '/platform/video/dataset/item/list';
const API_VIDEO_DATASET_ITEM_GET = '/platform/video/dataset/item/get/';
const API_VIDEO_DATASET_ITEM_CREATE = '/platform/video/dataset/item/create';
const API_VIDEO_DATASET_ITEM_BATCH_CREATE = '/platform/video/dataset/item/batchCreate';
const API_VIDEO_DATASET_ITEM_UPDATE = '/platform/video/dataset/item/update';
const API_VIDEO_DATASET_ITEM_DELETE = '/platform/video/dataset/item/delete/';
const API_VIDEO_DATASET_ITEM_SPLIT_USAGE = '/platform/video/dataset/item/splitUsage';
const API_VIDEO_DATASET_ITEM_RESET_USAGE = '/platform/video/dataset/item/resetUsage/';
const API_VIDEO_DATASET_ITEM_RESET_ANNOTATION_STATUS =
  '/platform/video/dataset/item/resetAnnotationStatus/';
const API_VIDEO_DATASET_ITEM_AI_ANNOTATE_START = '/platform/video/dataset/item/aiAnnotate/start/';
const API_VIDEO_DATASET_ITEM_AI_ANNOTATE_TASK = '/platform/video/dataset/item/aiAnnotate/task/';
const API_VIDEO_DATASET_ITEM_AI_ANNOTATE_CANCEL = '/platform/video/dataset/item/aiAnnotate/cancel/';
const API_VIDEO_DATASET_ITEM_FRAME_EXTRACT = '/platform/video/dataset/item/frameExtract';
const API_VIDEO_DATASET_AI_CONFIG_GET = '/platform/video/dataset/ai/config/get';
const API_VIDEO_DATASET_AI_CONFIG_SAVE = '/platform/video/dataset/ai/config/save';

export const getVideoDatasetMeta = async () =>
  request<{ datasetTypes: VideoOption[] }>(API_VIDEO_DATASET_DATA, {
    method: 'GET',
  });

export const getVideoDatasetPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoDatasetRecord>>(API_VIDEO_DATASET_LIST, {
    method: 'POST',
    data,
  });

export const getVideoDatasetDetail = async (id: string) =>
  request<VideoDatasetRecord>(`${API_VIDEO_DATASET_GET}${id}`, {
    method: 'GET',
  });

export const createVideoDataset = async (data: Record<string, unknown>) =>
  request<VideoDatasetRecord>(API_VIDEO_DATASET_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoDataset = async (data: Record<string, unknown>) =>
  request<VideoDatasetRecord>(API_VIDEO_DATASET_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoDataset = async (id: string) =>
  request<unknown>(`${API_VIDEO_DATASET_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const listVideoDatasetLabels = async (datasetId: string) =>
  request<VideoDatasetLabelRecord[]>(`${API_VIDEO_DATASET_LABEL_LIST}${datasetId}`, {
    method: 'GET',
  });

export const getVideoDatasetLabelDetail = async (id: string) =>
  request<VideoDatasetLabelRecord>(`${API_VIDEO_DATASET_LABEL_GET}${id}`, {
    method: 'GET',
  });

export const createVideoDatasetLabel = async (data: Record<string, unknown>) =>
  request<VideoDatasetLabelRecord>(API_VIDEO_DATASET_LABEL_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoDatasetLabel = async (data: Record<string, unknown>) =>
  request<VideoDatasetLabelRecord>(API_VIDEO_DATASET_LABEL_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoDatasetLabel = async (id: string) =>
  request<unknown>(`${API_VIDEO_DATASET_LABEL_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getVideoDatasetAnnotationMeta = async (datasetId: string) =>
  request<{ annotationTypes: VideoOption[]; labels: VideoDatasetLabelRecord[] }>(
    `${API_VIDEO_DATASET_ANNOTATION_DATA}${datasetId}`,
    {
      method: 'GET',
    },
  );

export const getVideoDatasetAnnotationPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoDatasetAnnotationRecord>>(API_VIDEO_DATASET_ANNOTATION_LIST, {
    method: 'POST',
    data,
  });

export const getVideoDatasetAnnotationDetail = async (id: string) =>
  request<VideoDatasetAnnotationRecord>(`${API_VIDEO_DATASET_ANNOTATION_GET}${id}`, {
    method: 'GET',
  });

export const createVideoDatasetAnnotation = async (data: Record<string, unknown>) =>
  request<VideoDatasetAnnotationRecord>(API_VIDEO_DATASET_ANNOTATION_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoDatasetAnnotation = async (data: Record<string, unknown>) =>
  request<VideoDatasetAnnotationRecord>(API_VIDEO_DATASET_ANNOTATION_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoDatasetAnnotation = async (id: string) =>
  request<unknown>(`${API_VIDEO_DATASET_ANNOTATION_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getVideoDatasetItemMeta = async (datasetId: string) =>
  request<VideoDatasetItemMeta>(`${API_VIDEO_DATASET_ITEM_DATA}${datasetId}`, {
    method: 'GET',
  });

export const getVideoDatasetItemPage = async (data: Record<string, unknown>) =>
  request<VideoPagedList<VideoDatasetItemRecord>>(API_VIDEO_DATASET_ITEM_LIST, {
    method: 'POST',
    data,
  });

export const getVideoDatasetItemDetail = async (id: string) =>
  request<VideoDatasetItemRecord>(`${API_VIDEO_DATASET_ITEM_GET}${id}`, {
    method: 'GET',
  });

export const createVideoDatasetItem = async (data: Record<string, unknown>) =>
  request<VideoDatasetItemRecord>(API_VIDEO_DATASET_ITEM_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const batchCreateVideoDatasetItems = async (data: Array<Record<string, unknown>>) =>
  request<VideoDatasetItemRecord[]>(API_VIDEO_DATASET_ITEM_BATCH_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoDatasetItem = async (data: Record<string, unknown>) =>
  request<VideoDatasetItemRecord>(API_VIDEO_DATASET_ITEM_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoDatasetItem = async (id: string) =>
  request<unknown>(`${API_VIDEO_DATASET_ITEM_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const splitVideoDatasetItemUsage = async (data: Record<string, unknown>) =>
  request<Record<string, number>>(API_VIDEO_DATASET_ITEM_SPLIT_USAGE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const resetVideoDatasetItemUsage = async (datasetId: string) =>
  request<number>(`${API_VIDEO_DATASET_ITEM_RESET_USAGE}${datasetId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const resetVideoDatasetItemAnnotationStatus = async (datasetId: string) =>
  request<{ itemCount: number; annotationCount: number }>(
    `${API_VIDEO_DATASET_ITEM_RESET_ANNOTATION_STATUS}${datasetId}`,
    {
      method: 'POST',
      showSuccessMessage: true,
    },
  );

export const startVideoDatasetAiAnnotateTask = async (datasetId: string) =>
  request<VideoDatasetAiTaskRecord>(`${API_VIDEO_DATASET_ITEM_AI_ANNOTATE_START}${datasetId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const getVideoDatasetAiAnnotateTask = async (taskId: string) =>
  request<VideoDatasetAiTaskRecord>(`${API_VIDEO_DATASET_ITEM_AI_ANNOTATE_TASK}${taskId}`, {
    method: 'GET',
  });

export const cancelVideoDatasetAiAnnotateTask = async (taskId: string) =>
  request<VideoDatasetAiTaskRecord>(`${API_VIDEO_DATASET_ITEM_AI_ANNOTATE_CANCEL}${taskId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const extractVideoDatasetFrames = async (data: Record<string, unknown>) =>
  request<{ createdCount: number; uploadedCount: number }>(API_VIDEO_DATASET_ITEM_FRAME_EXTRACT, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const getVideoDatasetAiConfig = async () =>
  request<VideoDatasetAiConfigRecord>(API_VIDEO_DATASET_AI_CONFIG_GET, {
    method: 'GET',
  });

export const saveVideoDatasetAiConfig = async (data: VideoDatasetAiConfigRecord) =>
  request<VideoDatasetAiConfigRecord>(API_VIDEO_DATASET_AI_CONFIG_SAVE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
