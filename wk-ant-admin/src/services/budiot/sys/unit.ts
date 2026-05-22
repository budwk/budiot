import type {
  SysUnitDetail,
  SysUnitRecord,
  SysUnitUserRelation,
  SysUserOption,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_UNIT_LIST = '/platform/sys/unit/list';
const API_SYS_UNIT_CREATE = '/platform/sys/unit/create';
const API_SYS_UNIT_DELETE = '/platform/sys/unit/delete/';
const API_SYS_UNIT_GET = '/platform/sys/unit/get/';
const API_SYS_UNIT_UPDATE = '/platform/sys/unit/update';
const API_SYS_UNIT_SORT = '/platform/sys/unit/sort';
const API_SYS_UNIT_SEARCH_USER = '/platform/sys/unit/search_user';

export const getUnitList = async (params: {
  name?: string;
  leaderName?: string;
}) =>
  request<SysUnitRecord[]>(API_SYS_UNIT_LIST, {
    method: 'GET',
    params,
  });

export const getUnitDetail = async (id: string) =>
  request<{ unit: SysUnitDetail; unitUserList: SysUnitUserRelation[] }>(
    `${API_SYS_UNIT_GET}${id}`,
    {
      method: 'GET',
    },
  );

export const createUnit = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_UNIT_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateUnit = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_UNIT_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteUnit = async (id: string) =>
  request<unknown>(`${API_SYS_UNIT_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const sortUnits = async (ids: string) =>
  request<unknown>(API_SYS_UNIT_SORT, {
    method: 'POST',
    data: { ids },
    showSuccessMessage: true,
  });

export const searchUnitUsers = async (query: string, unitId: string) =>
  request<{ list: SysUserOption[] }>(API_SYS_UNIT_SEARCH_USER, {
    method: 'POST',
    data: { query, unitId },
  });

