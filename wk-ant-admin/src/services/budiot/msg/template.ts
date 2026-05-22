import type { MsgOption, MsgTemplateRecord, PagedList } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_MSG_TEMPLATE_LIST = '/platform/msg/template/list';
const API_MSG_TEMPLATE_DATA = '/platform/msg/template/data';
const API_MSG_TEMPLATE_GET = '/platform/msg/template/get/';
const API_MSG_TEMPLATE_CREATE = '/platform/msg/template/create';
const API_MSG_TEMPLATE_UPDATE = '/platform/msg/template/update';
const API_MSG_TEMPLATE_DELETE = '/platform/msg/template/delete/';

export const getMsgTemplateMeta = async () =>
  request<{ bizTypes: MsgOption[]; channels: Array<{ id: string; name: string; channelType?: string; providerType?: string }> }>(
    API_MSG_TEMPLATE_DATA,
    {
      method: 'GET',
    },
  );

export const getMsgTemplatePage = async (params: Record<string, unknown>) =>
  request<PagedList<MsgTemplateRecord>>(API_MSG_TEMPLATE_LIST, {
    method: 'POST',
    params,
  });

export const getMsgTemplateDetail = async (id: string) =>
  request<MsgTemplateRecord>(`${API_MSG_TEMPLATE_GET}${id}`, {
    method: 'GET',
  });

export const createMsgTemplate = async (data: Record<string, unknown>) =>
  request<MsgTemplateRecord>(API_MSG_TEMPLATE_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateMsgTemplate = async (data: Record<string, unknown>) =>
  request<MsgTemplateRecord>(API_MSG_TEMPLATE_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteMsgTemplate = async (id: string) =>
  request<unknown>(`${API_MSG_TEMPLATE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
