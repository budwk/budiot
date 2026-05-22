import type { MsgHistoryRecord, MsgOption, PagedList } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_MSG_HISTORY_DATA = '/platform/msg/history/data';
const API_MSG_HISTORY_LIST = '/platform/msg/history/list';
const API_MSG_HISTORY_GET = '/platform/msg/history/get/';

export const getMsgHistoryMeta = async () =>
  request<{
    channels: Array<{ id: string; name: string }>;
    channelTypes: MsgOption[];
    providerTypes: MsgOption[];
    statuses: MsgOption[];
  }>(API_MSG_HISTORY_DATA, {
    method: 'GET',
  });

export const getMsgHistoryPage = async (data: Record<string, unknown>) =>
  request<PagedList<MsgHistoryRecord>>(API_MSG_HISTORY_LIST, {
    method: 'POST',
    data,
  });

export const getMsgHistoryDetail = async (id: string, createdAt: number) =>
  request<MsgHistoryRecord>(`${API_MSG_HISTORY_GET}${id}`, {
    method: 'GET',
    params: { createdAt },
  });
