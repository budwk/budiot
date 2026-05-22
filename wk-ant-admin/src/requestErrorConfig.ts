import type { RequestConfig } from '@umijs/max';
import { appNotification } from '@/utils/antdApp';
import { redirectToLogin } from '@/utils/request';

export const errorConfig: RequestConfig = {
  errorConfig: {
    errorHandler: (error: any, opts: any) => {
      if (opts?.skipErrorHandler) {
        throw error;
      }

      if (error?.response?.status === 401) {
        appNotification.error({
          title: '登录失效',
          description: '无效的会话，或者会话已过期，请重新登录。',
        });
        redirectToLogin();
        throw error;
      }

      if (error?.response?.status) {
        const description =
          error.response.status === 404
            ? `请求地址不存在：${error.response.url || ''}`
            : `请求失败，状态码：${error.response.status}`;

        appNotification.error({
          title: '网络请求异常',
          description,
        });
        return;
      }

      if (error?.message?.includes('timeout')) {
        appNotification.error({
          title: '网络请求超时',
        });
        return;
      }

      appNotification.error({
        title: '请求失败',
        description: error?.message || '请稍后重试',
      });
    },
  },
};
