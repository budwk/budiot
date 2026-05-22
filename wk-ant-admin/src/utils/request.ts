import { history, request as umiRequest } from '@umijs/max';
import { HOME_PATH, LOGIN_PATH } from '@/constants/app';
import { appNotification } from '@/utils/antdApp';
import { clearRuntimeSession } from '@/utils/session';
import type { ApiResponse } from '@/services/budiot/typing';

export type BudiotRequestOptions = Record<string, unknown> & {
  showCodeMessage?: boolean;
  showSuccessMessage?: boolean;
  skipAuthFailureRedirect?: boolean;
};

let authRedirecting = false;

const buildValidationMessage = (data: unknown) => {
  if (!data || typeof data !== 'object') {
    return '参数错误';
  }

  const lines = Object.entries(data as Record<string, any>).map(
    ([field, value]) => {
      const name = value?.name || field;
      const msg = value?.msg || '参数错误';
      return `${name} : ${msg}`;
    },
  );

  return lines.join('\n');
};

export const redirectToLogin = () => {
  if (typeof window === 'undefined' || authRedirecting) {
    return;
  }
  authRedirecting = true;
  clearRuntimeSession();
  const isLoginPage = window.location.pathname === LOGIN_PATH;
  const redirect =
    !isLoginPage && window.location.pathname !== HOME_PATH
      ? `${window.location.pathname}${window.location.search}`
      : '';
  const target = redirect
    ? `${LOGIN_PATH}?redirect=${encodeURIComponent(redirect)}`
    : LOGIN_PATH;
  window.location.replace(target);
};

export async function request<T>(
  url: string,
  options: BudiotRequestOptions = {},
): Promise<ApiResponse<T>> {
  const response = await umiRequest<ApiResponse<T>>(url, options);

  if (response.code === 200) {
    if (options.showSuccessMessage && response.msg) {
      appNotification.success({
        title: response.msg,
      });
    }
    return response;
  }

  if (response.code === 401) {
    if (!options.skipAuthFailureRedirect) {
      appNotification.error({
        title: '登录失效',
        description: '无效的会话，或者会话已过期，请重新登录。',
      });
      redirectToLogin();
    }
    throw response;
  }

  if (response.code === 500200) {
    appNotification.error({
      title: '参数错误',
      description: buildValidationMessage(response.data),
    });
    throw response;
  }

  if (response.code === 302) {
    const routePath = (response.data as Record<string, string>)?.routePath;
    history.push(routePath || HOME_PATH);
    throw response;
  }

  if (options.showCodeMessage !== false) {
    appNotification.error({
      title: response.msg || '请求失败',
    });
  }

  throw response;
}
