import { request } from '@/utils/request';
import type {
  VideoCameraMeta,
  VideoCameraQueryParams,
  VideoCameraRecord,
  VideoPagedList,
} from './typing';

const API_VIDEO_CAMERA_DATA = '/platform/video/camera/data';
const API_VIDEO_CAMERA_LIST = '/platform/video/camera/list';
const API_VIDEO_CAMERA_GET = '/platform/video/camera/get/';
const API_VIDEO_CAMERA_CREATE = '/platform/video/camera/create';
const API_VIDEO_CAMERA_UPDATE = '/platform/video/camera/update';
const API_VIDEO_CAMERA_DELETE = '/platform/video/camera/delete/';

export const getVideoCameraMeta = async () =>
  request<VideoCameraMeta>(API_VIDEO_CAMERA_DATA, {
    method: 'GET',
  });

export const getVideoCameraPage = async (data: VideoCameraQueryParams) =>
  request<VideoPagedList<VideoCameraRecord>>(API_VIDEO_CAMERA_LIST, {
    method: 'POST',
    data,
  });

export const getVideoCameraDetail = async (id: string) =>
  request<VideoCameraRecord>(`${API_VIDEO_CAMERA_GET}${id}`, {
    method: 'GET',
  });

export const createVideoCamera = async (data: Record<string, unknown>) =>
  request<VideoCameraRecord>(API_VIDEO_CAMERA_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoCamera = async (data: Record<string, unknown>) =>
  request<VideoCameraRecord>(API_VIDEO_CAMERA_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoCamera = async (id: string) =>
  request<unknown>(`${API_VIDEO_CAMERA_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
