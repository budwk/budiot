import type {
  PagedList,
  SysPostRecord,
  SysRoleGroupRecord,
  SysUnitRecord,
  SysUserDetail,
  SysUserRecord,
} from '@/services/budiot/typing';
import { API_BASE_URL, APP_ID, DEFAULT_LANG } from '@/constants/app';
import { downloadFile } from '@/utils/file';
import { getStoredToken } from '@/utils/session';
import { request } from '@/utils/request';

export const API_SYS_USER_LIST = '/platform/sys/user/list';
export const API_SYS_USER_UNITLIST = '/platform/sys/user/unitlist';
export const API_SYS_USER_POST = '/platform/sys/user/post';
export const API_SYS_USER_NUMBER = '/platform/sys/user/number';
export const API_SYS_USER_GROUP = '/platform/sys/user/group';
export const API_SYS_USER_CREATE = '/platform/sys/user/create';
export const API_SYS_USER_GET = '/platform/sys/user/get/';
export const API_SYS_USER_UPDATE = '/platform/sys/user/update';
export const API_SYS_USER_RESETPWD = '/platform/sys/user/reset_pwd/';
export const API_SYS_USER_DISABLED = '/platform/sys/user/disabled';
export const API_SYS_USER_DELETE = '/platform/sys/user/delete/';
export const API_SYS_USER_EXPORT = '/platform/sys/user/export';
export const API_SYS_USER_IMPORT_DATA = '/platform/sys/user/importData';
export const API_SYS_USER_IMPORT_TEMPLATE = '/platform/sys/user/importTemplate';

export const getUserUnitTree = async (params?: { name?: string }) =>
  request<SysUnitRecord[]>(API_SYS_USER_UNITLIST, {
    method: 'GET',
    params,
  });

export const getUserList = async (data: Record<string, unknown>) =>
  request<PagedList<SysUserRecord>>(API_SYS_USER_LIST, {
    method: 'POST',
    data,
  });

export const getUserPosts = async () =>
  request<SysPostRecord[]>(API_SYS_USER_POST, {
    method: 'GET',
  });

export const getUserSerialNo = async () =>
  request<string>(API_SYS_USER_NUMBER, {
    method: 'GET',
  });

export const getUserRoleGroups = async (unitId: string) =>
  request<SysRoleGroupRecord[]>(API_SYS_USER_GROUP, {
    method: 'GET',
    params: { unitId },
  });

export const getUserDetail = async (id: string) =>
  request<{ user: SysUserDetail; roleIds: string[] }>(`${API_SYS_USER_GET}${id}`, {
    method: 'GET',
  });

export const createUser = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_USER_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateUser = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_USER_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const resetUserPassword = async (id: string) =>
  request<string>(`${API_SYS_USER_RESETPWD}${id}`, {
    method: 'GET',
  });

export const updateUserDisabled = async (payload: {
  id: string;
  loginname: string;
  disabled: boolean;
}) =>
  request<unknown>(API_SYS_USER_DISABLED, {
    method: 'POST',
    data: payload,
    showSuccessMessage: true,
  });

export const deleteUser = async (id: string, loginname: string) =>
  request<unknown>(`${API_SYS_USER_DELETE}${id}`, {
    method: 'DELETE',
    data: { loginname },
    showSuccessMessage: true,
  });

export const downloadUserImportTemplate = async (fileName: string) =>
  downloadFile({
    url: API_SYS_USER_IMPORT_TEMPLATE,
    fileName,
  });

export const exportUsers = async (params: Record<string, unknown>, fileName: string) =>
  downloadFile({
    url: API_SYS_USER_EXPORT,
    params,
    fileName,
  });

export const importUsers = async (
  formData: FormData,
  params: { updateSupport: boolean; pwd?: string },
) => {
  const searchParams = new URLSearchParams({
    updateSupport: String(params.updateSupport),
    pwd: params.pwd || '',
  });

  const response = await fetch(
    `${API_BASE_URL}${API_SYS_USER_IMPORT_DATA}?${searchParams.toString()}`,
    {
      method: 'POST',
      body: formData,
      headers: {
        Authorization: getStoredToken() || '',
        lang: DEFAULT_LANG,
        appId: APP_ID,
      },
    },
  );

  return response.json();
};
