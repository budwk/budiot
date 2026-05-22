import type {
  PagedList,
  SysMsgRecord,
  SysMsgScopeOption,
  SysMsgTypeOption,
  SysUserRecord,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_MSG_LIST = '/platform/sys/msg/list';
const API_SYS_MSG_CREATE = '/platform/sys/msg/create';
const API_SYS_MSG_DELETE = '/platform/sys/msg/delete/';
const API_SYS_MSG_GET_USER_VIEW_LIST = '/platform/sys/msg/get_user_view_list';
const API_SYS_MSG_DATA = '/platform/sys/msg/data';
const API_SYS_MSG_GET = '/platform/sys/msg/get/';

export const getMsgPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysMsgRecord>>(API_SYS_MSG_LIST, {
    method: 'POST',
    data,
  });

export const getMsgDetail = async (id: string) =>
  request<SysMsgRecord>(`${API_SYS_MSG_GET}${id}`, {
    method: 'GET',
  });

export const createMsg = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_MSG_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const revokeMsg = async (id: string) =>
  request<unknown>(`${API_SYS_MSG_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getMsgMeta = async () =>
  request<{ types: SysMsgTypeOption[]; scopes: SysMsgScopeOption[] }>(API_SYS_MSG_DATA, {
    method: 'GET',
  });

export const getMsgViewUsers = async (data: Record<string, unknown>) =>
  request<PagedList<SysUserRecord>>(API_SYS_MSG_GET_USER_VIEW_LIST, {
    method: 'POST',
    data,
  });
