import { request } from '@/utils/request';
import type {
  IotPagedList,
  IotProtocolDebugResult,
  IotProtocolMeta,
  IotProtocolRecord,
} from './typing';

const API_IOT_PROTOCOL_DATA = '/platform/iot/protocol/data';
const API_IOT_PROTOCOL_LIST = '/platform/iot/protocol/list';
const API_IOT_PROTOCOL_GET = '/platform/iot/protocol/get/';
const API_IOT_PROTOCOL_CREATE = '/platform/iot/protocol/create';
const API_IOT_PROTOCOL_UPDATE = '/platform/iot/protocol/update';
const API_IOT_PROTOCOL_DELETE = '/platform/iot/protocol/delete/';
const API_IOT_PROTOCOL_DEBUG = '/platform/iot/protocol/debug';

export const getIotProtocolMeta = async () =>
  request<IotProtocolMeta>(API_IOT_PROTOCOL_DATA, {
    method: 'GET',
  });

export const getIotProtocolPage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotProtocolRecord>>(API_IOT_PROTOCOL_LIST, {
    method: 'POST',
    params,
  });

export const getIotProtocolDetail = async (id: string) =>
  request<IotProtocolRecord>(`${API_IOT_PROTOCOL_GET}${id}`, {
    method: 'GET',
  });

export const createIotProtocol = async (data: Record<string, unknown>) =>
  request<IotProtocolRecord>(API_IOT_PROTOCOL_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotProtocol = async (data: Record<string, unknown>) =>
  request<IotProtocolRecord>(API_IOT_PROTOCOL_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotProtocol = async (id: string) =>
  request<unknown>(`${API_IOT_PROTOCOL_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const debugIotProtocol = async (data: Record<string, unknown>) =>
  request<IotProtocolDebugResult>(API_IOT_PROTOCOL_DEBUG, {
    method: 'POST',
    data,
  });
