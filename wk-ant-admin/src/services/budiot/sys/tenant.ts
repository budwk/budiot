import type {
  PagedList,
  SysTenantPackageRecord,
  SysTenantRecord,
  TenantMenuTreeNode,
} from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_TENANT_LIST = '/platform/sys/tenant/list';
const API_SYS_TENANT_DATA = '/platform/sys/tenant/data';
const API_SYS_TENANT_GET = '/platform/sys/tenant/get/';
const API_SYS_TENANT_CREATE = '/platform/sys/tenant/create';
const API_SYS_TENANT_UPDATE = '/platform/sys/tenant/update';
const API_SYS_TENANT_DISABLED = '/platform/sys/tenant/disabled';
const API_SYS_TENANT_EXPIRE = '/platform/sys/tenant/expire';
const API_SYS_TENANT_DELETE = '/platform/sys/tenant/delete/';

const API_SYS_TENANT_PACKAGE_LIST = '/platform/sys/tenant/package/list';
const API_SYS_TENANT_PACKAGE_DATA = '/platform/sys/tenant/package/data';
const API_SYS_TENANT_PACKAGE_GET = '/platform/sys/tenant/package/get/';
const API_SYS_TENANT_PACKAGE_CREATE = '/platform/sys/tenant/package/create';
const API_SYS_TENANT_PACKAGE_UPDATE = '/platform/sys/tenant/package/update';
const API_SYS_TENANT_PACKAGE_DISABLED = '/platform/sys/tenant/package/disabled';
const API_SYS_TENANT_PACKAGE_DELETE = '/platform/sys/tenant/package/delete/';

export const getTenantPage = async (data: Record<string, unknown>) =>
  request<PagedList<SysTenantRecord>>(API_SYS_TENANT_LIST, {
    method: 'POST',
    data,
  });

export const getTenantMeta = async () =>
  request<{ packages: SysTenantPackageRecord[] }>(API_SYS_TENANT_DATA, {
    method: 'GET',
  });

export const getTenantDetail = async (id: string) =>
  request<SysTenantRecord>(`${API_SYS_TENANT_GET}${id}`, {
    method: 'GET',
  });

export const createTenant = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_TENANT_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateTenant = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_TENANT_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateTenantDisabled = async (data: { id: string; disabled: boolean }) =>
  request<unknown>(API_SYS_TENANT_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateTenantExpire = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_TENANT_EXPIRE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteTenant = async (id: string) =>
  request<unknown>(`${API_SYS_TENANT_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const getTenantPackagePage = async (data: Record<string, unknown>) =>
  request<PagedList<SysTenantPackageRecord>>(API_SYS_TENANT_PACKAGE_LIST, {
    method: 'POST',
    data,
  });

export const getTenantPackageMeta = async () =>
  request<{ menuTree: TenantMenuTreeNode[] }>(API_SYS_TENANT_PACKAGE_DATA, {
    method: 'GET',
  });

export const getTenantPackageDetail = async (id: string) =>
  request<{ package: SysTenantPackageRecord; menuIds: string[] }>(
    `${API_SYS_TENANT_PACKAGE_GET}${id}`,
    {
      method: 'GET',
    },
  );

export const createTenantPackage = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_TENANT_PACKAGE_CREATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateTenantPackage = async (data: Record<string, unknown>) =>
  request<unknown>(API_SYS_TENANT_PACKAGE_UPDATE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const updateTenantPackageDisabled = async (data: { id: string; disabled: boolean }) =>
  request<unknown>(API_SYS_TENANT_PACKAGE_DISABLED, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });

export const deleteTenantPackage = async (id: string) =>
  request<unknown>(`${API_SYS_TENANT_PACKAGE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });
