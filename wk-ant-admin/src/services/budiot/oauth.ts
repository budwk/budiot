import type { LoginResult, OauthProvider } from './typing';
import { request } from '@/utils/request';

const API_OAUTH_PROVIDERS = '/platform/oauth/providers';
const API_OAUTH_AUTHORIZE = '/platform/oauth/authorize';
const API_OAUTH_LOGIN_TICKET = '/platform/oauth/login/ticket';
const API_OAUTH_BIND_LOGIN = '/platform/oauth/bind/login';

export const getOauthProviders = async () =>
  request<OauthProvider[]>(API_OAUTH_PROVIDERS, {
    method: 'GET',
    skipAuthFailureRedirect: true,
    skipErrorHandler: true,
    showCodeMessage: false,
  });

export const getOauthAuthorizeUrl = async (
  provider: string,
  params: Record<string, unknown>,
) =>
  request<{ authorizeUrl: string }>(`${API_OAUTH_AUTHORIZE}/${provider}`, {
    method: 'GET',
    params,
    skipAuthFailureRedirect: true,
    skipErrorHandler: true,
    showCodeMessage: false,
  });

export const exchangeOauthLoginTicket = async (ticket: string) =>
  request<LoginResult>(API_OAUTH_LOGIN_TICKET, {
    method: 'POST',
    data: { ticket },
    skipAuthFailureRedirect: true,
  });

export const bindOauthLogin = async (data: Record<string, unknown>) =>
  request<LoginResult>(API_OAUTH_BIND_LOGIN, {
    method: 'POST',
    data,
    skipAuthFailureRedirect: true,
  });
