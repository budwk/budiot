import { request } from '@/utils/request';
import type { IotGatewayMeta, IotGatewayRecord, IotPagedList } from './typing';

const API_IOT_GATEWAY_DATA = '/platform/iot/gateway/data';
const API_IOT_GATEWAY_LIST = '/platform/iot/gateway/list';
const API_IOT_GATEWAY_GET = '/platform/iot/gateway/get/';
const API_IOT_GATEWAY_CREATE = '/platform/iot/gateway/create';
const API_IOT_GATEWAY_UPDATE = '/platform/iot/gateway/update';
const API_IOT_GATEWAY_START = '/platform/iot/gateway/start/';
const API_IOT_GATEWAY_SUSPEND = '/platform/iot/gateway/suspend/';
const API_IOT_GATEWAY_STOP = '/platform/iot/gateway/stop/';
const API_IOT_GATEWAY_DELETE = '/platform/iot/gateway/delete/';

export const getIotGatewayMeta = async () =>
  request<IotGatewayMeta>(API_IOT_GATEWAY_DATA, {
    method: 'GET',
  });

export const getIotGatewayPage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotGatewayRecord>>(API_IOT_GATEWAY_LIST, {
    method: 'POST',
    params,
  });

export const getIotGatewayDetail = async (id: string) =>
  request<IotGatewayRecord>(`${API_IOT_GATEWAY_GET}${id}`, {
    method: 'GET',
  });

export const createIotGateway = async (data: Record<string, unknown>) =>
  request<IotGatewayRecord>(API_IOT_GATEWAY_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotGateway = async (data: Record<string, unknown>) =>
  request<IotGatewayRecord>(API_IOT_GATEWAY_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotGateway = async (id: string) =>
  request<unknown>(`${API_IOT_GATEWAY_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const startIotGateway = async (id: string) =>
  request<unknown>(`${API_IOT_GATEWAY_START}${id}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const suspendIotGateway = async (id: string) =>
  request<unknown>(`${API_IOT_GATEWAY_SUSPEND}${id}`, {
    method: 'POST',
    showSuccessMessage: true,
  });

export const stopIotGateway = async (id: string) =>
  request<unknown>(`${API_IOT_GATEWAY_STOP}${id}`, {
    method: 'POST',
    showSuccessMessage: true,
  });
