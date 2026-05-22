import { HOME_PATH } from '@/constants/app';
import type { BudiotAuthInfo } from '@/services/budiot/typing';
import { getAppById, normalizeAppPath } from './app';
import { buildRuntimeMenuData } from './menu';

const findFirstMenuPath = (items: any[] = []): string | undefined => {
  for (const item of items) {
    if (item?.path) {
      return item.path;
    }
    const childPath = findFirstMenuPath(item?.children || item?.routes || []);
    if (childPath) {
      return childPath;
    }
  }
  return undefined;
};

export const resolveRuntimeAppHomePath = (
  authInfo: BudiotAuthInfo | undefined,
  appId?: string,
  fallbackPath: string = HOME_PATH,
) => {
  if (!authInfo || !appId) {
    return fallbackPath;
  }
  const runtimeMenu = buildRuntimeMenuData(authInfo, appId);
  const app = getAppById(authInfo, appId);
  return normalizeAppPath(app?.path) || findFirstMenuPath(runtimeMenu.menuData) || fallbackPath;
};
