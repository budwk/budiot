import { request } from '@/utils/request';
import type {
  HomeMsgDetail,
  HomeMsgInitData,
  HomeMsgListRecord,
  HomeMsgNoticePayload,
  PagedList,
} from '../typing';

export const API_HOME_MSG_MY_LIST = '/platform/home/msg/my_msg_list';
export const API_HOME_MSG_GET = '/platform/home/msg/get/';
export const API_HOME_MSG_READ_ONE = '/platform/home/msg/status/read_one/';
export const API_HOME_MSG_READ_ALL = '/platform/home/msg/status/read_all';
export const API_HOME_MSG_READ_MORE = '/platform/home/msg/status/read_more';
export const API_HOME_MSG_DATA = '/platform/home/msg/data';
export const API_HOME_MSG_WS = '/platform/home/msg/wsmsg';

export async function getWsMsg() {
  return request<HomeMsgNoticePayload>(API_HOME_MSG_WS, {
    method: 'GET',
    showCodeMessage: false,
  });
}

export async function getHomeMsgList(data: Record<string, unknown>) {
  return request<PagedList<HomeMsgListRecord>>(API_HOME_MSG_MY_LIST, {
    method: 'POST',
    data,
  });
}

export async function getHomeMsgInfo(id: string) {
  return request<HomeMsgDetail>(`${API_HOME_MSG_GET}${id}`, {
    method: 'POST',
  });
}

export async function getHomeMsgData() {
  return request<HomeMsgInitData>(API_HOME_MSG_DATA, {
    method: 'GET',
  });
}

export async function doReadAllHomeMsg() {
  return request<Record<string, never>>(API_HOME_MSG_READ_ALL, {
    method: 'POST',
    showSuccessMessage: true,
  });
}

export async function doReadMoreHomeMsg(ids: string) {
  return request<Record<string, never>>(API_HOME_MSG_READ_MORE, {
    method: 'POST',
    data: { ids },
    showSuccessMessage: true,
  });
}

export async function doReadOneHomeMsg(id: string) {
  return request<Record<string, never>>(`${API_HOME_MSG_READ_ONE}${id}`, {
    method: 'POST',
    showCodeMessage: false,
  });
}
