import { HOME_PATH } from '@/constants/app';
import { renderNamedIcon } from '@/components/IconPicker';
import {
  APP_ROUTE_DEFINITIONS,
  findRouteDefinition,
} from '@/constants/routeManifest';
import type { BackendMenuItem, BudiotAuthInfo } from '@/services/budiot/typing';
import type { ReactNode } from 'react';
import { normalizeAuthApps } from '@/utils/app';

export interface RuntimeRouteMeta {
  path: string;
  title: string;
  alias?: string;
  permission?: string;
  tree?: string;
  breadcrumb: string[];
  activeMenu?: string;
}

export interface RuntimeMenuItem {
  path?: string;
  name: string;
  icon?: ReactNode | string;
  hideInMenu?: boolean;
  target?: string;
  children?: RuntimeMenuItem[];
  routes?: RuntimeMenuItem[];
}

const getMenuBucket = (
  menus: Record<string, BackendMenuItem[]> = {},
  key?: string,
) => {
  if (!key) return undefined;
  if (menus[key]) return menus[key];
  const normalizedKey = key.toUpperCase();
  const matchedKey = Object.keys(menus).find(
    (item) => item.toUpperCase() === normalizedKey,
  );
  return matchedKey ? menus[matchedKey] : undefined;
};

const resolveAppMenus = (
  authInfo: BudiotAuthInfo,
  appId?: string,
) => {
  const menus = authInfo.menus || {};
  const direct = getMenuBucket(menus, appId);
  if (direct?.length) return direct;

  const appMatch = (authInfo.apps || [])
    .map((item) => getMenuBucket(menus, typeof item === 'string' ? item : item.id))
    .find((item) => item?.length);
  if (appMatch?.length) return appMatch;

  const normalizedApps = normalizeAuthApps(authInfo.apps || []);
  const normalizedMatch = normalizedApps
    .map((item) => getMenuBucket(menus, item.id))
    .find((item) => item?.length);
  if (normalizedMatch?.length) return normalizedMatch;

  const firstBusinessMenu = Object.entries(menus).find(
    ([key, value]) => key.toUpperCase() !== 'COMMON' && value?.length,
  );
  return firstBusinessMenu?.[1] || [];
};

export interface RuntimeMenuBuildResult {
  menuData: RuntimeMenuItem[];
  routeMetaMap: Record<string, RuntimeRouteMeta>;
  missingPaths: string[];
}

const isExternalPath = (path?: string) =>
  Boolean(path && /^(https?:)?\/\//.test(path));

const normalizePath = (path?: string) =>
  path ? path.replace(/[?#].*$/, '').replace(/\/+$/, '') || '/' : '';

const alwaysAccessibleRouteMeta = APP_ROUTE_DEFINITIONS.filter(
  (route) => route.alwaysAccessible,
).reduce<Record<string, RuntimeRouteMeta>>((acc, route) => {
  acc[route.path] = {
    path: route.path,
    title: route.title,
    permission: route.permission,
    breadcrumb: [route.title],
    activeMenu: route.activeMenu,
  };
  return acc;
}, {});

const buildMenuTree = (
  items: BackendMenuItem[],
  parentId = '',
  parentBreadcrumb: string[] = [],
  routeMetaMap: Record<string, RuntimeRouteMeta>,
  missingPaths: Set<string>,
): RuntimeMenuItem[] => {
  return items
    .filter(
      (item) => (item.parentId || '') === parentId && item.type === 'menu',
    )
    .map((item) => {
      const breadcrumb = [...parentBreadcrumb, item.name];
      const children = buildMenuTree(
        items,
        item.id,
        breadcrumb,
        routeMetaMap,
        missingPaths,
      );
      const href = normalizePath(item.href);
      const external = isExternalPath(item.href);
      const routeDefinition = href ? findRouteDefinition(href) : undefined;
      const resolvedIcon =
        (item.icon ? renderNamedIcon(item.icon) : null) ||
        (routeDefinition?.icon ? renderNamedIcon(routeDefinition.icon) : null) ||
        routeDefinition?.icon;

      if (href && !external) {
        if (routeDefinition) {
          routeMetaMap[href] = {
            path: href,
            title: item.name,
            alias: item.alias,
            permission: item.permission,
            tree: item.path,
            breadcrumb,
            activeMenu: routeDefinition.activeMenu,
          };
        } else {
          missingPaths.add(href);
        }
      }

      const menuItem: RuntimeMenuItem = {
        name: item.name,
        icon: resolvedIcon,
        hideInMenu: !item.showit || routeDefinition?.hideInMenu,
        children: children.length ? children : undefined,
        routes: children.length ? children : undefined,
      };

      if (external && item.href) {
        menuItem.path = item.href;
        menuItem.target = '_blank';
      } else if (routeDefinition) {
        menuItem.path = routeDefinition.path;
      }

      return menuItem;
    })
    .filter((item) => !item.hideInMenu && (item.path || item.routes?.length));
};

export const buildRuntimeMenuData = (
  authInfo?: BudiotAuthInfo,
  appId?: string,
): RuntimeMenuBuildResult => {
  if (!authInfo) {
    return {
      menuData: [],
      routeMetaMap: { ...alwaysAccessibleRouteMeta },
      missingPaths: [],
    };
  }

  const routeMetaMap: Record<string, RuntimeRouteMeta> = {
    ...alwaysAccessibleRouteMeta,
  };
  const missingPaths = new Set<string>();
  const appMenus = resolveAppMenus(authInfo, appId);
  const commonMenus = getMenuBucket(authInfo.menus, 'COMMON') || [];

  const menuData = [
    ...buildMenuTree(appMenus, '', [], routeMetaMap, missingPaths),
    ...buildMenuTree(commonMenus, '', [], routeMetaMap, missingPaths),
  ];

  return {
    menuData,
    routeMetaMap,
    missingPaths: Array.from(missingPaths),
  };
};

export const isAccessiblePlatformPath = (
  path: string,
  routeMetaMap: Record<string, RuntimeRouteMeta> = {},
  permissions: string[] = [],
): boolean => {
  const normalizedPath = normalizePath(path);
  const routeDefinition = findRouteDefinition(normalizedPath);

  if (!routeDefinition) {
    return true;
  }
  if (routeDefinition.alwaysAccessible) {
    return true;
  }
  if (routeMetaMap[normalizedPath]) {
    return true;
  }
  if (routeDefinition.permission) {
    return permissions.includes(routeDefinition.permission);
  }
  return false;
};

export const getRuntimeRouteMeta = (
  path: string,
  routeMetaMap: Record<string, RuntimeRouteMeta> = {},
) => {
  const normalizedPath = normalizePath(path);
  const direct =
    routeMetaMap[normalizedPath] ||
    (normalizedPath === HOME_PATH ? routeMetaMap[HOME_PATH] : undefined);
  if (direct) {
    return direct;
  }
  const routeDefinition = findRouteDefinition(normalizedPath);
  if (!routeDefinition) {
    return undefined;
  }
  const parentMeta = routeDefinition.activeMenu
    ? routeMetaMap[routeDefinition.activeMenu]
    : undefined;
  const breadcrumb = parentMeta
    ? [...parentMeta.breadcrumb, routeDefinition.title]
    : [routeDefinition.title];
  return {
    path: normalizedPath,
    title: routeDefinition.title,
    permission: routeDefinition.permission,
    breadcrumb,
    activeMenu: routeDefinition.activeMenu,
  };
};
