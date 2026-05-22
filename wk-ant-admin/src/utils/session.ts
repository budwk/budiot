import type { BudiotAuthInfo, PlatformInfo } from '@/services/budiot/typing';
import { AUTH_STATE_KEY, PLATFORM_INFO_KEY } from '@/constants/storage';

type StoredAuthState = {
  token?: string;
  authInfo?: BudiotAuthInfo;
  appId?: string;
};

const readJson = <T>(key: string): T | undefined => {
  if (typeof window === 'undefined') {
    return undefined;
  }
  const raw = window.localStorage.getItem(key);
  if (!raw) {
    return undefined;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    window.localStorage.removeItem(key);
    return undefined;
  }
};

const writeJson = (key: string, value: unknown) => {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(key, JSON.stringify(value));
};

export const getStoredAuthState = () => readJson<StoredAuthState>(AUTH_STATE_KEY);

export const getStoredToken = () => getStoredAuthState()?.token;

export const setStoredToken = (token: string) => {
  const state = getStoredAuthState() || {};
  writeJson(AUTH_STATE_KEY, { ...state, token });
};

export const setStoredAuthInfo = (authInfo: BudiotAuthInfo) => {
  const state = getStoredAuthState() || {};
  writeJson(AUTH_STATE_KEY, { ...state, authInfo });
};

export const getStoredAuthInfo = () => getStoredAuthState()?.authInfo;

export const getStoredAppId = () => getStoredAuthState()?.appId;

export const setStoredAppId = (appId: string) => {
  const state = getStoredAuthState() || {};
  writeJson(AUTH_STATE_KEY, { ...state, appId });
};

export const clearStoredAuthState = () => {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(AUTH_STATE_KEY);
};

export const getStoredPlatformInfo = () =>
  readJson<PlatformInfo>(PLATFORM_INFO_KEY);

export const setStoredPlatformInfo = (platformInfo: PlatformInfo) => {
  writeJson(PLATFORM_INFO_KEY, platformInfo);
};

export const clearStoredPlatformInfo = () => {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(PLATFORM_INFO_KEY);
};

export const clearRuntimeSession = () => {
  clearStoredAuthState();
};
