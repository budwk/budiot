import { request } from '@/utils/request';
import type { IotCategoryRecord } from './typing';

const API_IOT_CATEGORY_LIST = '/platform/iot/category/list';
const API_IOT_CATEGORY_GET = '/platform/iot/category/get/';
const API_IOT_CATEGORY_CREATE = '/platform/iot/category/create';
const API_IOT_CATEGORY_UPDATE = '/platform/iot/category/update';
const API_IOT_CATEGORY_DELETE = '/platform/iot/category/delete/';
const API_IOT_CATEGORY_SORT_TREE = '/platform/iot/category/get_sort_tree';
const API_IOT_CATEGORY_SORT = '/platform/iot/category/sort';
const API_IOT_CATEGORY_DISABLED = '/platform/iot/category/disabled';

export const getIotCategoryList = async (params?: Record<string, unknown>) =>
  request<IotCategoryRecord[]>(API_IOT_CATEGORY_LIST, {
    method: 'GET',
    params,
  });

export const getIotCategoryDetail = async (id: string) =>
  request<IotCategoryRecord>(`${API_IOT_CATEGORY_GET}${id}`, {
    method: 'GET',
  });

export const createIotCategory = async (data: Record<string, unknown>) =>
  request<IotCategoryRecord>(API_IOT_CATEGORY_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotCategory = async (data: Record<string, unknown>) =>
  request<IotCategoryRecord>(API_IOT_CATEGORY_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotCategory = async (id: string) =>
  request<unknown>(`${API_IOT_CATEGORY_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getIotCategorySortTree = async () =>
  request<IotCategoryRecord[]>(API_IOT_CATEGORY_SORT_TREE, {
    method: 'GET',
  });

export const sortIotCategories = async (ids: string) =>
  request<unknown>(API_IOT_CATEGORY_SORT, {
    method: 'POST',
    params: { ids },
    showSuccessMessage: true,
  });

export const toggleIotCategoryDisabled = async (id: string, disabled: boolean) =>
  request<unknown>(API_IOT_CATEGORY_DISABLED, {
    method: 'POST',
    params: { id, disabled },
    showSuccessMessage: true,
  });
