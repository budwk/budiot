import { API_BASE_URL, APP_ID, DEFAULT_LANG } from '@/constants/app';
import type { ManagedFileRecord, PagedList } from '@/services/budiot/typing';
import { downloadFile } from '@/utils/file';
import { request } from '@/utils/request';
import { getStoredToken } from '@/utils/session';

const API_FILE_LIST = '/platform/file/list';
const API_FILE_GET = '/platform/file/get/';
const API_FILE_UPLOAD = '/platform/file/upload';
const API_FILE_PREVIEW = '/platform/file/preview/';
const API_FILE_DOWNLOAD = '/platform/file/download/';
const API_FILE_DELETE = '/platform/file/delete/';
const API_FILE_PUBLIC = '/platform/file/public/';

export const getManagedFilePage = async (data: Record<string, unknown>) =>
  request<PagedList<ManagedFileRecord>>(API_FILE_LIST, {
    method: 'POST',
    data,
  });

export const getManagedFileDetail = async (id: string) =>
  request<ManagedFileRecord>(`${API_FILE_GET}${id}`, {
    method: 'GET',
  });

export const uploadManagedFile = async (formData: FormData) =>
  request<unknown>(API_FILE_UPLOAD, {
    method: 'POST',
    data: formData,
    showSuccessMessage: false,
  });

export const deleteManagedFile = async (id: string) =>
  request<unknown>(`${API_FILE_DELETE}${id}`, {
    method: 'DELETE',
    showSuccessMessage: true,
  });

export const updateManagedFilePublicFlag = async (id: string, publicFlag: boolean) =>
  request<unknown>(`${API_FILE_PUBLIC}${id}`, {
    method: 'POST',
    data: { publicFlag },
    showSuccessMessage: true,
  });

export const previewManagedFile = async (id: string) => {
  const response = await fetch(`${API_BASE_URL}${API_FILE_PREVIEW}${id}`, {
    method: 'GET',
    headers: {
      Authorization: getStoredToken() || '',
      lang: DEFAULT_LANG,
      appId: APP_ID,
    },
  });
  return response.blob();
};

export const downloadManagedFile = async (id: string, fileName: string) =>
  downloadFile({
    url: `${API_FILE_DOWNLOAD}${id}`,
    fileName,
  });
