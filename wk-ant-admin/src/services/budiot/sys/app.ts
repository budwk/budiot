import type { PagedList, SysAppRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_APP_LIST = '/platform/sys/app/list';
const API_SYS_APP_GET = '/platform/sys/app/get/';
const API_SYS_APP_CREATE = '/platform/sys/app/create';
const API_SYS_APP_DELETE = '/platform/sys/app/delete/';
const API_SYS_APP_UPDATE = '/platform/sys/app/update';
const API_SYS_APP_DISABLED = '/platform/sys/app/disabled';
const API_SYS_APP_LOCATION = '/platform/sys/app/location';

export const getAppPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysAppRecord>>(API_SYS_APP_LIST, {
    method: 'POST',
    data,
  });

export const getAppDetail = async (id: string) =>
  request<SysAppRecord>(`${API_SYS_APP_GET}${id}`, {
    method: 'GET',
  });

export const createApp = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_APP_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateApp = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_APP_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteApp = async (id: string) =>
  request<unknown>(`${API_SYS_APP_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const updateAppDisabled = async (data: { id: string; disabled: boolean }) =>
  request<unknown>(API_SYS_APP_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateAppLocation = async (data: { id: string; location: number }) =>
  request<unknown>(API_SYS_APP_LOCATION, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
