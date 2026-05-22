import type {
  HomeUserInfo,
  PagedList,
  SysLogRecord,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_HOME_USER_CHANGE_PWD = '/platform/home/user/pwd';
const API_HOME_USER_CHANGE_INFO = '/platform/home/user/info';
const API_HOME_USER_GET_INFO = '/platform/home/user/get';
const API_HOME_USER_SET_AVATAR = '/platform/home/user/avatar';
const API_HOME_USER_GET_LOG = '/platform/home/user/log';

export const getHomeUserInfo = async () =>
  request<HomeUserInfo>(API_HOME_USER_GET_INFO, {
    method: 'GET',
  });

export const changeHomeUserInfo = async (data: Record<string, unknown>) =>
  request<unknown>(API_HOME_USER_CHANGE_INFO, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const changeHomeUserPassword = async (oldPassword: string, newPassword: string) =>
  request<unknown>(API_HOME_USER_CHANGE_PWD, {
    method: 'POST',
    data: { oldPassword, newPassword },
    showSuccessMessage: true,
  });

export const changeHomeUserAvatar = async (avatar: string) =>
  request<unknown>(API_HOME_USER_SET_AVATAR, {
    method: 'POST',
    data: { avatar },
    showSuccessMessage: true,
  });

export const getHomeUserLogPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysLogRecord>>(API_HOME_USER_GET_LOG, {
    method: 'POST',
    data,
  });
