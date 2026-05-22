import type {
  BudiotAppOption,
  SysMenuDetail,
  SysMenuRecord,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_MENU_LIST = '/platform/sys/menu/list';
const API_SYS_MENU_CREATE = '/platform/sys/menu/create';
const API_SYS_MENU_DELETE = '/platform/sys/menu/delete/';
const API_SYS_MENU_GET_MENU = '/platform/sys/menu/get_menu/';
const API_SYS_MENU_UPDATE_MENU = '/platform/sys/menu/update_menu';
const API_SYS_MENU_UPDATE_DATA = '/platform/sys/menu/update_data';
const API_SYS_MENU_DISABLED = '/platform/sys/menu/disabled';
const API_SYS_MENU_APP_DATA = '/platform/sys/menu/data';
const API_SYS_MENU_SORT = '/platform/sys/menu/sort';

interface MenuAppsResponse {
  apps: BudiotAppOption[];
}

export const getMenuApps = async () =>
  request<MenuAppsResponse>(API_SYS_MENU_APP_DATA, {
    method: 'GET',
  });

export const getMenuList = async (params: {
  appId: string;
  name?: string;
  href?: string;
}) =>
  request<SysMenuRecord[]>(API_SYS_MENU_LIST, {
    method: 'GET',
    params,
  });

export const getMenuDetail = async (id: string) =>
  request<SysMenuDetail>(`${API_SYS_MENU_GET_MENU}${id}`, {
    method: 'GET',
  });

export const createMenu = async (menu: string, buttons: string, appId: string) =>
  request<unknown>(API_SYS_MENU_CREATE, {
    method: 'POST',
    data: { menu, buttons, appId },
    showSuccessMessage: true,
  });

export const updateMenu = async (menu: string, buttons: string) =>
  request<unknown>(API_SYS_MENU_UPDATE_MENU, {
    method: 'POST',
    data: { menu, buttons },
    showSuccessMessage: true,
  });

export const updateMenuData = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_MENU_UPDATE_DATA, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteMenu = async (id: string) =>
  request<unknown>(`${API_SYS_MENU_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const updateMenuDisabled = async (payload: {
  id: string;
  path: string;
  disabled: boolean;
}) =>
  request<unknown>(API_SYS_MENU_DISABLED, {
    method: 'POST',
    data: payload,
    showSuccessMessage: true,
  });

export const sortMenus = async (payload: {
  ids: string;
  appId: string;
}) =>
  request<unknown>(API_SYS_MENU_SORT, {
    method: 'POST',
    params: payload,
    showSuccessMessage: true,
  });
