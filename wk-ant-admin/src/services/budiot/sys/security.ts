import type { SysSecurityConfig } from '@/services/budiot/typing';
import { request } from '@/utils/request';

const API_SYS_SECURITY_GET = '/platform/sys/security/get';
const API_SYS_SECURITY_SAVE = '/platform/sys/security/save';

export const getSecurityConfig = async () =>
  request<SysSecurityConfig>(API_SYS_SECURITY_GET, {
    method: 'GET',
  });

export const saveSecurityConfig = async (data: SysSecurityConfig) =>
  request<unknown>(API_SYS_SECURITY_SAVE, {
    method: 'POST',
    data,
    showSuccessMessage: true,
  });
