import type { PagedList, SysUnitRecord, SysUserRecord } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_PUB_USER_UNITLIST = '/platform/pub/user/unitlist';
const API_PUB_USER_LIST = '/platform/pub/user/list';

export const getPubUserUnitTree = async (params?: { name?: string }) =>
  request<SysUnitRecord[]>(API_PUB_USER_UNITLIST, {
    method: 'GET',
    params,
  });

export const getPubUserPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysUserRecord>>(API_PUB_USER_LIST, {
    method: 'POST',
    data,
  });
