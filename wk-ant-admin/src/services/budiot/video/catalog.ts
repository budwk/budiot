import { request } from '@/utils/request';
import type { VideoCatalogRecord } from './typing';

const API_VIDEO_CATALOG_TREE = '/platform/video/catalog/tree';
const API_VIDEO_CATALOG_LIST = '/platform/video/catalog/list';
const API_VIDEO_CATALOG_GET = '/platform/video/catalog/get/';
const API_VIDEO_CATALOG_CREATE = '/platform/video/catalog/create';
const API_VIDEO_CATALOG_UPDATE = '/platform/video/catalog/update';
const API_VIDEO_CATALOG_DELETE = '/platform/video/catalog/delete/';

export const getVideoCatalogTree = async () =>
  request<VideoCatalogRecord[]>(API_VIDEO_CATALOG_TREE, {
    method: 'GET',
  });

export const getVideoCatalogList = async () =>
  request<VideoCatalogRecord[]>(API_VIDEO_CATALOG_LIST, {
    method: 'GET',
  });

export const getVideoCatalogDetail = async (id: string) =>
  request<VideoCatalogRecord>(`${API_VIDEO_CATALOG_GET}${id}`, {
    method: 'GET',
  });

export const createVideoCatalog = async (data: Record<string, unknown>) =>
  request<VideoCatalogRecord>(API_VIDEO_CATALOG_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateVideoCatalog = async (data: Record<string, unknown>) =>
  request<VideoCatalogRecord>(API_VIDEO_CATALOG_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteVideoCatalog = async (id: string) =>
  request<unknown>(`${API_VIDEO_CATALOG_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
