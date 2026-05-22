import { request } from '@/utils/request';
import type {
  IotPagedList,
  IotProductMeta,
  IotProductRecord,
  IotProductThingModelRecord,
} from './typing';

const API_IOT_PRODUCT_DATA = '/platform/iot/product/data';
const API_IOT_PRODUCT_LIST = '/platform/iot/product/list';
const API_IOT_PRODUCT_GET = '/platform/iot/product/get/';
const API_IOT_PRODUCT_THING_MODEL_GET = '/platform/iot/product/thing-model/get/';
const API_IOT_PRODUCT_CREATE = '/platform/iot/product/create';
const API_IOT_PRODUCT_UPDATE = '/platform/iot/product/update';
const API_IOT_PRODUCT_THING_MODEL_UPDATE = '/platform/iot/product/thing-model/update';
const API_IOT_PRODUCT_DELETE = '/platform/iot/product/delete/';

export const getIotProductMeta = async () =>
  request<IotProductMeta>(API_IOT_PRODUCT_DATA, {
    method: 'GET',
  });

export const getIotProductPage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotProductRecord>>(API_IOT_PRODUCT_LIST, {
    method: 'POST',
    params,
  });

export const getIotProductDetail = async (id: string) =>
  request<IotProductRecord>(`${API_IOT_PRODUCT_GET}${id}`, {
    method: 'GET',
  });

export const getIotProductThingModel = async (id: string) =>
  request<IotProductThingModelRecord>(`${API_IOT_PRODUCT_THING_MODEL_GET}${id}`, {
    method: 'GET',
  });

export const createIotProduct = async (data: Record<string, unknown>) =>
  request<IotProductRecord>(API_IOT_PRODUCT_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotProduct = async (data: Record<string, unknown>) =>
  request<IotProductRecord>(API_IOT_PRODUCT_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotProductThingModel = async (data: Record<string, unknown>) =>
  request<IotProductThingModelRecord>(API_IOT_PRODUCT_THING_MODEL_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotProduct = async (id: string) =>
  request<unknown>(`${API_IOT_PRODUCT_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
