import { API_BASE_URL } from '@/constants/app';

const joinPath = (basePath: string, nextPath: string) => {
  const normalizedBase = basePath.endsWith('/')
    ? basePath.slice(0, -1)
    : basePath;
  const normalizedNext = nextPath.startsWith('/') ? nextPath : `/${nextPath}`;
  return `${normalizedBase}${normalizedNext}`;
};

export const buildWebSocketUrl = (path: string) => {
  const absoluteBase = new URL(API_BASE_URL, window.location.origin);
  const wsProtocol =
    absoluteBase.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${absoluteBase.host}${joinPath(
    absoluteBase.pathname,
    path,
  )}`;
};

export const PLATFORM_WEBSOCKET_PATH = '/platform/ws';
