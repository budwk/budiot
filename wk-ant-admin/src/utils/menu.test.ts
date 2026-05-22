import {
  buildRuntimeMenuData,
  isAccessiblePlatformPath,
} from './menu';

describe('runtime menu utils', () => {
  it('builds menu data and route meta from backend menus', () => {
    const result = buildRuntimeMenuData(
      {
        user: {
          id: '1',
          username: 'tester',
          loginname: 'tester',
          email: '',
          mobile: '',
          avatar: '',
          loginAt: 0,
          loginIp: '',
          themeConfig: '',
        },
        conf: {},
        apps: ['PLATFORM'],
        menus: {
          PLATFORM: [
            {
              id: 'dashboard',
              name: '控制台',
              path: 'dashboard',
              href: '/platform/dashboard',
              type: 'menu',
              showit: true,
            },
            {
              id: 'sys',
              name: '系统管理',
              path: 'sys',
              type: 'menu',
              showit: true,
            },
            {
              id: 'sys-user',
              parentId: 'sys',
              name: '用户管理',
              path: 'user',
              href: '/platform/sys/user',
              permission: 'sys.user',
              type: 'menu',
              showit: true,
            },
            {
              id: 'external',
              name: '官网',
              path: 'budwk',
              href: 'https://budwk.com',
              type: 'menu',
              showit: true,
            },
          ],
          COMMON: [],
        },
        token: 'token',
        roles: ['admin'],
        permissions: ['sys.user'],
      },
      'PLATFORM',
    );

    expect(result.menuData).toEqual([
      expect.objectContaining({ path: '/platform/dashboard', name: '控制台' }),
      expect.objectContaining({
        name: '系统管理',
        routes: [
          expect.objectContaining({
            path: '/platform/sys/user',
            name: '用户管理',
          }),
        ],
      }),
      expect.objectContaining({
        path: 'https://budwk.com',
        target: '_blank',
      }),
    ]);
    expect(result.routeMetaMap['/platform/sys/user']).toEqual(
      expect.objectContaining({
        title: '用户管理',
        permission: 'sys.user',
        breadcrumb: ['系统管理', '用户管理'],
      }),
    );
  });

  it('marks unknown backend href as missing and blocks access', () => {
    const result = buildRuntimeMenuData(
      {
        user: {
          id: '1',
          username: 'tester',
          loginname: 'tester',
          email: '',
          mobile: '',
          avatar: '',
          loginAt: 0,
          loginIp: '',
          themeConfig: '',
        },
        conf: {},
        apps: ['PLATFORM'],
        menus: {
          PLATFORM: [
            {
              id: 'custom',
              name: '未注册页面',
              path: 'custom',
              href: '/platform/custom/page',
              type: 'menu',
              showit: true,
            },
          ],
          COMMON: [],
        },
        token: 'token',
        roles: [],
        permissions: [],
      },
      'PLATFORM',
    );

    expect(result.missingPaths).toContain('/platform/custom/page');
    expect(
      isAccessiblePlatformPath('/platform/sys/role', result.routeMetaMap),
    ).toBe(false);
    expect(
      isAccessiblePlatformPath('/platform/dashboard', result.routeMetaMap),
    ).toBe(true);
  });

  it('falls back to available app menu bucket when app id does not match exactly', () => {
    const result = buildRuntimeMenuData(
      {
        user: {
          id: '1',
          username: 'tester',
          loginname: 'tester',
          email: '',
          mobile: '',
          avatar: '',
          loginAt: 0,
          loginIp: '',
          themeConfig: '',
        },
        conf: {},
        apps: ['platform'],
        menus: {
          platform: [
            {
              id: 'sys-user',
              name: '用户管理',
              path: 'user',
              href: '/platform/sys/user',
              permission: 'sys.user',
              type: 'menu',
              showit: true,
            },
          ],
          COMMON: [],
        },
        token: 'token',
        roles: [],
        permissions: [],
      },
      'PLATFORM',
    );

    expect(result.menuData).toEqual([
      expect.objectContaining({
        path: '/platform/sys/user',
        name: '用户管理',
      }),
    ]);
  });
});
