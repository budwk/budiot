import { API_BASE_URL, APP_ID, DEFAULT_LANG } from '@/constants/app';
import { PLATFORM_UPLOAD_URL } from '@/services/budiot/common';
import type { ApiResponse } from '@/services/budiot/typing';
import { appNotification } from '@/utils/antdApp';
import { getStoredToken } from '@/utils/session';

const buildQueryString = (params?: Record<string, unknown>) => {
  if (!params) {
    return '';
  }

  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, String(value));
    }
  });

  const query = searchParams.toString();
  return query ? `?${query}` : '';
};

const saveBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const validateUploadFile = (file: File, maxSizeKb: string | number) => {
  const maxSize = Number(maxSizeKb) * 1024;

  if (!file.name || typeof file.size === 'undefined') {
    return '上传文件数据不完整';
  }

  if (file.size > maxSize) {
    return '上传文件大小超出允许范围';
  }

  return undefined;
};

export async function uploadFile(
  formData: FormData,
  options?: {
    params?: Record<string, unknown>;
    type?: 'file' | 'image' | 'video';
  },
) {
  const type = options?.type || 'file';
  const query = buildQueryString(options?.params);
  const response = await fetch(`${API_BASE_URL}${PLATFORM_UPLOAD_URL}${type}${query}`, {
    method: 'POST',
    body: formData,
    headers: {
      Authorization: getStoredToken() || '',
      lang: DEFAULT_LANG,
      appId: APP_ID,
    },
  });

  const json = (await response.json()) as ApiResponse<unknown>;
  if (json.code !== 200) {
    appNotification.error({
      title: json.msg || '上传失败',
    });
    throw json;
  }

  return json;
}

export async function downloadFile(options: {
  url: string;
  method?: 'GET' | 'POST';
  params?: Record<string, unknown>;
  fileName: string;
}) {
  const method = options.method || 'GET';
  const query = method === 'GET' ? buildQueryString(options.params) : '';
  const response = await fetch(`${API_BASE_URL}${options.url}${query}`, {
    method,
    headers: {
      Authorization: getStoredToken() || '',
      lang: DEFAULT_LANG,
      appId: APP_ID,
      ...(method === 'POST'
        ? { 'Content-Type': 'application/json' }
        : undefined),
    },
    body: method === 'POST' ? JSON.stringify(options.params || {}) : undefined,
  });

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const json = (await response.json()) as ApiResponse<unknown>;
    appNotification.error({
      title: json.msg || '下载失败',
    });
    throw json;
  }

  const blob = await response.blob();
  saveBlob(blob, options.fileName);
}
