import { isAccessiblePlatformPath } from '@/utils/menu';

export default function access(
  initialState:
    | {
        currentUser?: API.CurrentUser;
        permissions?: string[];
        roles?: string[];
        routeMetaMap?: Record<
          string,
          {
            path: string;
            title: string;
            breadcrumb: string[];
          }
        >;
      }
    | undefined,
) {
  const {
    currentUser,
    permissions = [],
    roles = [],
    routeMetaMap = {},
  } = initialState ?? {};

  return {
    canAdmin: currentUser && currentUser.access === 'admin',
    hasRole: (role: string) => roles.includes(role),
    hasPermission: (permission: string) => permissions.includes(permission),
    canAccessPath: (path: string) =>
      isAccessiblePlatformPath(path, routeMetaMap),
  };
}
