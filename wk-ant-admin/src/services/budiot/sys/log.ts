import type {
  BudiotAppOption,
  PagedList,
  SysLogRecord,
  SysLogTypeOption,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_LOG_LIST = '/platform/sys/log/list';
const API_SYS_LOG_DATA = '/platform/sys/log/data';
const API_SYS_LOG_DELETE = '/platform/sys/log/delete/';
const API_SYS_LOG_CLEAR = '/platform/sys/log/clear';

export const getLogPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysLogRecord>>(API_SYS_LOG_LIST, {
    method: 'POST',
    data,
  });

export const getLogMeta = async () =>
  request<{ apps: BudiotAppOption[]; types: SysLogTypeOption[] }>(API_SYS_LOG_DATA, {
    method: 'GET',
  });

export const deleteLog = async (id: string, createdAt?: number) =>
  request<unknown>(`${API_SYS_LOG_DELETE}${id}`, {
    method: 'DELETE',
    params: { createdAt },
    showSuccessMessage: true,
  });

export const clearLogs = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_LOG_CLEAR, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
