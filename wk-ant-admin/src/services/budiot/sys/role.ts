import type {
  BudiotAppOption,
  PagedList,
  SysMenuRecord,
  SysPostRecord,
  SysRoleGroupRecord,
  SysUnitRecord,
  SysUserRecord,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_ROLE_CREATE_ROLE = '/platform/sys/role/create_role';
const API_SYS_ROLE_CREATE_GROUP = '/platform/sys/role/create_group';
const API_SYS_ROLE_UPDATE_ROLE = '/platform/sys/role/update_role';
const API_SYS_ROLE_UPDATE_GROUP = '/platform/sys/role/update_group';
const API_SYS_ROLE_DELETE_ROLE = '/platform/sys/role/delete_role';
const API_SYS_ROLE_DELETE_GROUP = '/platform/sys/role/delete_group';
const API_SYS_ROLE_USERLIST = '/platform/sys/role/user';
const API_SYS_ROLE_GET_DO_MENU = '/platform/sys/role/get_menus';
const API_SYS_ROLE_DO_MENU = '/platform/sys/role/do_menu';
const API_SYS_ROLE_UNIT = '/platform/sys/role/unit';
const API_SYS_ROLE_GROUP = '/platform/sys/role/group';
const API_SYS_ROLE_APP = '/platform/sys/role/app';
const API_SYS_ROLE_POST = '/platform/sys/role/post';
const API_SYS_ROLE_SELECT_USER = '/platform/sys/role/select_user';
const API_SYS_ROLE_LINK_USER = '/platform/sys/role/link_user';
const API_SYS_ROLE_UNLINK_USER = '/platform/sys/role/unlink_user';

export const getRoleUnits = async () =>
  request<SysUnitRecord[]>(API_SYS_ROLE_UNIT, {
    method: 'GET',
  });

export const getRoleGroups = async (unitId: string) =>
  request<SysRoleGroupRecord[]>(API_SYS_ROLE_GROUP, {
    method: 'GET',
    params: { unitId },
  });

export const getRoleApps = async () =>
  request<BudiotAppOption[]>(API_SYS_ROLE_APP, {
    method: 'GET',
  });

export const getRolePosts = async () =>
  request<SysPostRecord[]>(API_SYS_ROLE_POST, {
    method: 'GET',
  });

export const getRoleUsers = async (data: Record<string, unknown>) =>
  request<PagedList<SysUserRecord>>(API_SYS_ROLE_USERLIST, {
    method: 'POST',
    data,
  });

export const getRoleSelectableUsers = async (data: Record<string, unknown>) =>
  request<PagedList<SysUserRecord>>(API_SYS_ROLE_SELECT_USER, {
    method: 'POST',
    data,
  });

export const createRole = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_ROLE_CREATE_ROLE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const createRoleGroup = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_ROLE_CREATE_GROUP, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateRole = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_ROLE_UPDATE_ROLE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateRoleGroup = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_ROLE_UPDATE_GROUP, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteRole = async (id: string) =>
  request<unknown>(API_SYS_ROLE_DELETE_ROLE, {
    method: 'POST',
    params: { id },
    showSuccessMessage: true,
  });

export const deleteRoleGroup = async (id: string) =>
  request<unknown>(API_SYS_ROLE_DELETE_GROUP, {
    method: 'POST',
    params: { id },
    showSuccessMessage: true,
  });

export const getRoleMenus = async (roleId: string, appId: string) =>
  request<{ menuList: SysMenuRecord[]; menuIds: string[] }>(
    API_SYS_ROLE_GET_DO_MENU,
    {
      method: 'GET',
      params: { roleId, appId },
    },
  );

export const saveRoleMenus = async (data: {
  roleId: string;
  roleCode: string;
  appId: string;
  menuIds: string;
}) =>
  request<unknown>(API_SYS_ROLE_DO_MENU, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const linkRoleUsers = async (data: {
  roleId: string;
  roleCode: string;
  ids: string;
  names: string;
}) =>
  request<unknown>(API_SYS_ROLE_LINK_USER, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const unlinkRoleUser = async (data: {
  roleId: string;
  roleCode: string;
  id: string;
  name: string;
}) =>
  request<unknown>(API_SYS_ROLE_UNLINK_USER, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

