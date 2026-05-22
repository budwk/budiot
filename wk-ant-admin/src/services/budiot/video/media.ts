import { request } from '@/utils/request';
import type { VideoMediaAction, VideoMediaEndpoint, VideoOnvifScanResult } from './typing';

const API_VIDEO_MEDIA_ENDPOINT = '/platform/video/media/endpoint/';
const API_VIDEO_MEDIA_SNAPSHOT = '/platform/video/media/snapshot/';
const API_VIDEO_MEDIA_RECORD_START = '/platform/video/media/record/start/';
const API_VIDEO_MEDIA_RECORD_STOP = '/platform/video/media/record/stop/';
const API_VIDEO_MEDIA_PROXY_ENABLE = '/platform/video/media/proxy/enable/';
const API_VIDEO_MEDIA_ONVIF_SCAN = '/platform/video/media/onvif/scan';
const API_VIDEO_MEDIA_ONVIF_TEST = '/platform/video/media/onvif/test';
const API_VIDEO_MEDIA_PTZ = '/platform/video/media/ptz/';
const API_VIDEO_MEDIA_PTZ_STOP = '/platform/video/media/ptz/stop/';

export const getVideoMediaEndpoint = async (cameraId: string) =>
  request<VideoMediaEndpoint>(`${API_VIDEO_MEDIA_ENDPOINT}${cameraId}`, {
    method: 'GET',
  });

export const snapshotVideoCamera = async (cameraId: string, showSuccessMessage = true) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_SNAPSHOT}${cameraId}`, {
    method: 'POST',
    showSuccessMessage,
  });

export const startVideoRecord = async (cameraId: string) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_RECORD_START}${cameraId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const stopVideoRecord = async (cameraId: string) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_RECORD_STOP}${cameraId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const enableVideoRtspProxy = async (cameraId: string) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_PROXY_ENABLE}${cameraId}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const scanOnvifDevices = async (params: { ipRange: string }) =>
  request<VideoOnvifScanResult[]>(API_VIDEO_MEDIA_ONVIF_SCAN, {
    method: 'POST',
    data: params,
  });

export const testOnvifConnection = async (params: { host: string; port: number; username: string; password: string }) =>
  request<{ success: boolean; message?: string }>(API_VIDEO_MEDIA_ONVIF_TEST, {
    method: 'POST',
    data: params,
  });

export const controlVideoPtz = async (cameraId: string, direction: string) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_PTZ}${cameraId}/${direction}`, {
    method: 'POST',
    showSuccessMessage: false,
  });

export const stopVideoPtz = async (cameraId: string) =>
  request<VideoMediaAction>(`${API_VIDEO_MEDIA_PTZ_STOP}${cameraId}`, {
    method: 'POST',
    showSuccessMessage: false,
  });
