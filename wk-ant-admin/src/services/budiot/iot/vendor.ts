import { request } from '@/utils/request';
import type { IotPagedList, IotVendorRecord } from './typing';

const API_IOT_VENDOR_LIST = '/platform/iot/vendor/list';
const API_IOT_VENDOR_GET = '/platform/iot/vendor/get/';
const API_IOT_VENDOR_CREATE = '/platform/iot/vendor/create';
const API_IOT_VENDOR_UPDATE = '/platform/iot/vendor/update';
const API_IOT_VENDOR_DELETE = '/platform/iot/vendor/delete/';

export const getIotVendorPage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotVendorRecord>>(API_IOT_VENDOR_LIST, {
    method: 'POST',
    params,
  });

export const getIotVendorDetail = async (id: string) =>
  request<IotVendorRecord>(`${API_IOT_VENDOR_GET}${id}`, {
    method: 'GET',
  });

export const createIotVendor = async (data: Record<string, unknown>) =>
  request<IotVendorRecord>(API_IOT_VENDOR_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotVendor = async (data: Record<string, unknown>) =>
  request<IotVendorRecord>(API_IOT_VENDOR_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotVendor = async (id: string) =>
  request<unknown>(`${API_IOT_VENDOR_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
