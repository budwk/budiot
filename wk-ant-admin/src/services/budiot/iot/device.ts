import { request } from '@/utils/request';
import type {
  IotDeviceDetail,
  IotDeviceEventLogRecord,
  IotDeviceMeta,
  IotDeviceRawLogRecord,
  IotDeviceRecord,
  IotDeviceDataLogRecord,
  IotPagedList,
  IotCommandRecord,
} from './typing';

const API_IOT_DEVICE_DATA = '/platform/iot/device/data';
const API_IOT_DEVICE_LIST = '/platform/iot/device/list';
const API_IOT_DEVICE_GET = '/platform/iot/device/get/';
const API_IOT_DEVICE_RAW_LOGS = '/platform/iot/device/raw/logs';
const API_IOT_DEVICE_DATA_LOGS = '/platform/iot/device/data/logs';
const API_IOT_DEVICE_EVENT_LOGS = '/platform/iot/device/event/logs';
const API_IOT_DEVICE_PENDING_COMMANDS = '/platform/iot/device/command/pending';
const API_IOT_DEVICE_COMMAND_LOGS = '/platform/iot/device/command/logs';
const API_IOT_DEVICE_CREATE = '/platform/iot/device/create';
const API_IOT_DEVICE_BATCH_CREATE = '/platform/iot/device/batch_create';
const API_IOT_DEVICE_UPDATE = '/platform/iot/device/update';
const API_IOT_DEVICE_DELETE = '/platform/iot/device/delete/';

export const getIotDeviceMeta = async () =>
  request<IotDeviceMeta>(API_IOT_DEVICE_DATA, {
    method: 'GET',
  });

export const getIotDevicePage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotDeviceRecord>>(API_IOT_DEVICE_LIST, {
    method: 'POST',
    params,
  });

export const getIotDeviceDetail = async (id: string) =>
  request<IotDeviceDetail>(`${API_IOT_DEVICE_GET}${id}`, {
    method: 'GET',
  });

export const getIotDeviceRawLogs = async (params: {
  deviceId: string;
  startAt?: number;
  endAt?: number;
  pageNo?: number;
  pageSize?: number;
}) =>
  request<IotPagedList<IotDeviceRawLogRecord>>(API_IOT_DEVICE_RAW_LOGS, {
    method: 'GET',
    params,
  });

export const getIotDeviceDataLogs = async (params: {
  deviceId: string;
  startAt?: number;
  endAt?: number;
  pageNo?: number;
  pageSize?: number;
}) =>
  request<IotPagedList<IotDeviceDataLogRecord>>(API_IOT_DEVICE_DATA_LOGS, {
    method: 'GET',
    params,
  });

export const getIotDeviceEventLogs = async (params: {
  deviceId: string;
  startAt?: number;
  endAt?: number;
  pageNo?: number;
  pageSize?: number;
}) =>
  request<IotPagedList<IotDeviceEventLogRecord>>(API_IOT_DEVICE_EVENT_LOGS, {
    method: 'GET',
    params,
  });

export const getIotDevicePendingCommands = async (params: {
  deviceId: string;
  startAt?: number;
  endAt?: number;
  pageNo?: number;
  pageSize?: number;
}) =>
  request<IotPagedList<IotCommandRecord>>(API_IOT_DEVICE_PENDING_COMMANDS, {
    method: 'GET',
    params,
  });

export const getIotDeviceCommandLogs = async (params: {
  deviceId: string;
  startAt?: number;
  endAt?: number;
  pageNo?: number;
  pageSize?: number;
}) =>
  request<IotPagedList<IotCommandRecord>>(API_IOT_DEVICE_COMMAND_LOGS, {
    method: 'GET',
    params,
  });

export const createIotDevice = async (data: Record<string, unknown>) =>
  request<IotDeviceRecord>(API_IOT_DEVICE_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const batchCreateIotDevice = async (data: Record<string, unknown>) =>
  request<unknown>(API_IOT_DEVICE_BATCH_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotDevice = async (data: Record<string, unknown>) =>
  request<IotDeviceRecord>(API_IOT_DEVICE_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotDevice = async (id: string) =>
  request<unknown>(`${API_IOT_DEVICE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
