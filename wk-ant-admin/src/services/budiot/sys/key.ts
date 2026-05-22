import type { PagedList, SysKeyRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_KEY_LIST = '/platform/sys/key/list';
const API_SYS_KEY_CREATE = '/platform/sys/key/create';
const API_SYS_KEY_DELETE = '/platform/sys/key/delete/';
const API_SYS_KEY_DISABLED = '/platform/sys/key/disabled';

export const getKeyPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysKeyRecord>>(API_SYS_KEY_LIST, {
    method: 'POST',
    data,
  });

export const createKey = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_KEY_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteKey = async (appid: string) =>
  request<unknown>(`${API_SYS_KEY_DELETE}${appid}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const updateKeyDisabled = async (data: { appid: string; disabled: boolean }) =>
  request<unknown>(API_SYS_KEY_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
