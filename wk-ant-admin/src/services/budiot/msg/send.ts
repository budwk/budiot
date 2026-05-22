import type {
  MsgOption,
  MsgSendResultRecord,
  MsgTemplateRecord,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_MSG_SEND_DATA = '/platform/msg/send/data';
const API_MSG_SEND_CREATE = '/platform/msg/send/create';

export const getMsgSendMeta = async () =>
  request<{
    channels: Array<{ id: string; name: string; channelType?: string; providerType?: string }>;
    templates: MsgTemplateRecord[];
    bizTypes: MsgOption[];
  }>(API_MSG_SEND_DATA, {
    method: 'GET',
  });

export const createMsgSend = async (data: Record<string, unknown>) =>
  request<MsgSendResultRecord[]>(API_MSG_SEND_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
