import type { SysAreaRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_AREA_LIST = '/platform/sys/area/list';
const API_SYS_AREA_CREATE = '/platform/sys/area/create';
const API_SYS_AREA_DELETE = '/platform/sys/area/delete/';
const API_SYS_AREA_GET = '/platform/sys/area/get/';
const API_SYS_AREA_UPDATE = '/platform/sys/area/update';
const API_SYS_AREA_SORT = '/platform/sys/area/sort';
const API_SYS_AREA_DISABLED = '/platform/sys/area/disabled';

export const getAreaList = async () =>
  request<SysAreaRecord[]>(API_SYS_AREA_LIST, {
    method: 'GET',
  });

export const getAreaDetail = async (id: string) =>
  request<SysAreaRecord>(`${API_SYS_AREA_GET}${id}`, {
    method: 'GET',
  });

export const createArea = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_AREA_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateArea = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_AREA_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteArea = async (id: string) =>
  request<unknown>(`${API_SYS_AREA_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const sortAreas = async (ids: string) =>
  request<unknown>(API_SYS_AREA_SORT, {
    method: 'POST',
    data: { ids },
    showSuccessMessage: true,
  });

export const updateAreaDisabled = async (data: { id: string; disabled: boolean }) =>
  request<unknown>(API_SYS_AREA_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
