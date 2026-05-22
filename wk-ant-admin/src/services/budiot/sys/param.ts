import type {
  BudiotAppOption,
  PagedList,
  SysParamRecord,
  SysParamTypeOption,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_PARAM_LIST = '/platform/sys/param/list';
const API_SYS_PARAM_GET = '/platform/sys/param/get/';
const API_SYS_PARAM_CREATE = '/platform/sys/param/create';
const API_SYS_PARAM_DELETE = '/platform/sys/param/delete/';
const API_SYS_PARAM_UPDATE = '/platform/sys/param/update';
const API_SYS_PARAM_DATA = '/platform/sys/param/data';

export const getParamPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysParamRecord>>(API_SYS_PARAM_LIST, {
    method: 'POST',
    data,
  });

export const getParamDetail = async (id: string) =>
  request<SysParamRecord>(`${API_SYS_PARAM_GET}${id}`, {
    method: 'GET',
  });

export const createParam = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_PARAM_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateParam = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_PARAM_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteParam = async (id: string) =>
  request<unknown>(`${API_SYS_PARAM_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getParamMeta = async () =>
  request<{ apps: BudiotAppOption[]; types: SysParamTypeOption[] }>(
    API_SYS_PARAM_DATA,
    {
      method: 'GET',
    },
  );

