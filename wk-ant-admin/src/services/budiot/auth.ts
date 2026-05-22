import { request } from '@/utils/request';
import type {
  BudiotAuthInfo,
  CaptchaResult,
  LoginPayload,
  LoginResult,
  RsaResult,
} from './typing';

export const API_AUTH_LOGIN = '/platform/auth/login';
export const API_AUTH_CAPTCHA = '/platform/auth/captcha';
export const API_AUTH_RSA = '/platform/auth/rsa';
export const API_AUTH_SMSCODE = '/platform/auth/smscode';
export const API_AUTH_LOGOUT = '/platform/auth/logout';
export const API_AUTH_INFO = '/platform/auth/info';
export const API_AUTH_THEME = '/platform/auth/theme';
export const API_AUTH_CHECK_LOGINNAME = '/platform/auth/check/loginname';
export const API_AUTH_PWD_SENDCODE = '/platform/auth/pwd/sendcode';
export const API_AUTH_PWD_SAVE = '/platform/auth/pwd/save';

export async function getCaptcha() {
  return request<CaptchaResult>(API_AUTH_CAPTCHA, {
    method: 'GET',
    skipAuthFailureRedirect: true,
    skipErrorHandler: true,
    showCodeMessage: false,
  });
}

export async function getRsa() {
  return request<RsaResult>(API_AUTH_RSA, {
    method: 'GET',
    skipAuthFailureRedirect: true,
    skipErrorHandler: true,
    showCodeMessage: false,
  });
}

export async function getSmsCode(mobile: string, tenantName?: string) {
  return request<Record<string, never>>(API_AUTH_SMSCODE, {
    method: 'POST',
    data: { mobile, tenantName },
  });
}

export async function checkLoginname(loginname: string) {
  return request<Record<string, string>>(API_AUTH_CHECK_LOGINNAME, {
    method: 'POST',
    data: { loginname },
  });
}

export async function sendResetPwdCode(data: Record<string, unknown>) {
  return request<Record<string, never>>(API_AUTH_PWD_SENDCODE, {
    method: 'POST',
    data,
  });
}

export async function saveNewPwd(data: Record<string, unknown>) {
  return request<Record<string, never>>(API_AUTH_PWD_SAVE, {
    method: 'POST',
    data,
  });
}

export async function doLogin(payload: LoginPayload) {
  return request<LoginResult>(API_AUTH_LOGIN, {
    method: 'POST',
    data: payload,
    skipAuthFailureRedirect: true,
  });
}

export async function getUserInfo() {
  return request<BudiotAuthInfo>(API_AUTH_INFO, {
    method: 'GET',
  });
}

export async function logout() {
  return request<Record<string, never>>(API_AUTH_LOGOUT, {
    method: 'GET',
    showCodeMessage: false,
    skipAuthFailureRedirect: true,
  });
}

export async function saveThemeConfig(themeConfig: string) {
  return request<Record<string, never>>(API_AUTH_THEME, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    data: new URLSearchParams({ themeConfig }).toString(),
    showCodeMessage: true,
  });
}
