import type { SysDictRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_DICT_LIST = '/platform/sys/dict/list';
const API_SYS_DICT_DISABLED = '/platform/sys/dict/disabled';
const API_SYS_DICT_CREATE = '/platform/sys/dict/create';
const API_SYS_DICT_DELETE = '/platform/sys/dict/delete/';
const API_SYS_DICT_GET = '/platform/sys/dict/get/';
const API_SYS_DICT_UPDATE = '/platform/sys/dict/update';
const API_SYS_DICT_SORT = '/platform/sys/dict/sort';

export const getDictList = async (params: {
  name?: string;
  leaderName?: string;
}) =>
  request<SysDictRecord[]>(API_SYS_DICT_LIST, {
    method: 'GET',
    params,
  });

export const getDictDetail = async (id: string) =>
  request<SysDictRecord>(`${API_SYS_DICT_GET}${id}`, {
    method: 'GET',
  });

export const createDict = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_DICT_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateDict = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_DICT_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteDict = async (id: string) =>
  request<unknown>(`${API_SYS_DICT_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const sortDicts = async (ids: string) =>
  request<unknown>(API_SYS_DICT_SORT, {
    method: 'POST',
    data: { ids },
    showSuccessMessage: true,
  });

export const updateDictDisabled = async (data: { id: string; disabled: boolean }) =>
  request<unknown>(API_SYS_DICT_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
