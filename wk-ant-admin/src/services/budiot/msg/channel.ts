import type { MsgChannelRecord, MsgOption, PagedList } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_MSG_CHANNEL_LIST = '/platform/msg/channel/list';
const API_MSG_CHANNEL_DATA = '/platform/msg/channel/data';
const API_MSG_CHANNEL_GET = '/platform/msg/channel/get/';
const API_MSG_CHANNEL_CREATE = '/platform/msg/channel/create';
const API_MSG_CHANNEL_UPDATE = '/platform/msg/channel/update';
const API_MSG_CHANNEL_DELETE = '/platform/msg/channel/delete/';
const API_MSG_CHANNEL_DEFAULT = '/platform/msg/channel/default';

export const getMsgChannelMeta = async () =>
  request<{ channelTypes: MsgOption[]; providerTypes: MsgOption[] }>(API_MSG_CHANNEL_DATA, {
    method: 'GET',
  });

export const getMsgChannelPage = async (params: Record<string, unknown>) =>
  request<PagedList<MsgChannelRecord>>(API_MSG_CHANNEL_LIST, {
    method: 'POST',
    params,
  });

export const getMsgChannelDetail = async (id: string) =>
  request<MsgChannelRecord>(`${API_MSG_CHANNEL_GET}${id}`, {
    method: 'GET',
  });

export const createMsgChannel = async (data: Record<string, unknown>) =>
  request<MsgChannelRecord>(API_MSG_CHANNEL_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateMsgChannel = async (data: Record<string, unknown>) =>
  request<MsgChannelRecord>(API_MSG_CHANNEL_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteMsgChannel = async (id: string) =>
  request<unknown>(`${API_MSG_CHANNEL_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const setDefaultMsgChannel = async (id: string) =>
  request<unknown>(API_MSG_CHANNEL_DEFAULT, {
    method: 'POST',
    params: { id },
    showSuccessMessage: true,
  });
