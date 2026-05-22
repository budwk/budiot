import { APP_ID } from '@/constants/app';
import type {
  BudiotAppOption,
  BudiotAuthAppItem,
  BudiotAuthInfo,
} from '@/services/budiot/typing';

const isExternalPath = (path?: string) => Boolean(path && /^(https?:)?\/\//.test(path));

export const normalizeAppPath = (path?: string) => {
  if (!path) {
    return undefined;
  }
  if (isExternalPath(path) || path.startsWith('/')) {
    return path;
  }
  return `/${path}`;
};

export const normalizeAuthApps = (
  apps: BudiotAuthAppItem[] = [],
): BudiotAppOption[] =>
  apps.map((item) =>
    typeof item === 'string'
      ? {
          id: item,
          name: item,
        }
      : {
          ...item,
          path: normalizeAppPath(item.path),
        },
  );

export const getAvailableApps = (
  authInfo?: Pick<BudiotAuthInfo, 'apps'>,
): BudiotAppOption[] =>
  normalizeAuthApps(authInfo?.apps || []).filter(
    (item) => !item.hidden && !item.disabled,
  );

export const getAppById = (
  authInfo: Pick<BudiotAuthInfo, 'apps'> | undefined,
  appId?: string,
) => {
  if (!appId) {
    return undefined;
  }
  return getAvailableApps(authInfo).find((item) => item.id === appId);
};

export const resolveActiveAppId = (
  authInfo?: Pick<BudiotAuthInfo, 'apps'>,
  preferredAppId?: string,
) => {
  const apps = getAvailableApps(authInfo);
  const candidates = [preferredAppId, APP_ID, apps[0]?.id].filter(Boolean) as string[];
  const matched = candidates.find((item) => apps.some((app) => app.id === item));
  return matched || preferredAppId || APP_ID;
};
