import { request } from '@/utils/request';
import type { IotCommandRecord } from './typing';

const API_IOT_COMMAND_DATA = '/platform/iot/command/data';
const API_IOT_COMMAND_CREATE = '/platform/iot/command/create';
const API_IOT_COMMAND_CANCEL = '/platform/iot/command/cancel/';
const API_IOT_COMMAND_PENDING = '/platform/iot/command/pending';
const API_IOT_COMMAND_LOGS = '/platform/iot/command/logs';

export const getIotCommandMeta = async () =>
  request<Record<string, unknown>>(API_IOT_COMMAND_DATA, {
    method: 'GET',
  });

export const createIotCommand = async (data: Record<string, unknown>) =>
  request<IotCommandRecord>(API_IOT_COMMAND_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const cancelIotCommand = async (id: string) =>
  request<unknown>(`${API_IOT_COMMAND_CANCEL}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getIotPendingCommands = async (deviceId: string) =>
  request<IotCommandRecord[]>(API_IOT_COMMAND_PENDING, {
    method: 'GET',
    params: { deviceId },
  });

export const getIotCommandLogs = async (deviceId: string) =>
  request<IotCommandRecord[]>(API_IOT_COMMAND_LOGS, {
    method: 'GET',
    params: { deviceId },
  });
