import { request } from '@/utils/request';
import type {
  IotPagedList,
  IotRuleMeta,
  IotRuleProductMeta,
  IotRuleRecord,
} from './typing';

const API_IOT_RULE_DATA = '/platform/iot/rule/data';
const API_IOT_RULE_META = '/platform/iot/rule/meta/';
const API_IOT_RULE_LIST = '/platform/iot/rule/list';
const API_IOT_RULE_GET = '/platform/iot/rule/get/';
const API_IOT_RULE_CREATE = '/platform/iot/rule/create';
const API_IOT_RULE_UPDATE = '/platform/iot/rule/update';
const API_IOT_RULE_DELETE = '/platform/iot/rule/delete/';

export const getIotRuleMeta = async () =>
  request<IotRuleMeta>(API_IOT_RULE_DATA, {
    method: 'GET',
  });

export const getIotRuleProductMeta = async (productId: string) =>
  request<IotRuleProductMeta>(`${API_IOT_RULE_META}${productId}`, {
    method: 'GET',
  });

export const getIotRulePage = async (params: Record<string, unknown>) =>
  request<IotPagedList<IotRuleRecord>>(API_IOT_RULE_LIST, {
    method: 'POST',
    params,
  });

export const getIotRuleDetail = async (id: string) =>
  request<IotRuleRecord>(`${API_IOT_RULE_GET}${id}`, {
    method: 'GET',
  });

export const createIotRule = async (data: Record<string, unknown>) =>
  request<IotRuleRecord>(API_IOT_RULE_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateIotRule = async (data: Record<string, unknown>) =>
  request<IotRuleRecord>(API_IOT_RULE_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteIotRule = async (id: string) =>
  request<unknown>(`${API_IOT_RULE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
