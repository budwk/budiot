import { SettingDrawer } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { Link, history } from '@umijs/max';
import { ConfigProvider, Select, Space, Typography, theme as antdTheme } from 'antd';
import type { BreadcrumbProps } from 'antd';
import React from 'react';
import {
  AppSwitcher,
  AvatarDropdown,
  AvatarName,
  Footer,
  NoticeDropdown,
  SelectLang,
} from '@/components';
import { APP_ID, DEFAULT_LANG, LOGIN_PATH } from '@/constants/app';
import { getUserInfo, saveThemeConfig } from '@/services/budiot/auth';
import { getPlatformInfo } from '@/services/budiot/common';
import type { BudiotAuthInfo, PlatformInfo } from '@/services/budiot/typing';
import { getAvailableApps, resolveActiveAppId } from '@/utils/app';
import {
  buildRuntimeMenuData,
  getRuntimeRouteMeta,
  isAccessiblePlatformPath,
  type RuntimeMenuItem,
} from '@/utils/menu';
import {
  clearRuntimeSession,
  getStoredAppId,
  getStoredPlatformInfo,
  getStoredToken,
  setStoredAppId,
  setStoredAuthInfo,
  setStoredPlatformInfo,
} from '@/utils/session';
import { AntdAppHolder, appNotification } from '@/utils/antdApp';
import {
  dispatchThemeSettingsChange,
  THEME_SETTINGS_OPEN_EVENT,
  resolveLayoutSettings,
  resolveStoredLayoutSettings,
  type RuntimeLayoutThemeSettings,
  serializeLayoutSettings,
  THEME_SETTINGS_EVENT,
} from '@/utils/themeSettings';
import { toCurrentUser } from '@/utils/user';
import { errorConfig } from './requestErrorConfig';

const isDev = process.env.NODE_ENV === 'development';

const defaultPlatformInfo: PlatformInfo = {
  AppSessionOnlyOne: false,
  AppDemoEnv: false,
  AppWebSocket: false,
  AppUploadBase: '',
  AppUploadSize: '512000',
  AppFileDomain: '',
  AppDomain: '',
  AppName: 'BudIot Admin',
  AppShrotName: 'BudIot',
  AppVersion: '',
  AppDefault: '',
};

const resolveAvatarSrc = (avatar?: string, fileDomain?: string) => {
  if (!avatar) {
    return undefined;
  }
  if (/^(https?:)?\/\//.test(avatar)) {
    return avatar;
  }
  return `${fileDomain || ''}${avatar}`;
};

const normalizeBreadcrumbPath = (path?: string) =>
  path ? path.replace(/[?#].*$/, '').replace(/\/+$/, '') || '/' : undefined;

const findBreadcrumbTrail = (
  menuData: RuntimeMenuItem[] = [],
  names: string[] = [],
  trail: RuntimeMenuItem[] = [],
): RuntimeMenuItem[] | undefined => {
  if (!names.length) {
    return trail;
  }
  const [currentName, ...restNames] = names;
  for (const item of menuData) {
    if (item.name !== currentName) {
      continue;
    }
    const nextTrail = [...trail, item];
    if (!restNames.length) {
      return nextTrail;
    }
    const matched = findBreadcrumbTrail(
      item.children || item.routes || [],
      restNames,
      nextTrail,
    );
    if (matched) {
      return matched;
    }
  }
  return undefined;
};

const buildBreadcrumbItems = (
  pathname: string,
  routeMetaMap?: Record<
    string,
    {
      path: string;
      title: string;
      breadcrumb: string[];
    }
  >,
  menuData: RuntimeMenuItem[] = [],
): BreadcrumbProps['items'] => {
  const routeMeta = getRuntimeRouteMeta(pathname, routeMetaMap);
  const breadcrumbNames = routeMeta?.breadcrumb || [];
  if (!breadcrumbNames.length) {
    return [];
  }
  const trail = findBreadcrumbTrail(menuData, breadcrumbNames) || [];
  return breadcrumbNames.map((name, index) => {
    const matchedMenu = trail[index];
    const path =
      index < breadcrumbNames.length - 1
        ? normalizeBreadcrumbPath(matchedMenu?.path)
        : undefined;
    return {
      title: name,
      path,
    };
  });
};

const adaptMenuIconForTheme = (
  icon: React.ReactNode,
  isDarkTheme: boolean,
) => {
  if (
    !isDarkTheme ||
    !React.isValidElement(icon) ||
    typeof icon.type !== 'string' ||
    icon.type !== 'img'
  ) {
    return icon;
  }
  const imageIcon = icon as React.ReactElement<{ style?: React.CSSProperties }>;
  const currentStyle =
    typeof imageIcon.props.style === 'object' && imageIcon.props.style
      ? imageIcon.props.style
      : {};
  return React.cloneElement(imageIcon, {
    style: {
      ...currentStyle,
      filter: 'brightness(0) invert(1)',
    },
  });
};

type RuntimeInitialState = {
  settings?: RuntimeLayoutThemeSettings;
  token?: string;
  activeAppId?: string;
  currentUser?: API.CurrentUser;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
  fetchAuthInfo?: () => Promise<BudiotAuthInfo | undefined>;
  authInfo?: BudiotAuthInfo;
  platformInfo: PlatformInfo;
  permissions: string[];
  roles: string[];
  menuData: any[];
  routeMetaMap: Record<
    string,
    {
      path: string;
      title: string;
      breadcrumb: string[];
      alias?: string;
      permission?: string;
      tree?: string;
      activeMenu?: string;
    }
  >;
  missingMenuPaths: string[];
};

type ThemeSettingDrawerProps = {
  initialState?: RuntimeInitialState;
  setInitialState: (callback: (state: RuntimeInitialState | undefined) => RuntimeInitialState | undefined) => void;
};

const ThemeSettingDrawer: React.FC<ThemeSettingDrawerProps> = ({
  initialState,
  setInitialState,
}) => {
  const [open, setOpen] = React.useState(false);
  const saveTimerRef = React.useRef<number | undefined>(undefined);
  const lastSavedThemeRef = React.useRef(
    initialState?.currentUser?.themeConfig ||
      serializeLayoutSettings(initialState?.settings),
  );
  const availableApps = getAvailableApps(initialState?.authInfo);

  const applyThemeSettings = React.useCallback(
    (nextSettings: RuntimeLayoutThemeSettings) => {
      const themeConfig = serializeLayoutSettings(nextSettings);
      setInitialState((preInitialState) => {
        if (!preInitialState) {
          return preInitialState;
        }
        return {
          ...preInitialState,
          settings: nextSettings,
        };
      });
      dispatchThemeSettingsChange(nextSettings);

      if (themeConfig === lastSavedThemeRef.current) {
        return;
      }
      if (saveTimerRef.current) {
        window.clearTimeout(saveTimerRef.current);
      }
      saveTimerRef.current = window.setTimeout(() => {
        saveThemeConfig(themeConfig)
          .then(() => {
            lastSavedThemeRef.current = themeConfig;
            setInitialState((preInitialState) => {
              if (!preInitialState) {
                return preInitialState;
              }
              const nextAuthInfo = preInitialState.authInfo
                ? {
                    ...preInitialState.authInfo,
                    user: {
                      ...preInitialState.authInfo.user,
                      themeConfig,
                    },
                  }
                : preInitialState.authInfo;
              const nextState = {
                ...preInitialState,
                currentUser: preInitialState.currentUser
                  ? {
                      ...preInitialState.currentUser,
                      themeConfig,
                    }
                  : preInitialState.currentUser,
                authInfo: nextAuthInfo,
              };
              if (nextAuthInfo) {
                setStoredAuthInfo(nextAuthInfo);
              }
              return nextState;
            });
          })
          .catch((error) => {
            console.error('[wk-ant-admin] failed to save theme config', error);
          });
      }, 300);
    },
    [setInitialState],
  );

  React.useEffect(() => {
    lastSavedThemeRef.current =
      initialState?.authInfo?.user.themeConfig ||
      initialState?.currentUser?.themeConfig ||
      serializeLayoutSettings(initialState?.settings);
  }, [initialState?.authInfo?.user.themeConfig, initialState?.currentUser?.themeConfig]);

  React.useEffect(
    () => () => {
      if (saveTimerRef.current) {
        window.clearTimeout(saveTimerRef.current);
      }
    },
    [],
  );

  React.useEffect(() => {
    const handleOpenDrawer = () => setOpen(true);
    window.addEventListener(THEME_SETTINGS_OPEN_EVENT, handleOpenDrawer as EventListener);
    return () => window.removeEventListener(THEME_SETTINGS_OPEN_EVENT, handleOpenDrawer as EventListener);
  }, []);

  if (!initialState?.token) {
    return null;
  }

  return (
    <>
      <style>{`.ant-pro-setting-drawer-handle{display:none !important;}`}</style>
      <SettingDrawer
        disableUrlParams
        enableDarkTheme
        hideHintAlert
        hideCopyButton
        collapse={open}
        onCollapseChange={setOpen}
        settings={initialState.settings}
        drawerProps={{
          title: (
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <Typography.Title level={5} style={{ margin: 0 }}>
                主题设置
              </Typography.Title>
              <div>
                <div style={{ marginBottom: 8, fontSize: 12, color: 'rgba(0, 0, 0, 0.45)' }}>默认登陆</div>
                <Select
                  allowClear
                  placeholder="请选择默认登陆应用"
                  style={{ width: '100%' }}
                  value={initialState.settings?.defaultLoginAppId}
                  options={availableApps.map((item) => ({
                    label: item.name,
                    value: item.id,
                  }))}
                  onChange={(value) => {
                    applyThemeSettings({
                      ...(initialState.settings || {}),
                      defaultLoginAppId: value || undefined,
                    });
                  }}
                />
              </div>
            </Space>
          ),
        }}
        onSettingChange={(settings) => {
          applyThemeSettings({
            ...settings,
            defaultLoginAppId: initialState.settings?.defaultLoginAppId,
          });
        }}
      />
    </>
  );
};

const RuntimeThemeProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [settings, setSettings] = React.useState<RuntimeLayoutThemeSettings>(
    () => resolveStoredLayoutSettings(),
  );

  React.useEffect(() => {
    const handleThemeChange = (event: Event) => {
      const nextSettings = (event as CustomEvent<RuntimeLayoutThemeSettings>).detail;
      setSettings(nextSettings || resolveStoredLayoutSettings());
    };
    window.addEventListener(THEME_SETTINGS_EVENT, handleThemeChange as EventListener);
    return () =>
      window.removeEventListener(THEME_SETTINGS_EVENT, handleThemeChange as EventListener);
  }, []);

  const isDarkTheme = settings?.navTheme === 'realDark';
  return (
    <ConfigProvider
      theme={{
        algorithm: isDarkTheme
          ? antdTheme.darkAlgorithm
          : antdTheme.defaultAlgorithm,
        token: {
          colorPrimary: settings?.colorPrimary,
          fontFamily: 'AlibabaSans, sans-serif',
        },
      }}
    >
      {children}
    </ConfigProvider>
  );
};

const RuntimeStateSync: React.FC<{
  authInfo?: BudiotAuthInfo;
  settings?: RuntimeLayoutThemeSettings;
}> = ({ authInfo, settings }) => {
  React.useEffect(() => {
    if (authInfo) {
      setStoredAuthInfo(authInfo);
    }
  }, [authInfo]);

  React.useEffect(() => {
    if (settings) {
      dispatchThemeSettingsChange(settings);
    }
  }, [settings]);

  return null;
};

export const rootContainer = (container: React.ReactNode) => (
  <RuntimeThemeProvider>{container}</RuntimeThemeProvider>
);

const fetchPlatformRuntimeInfo = async (appId: string) => {
  try {
    const response = await getPlatformInfo(appId);
    setStoredPlatformInfo(response.data);
    return response.data;
  } catch {
    return getStoredPlatformInfo() || defaultPlatformInfo;
  }
};

const fetchAuthInfo = async () => {
  const response = await getUserInfo();
  setStoredAuthInfo(response.data);
  return response.data;
};

export async function getInitialState(): Promise<RuntimeInitialState> {
  const fetchUserInfo = async () => {
    const authInfo = await fetchAuthInfo();
    return authInfo ? toCurrentUser(authInfo) : undefined;
  };

  const storedAppId = getStoredAppId() || APP_ID;
  const token = getStoredToken();

  if (!token) {
    const platformInfo = await fetchPlatformRuntimeInfo(storedAppId);
    return {
      settings: resolveLayoutSettings(),
      activeAppId: storedAppId,
      platformInfo,
      permissions: [],
      roles: [],
      menuData: [],
      routeMetaMap: buildRuntimeMenuData().routeMetaMap,
      missingMenuPaths: [],
      fetchUserInfo,
      fetchAuthInfo,
    };
  }

  try {
    const authInfo = await fetchAuthInfo();
    const activeAppId = resolveActiveAppId(authInfo, storedAppId);
    if (activeAppId !== storedAppId) {
      setStoredAppId(activeAppId);
    }
    const runtimeMenu = buildRuntimeMenuData(authInfo, activeAppId);
    const platformInfo = await fetchPlatformRuntimeInfo(activeAppId);
    return {
      settings: resolveLayoutSettings(authInfo.user.themeConfig),
      token,
      activeAppId,
      authInfo,
      currentUser: toCurrentUser(authInfo),
      platformInfo,
      permissions: authInfo.permissions || [],
      roles: authInfo.roles || [],
      menuData: runtimeMenu.menuData,
      routeMetaMap: runtimeMenu.routeMetaMap,
      missingMenuPaths: runtimeMenu.missingPaths,
      fetchUserInfo,
      fetchAuthInfo,
    };
  } catch {
    clearRuntimeSession();
    history.push(LOGIN_PATH);
    const platformInfo = await fetchPlatformRuntimeInfo(APP_ID);
    return {
      settings: resolveLayoutSettings(),
      activeAppId: APP_ID,
      platformInfo,
      permissions: [],
      roles: [],
      menuData: [],
      routeMetaMap: buildRuntimeMenuData().routeMetaMap,
      missingMenuPaths: [],
      fetchUserInfo,
      fetchAuthInfo,
    };
  }
}

export const layout: RunTimeLayoutConfig = ({
  initialState,
  setInitialState,
}) => {
  const isDarkTheme = initialState?.settings?.navTheme === 'realDark';
  const renderMenuTitle = (item: { icon?: React.ReactNode; name?: string }) => {
    if (!item.icon) {
      return item.name;
    }
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 16,
          }}
        >
          {adaptMenuIconForTheme(item.icon, isDarkTheme)}
        </span>
        <span>{item.name}</span>
      </span>
    );
  };

  const renderCollapsedSubMenuTitle = (
    item: { icon?: React.ReactNode; name?: string },
    menuProps?: { baseClassName?: string; hashId?: string },
  ) => {
    const baseClassName = menuProps?.baseClassName || 'ant-pro-base-menu-inline';
    return (
      <div
        className={[
          `${baseClassName}-item-title`,
          `${baseClassName}-item-title-collapsed`,
          `${baseClassName}-item-title-collapsed-level-0`,
          menuProps?.hashId,
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <span
          className={[`${baseClassName}-item-icon`, menuProps?.hashId]
            .filter(Boolean)
            .join(' ')}
        >
          {adaptMenuIconForTheme(item.icon, isDarkTheme)}
        </span>
        <span
          className={[
            `${baseClassName}-item-text`,
            `${baseClassName}-item-text-has-icon`,
            menuProps?.hashId,
          ]
            .filter(Boolean)
            .join(' ')}
        >
          {item.name}
        </span>
      </div>
    );
  };

  return {
    actionsRender: () => [
      <AppSwitcher key="app-switcher" />,
      <NoticeDropdown key="notice" />,
      <SelectLang key="SelectLang" />,
    ],
    avatarProps: initialState?.currentUser
      ? {
          src: resolveAvatarSrc(
            initialState.currentUser.avatar,
            initialState.platformInfo?.AppFileDomain,
          ),
          title: <AvatarName />,
          render: (_, avatarChildren) => (
            <AvatarDropdown>{avatarChildren}</AvatarDropdown>
          ),
        }
      : undefined,
    footerRender: () => <Footer />,
    menu: {
      locale: false,
    },
    menuItemRender: (item, dom, menuProps) => {
      if (!item.path || item.children?.length) {
        return dom;
      }
      const content = renderMenuTitle(item);
      if (item.isUrl || item.target === '_blank') {
        return (
          <a href={item.path} target={item.target} rel="noreferrer">
            {content}
          </a>
        );
      }
      return <Link to={item.path}>{content}</Link>;
    },
    subMenuItemRender: (item, dom, menuProps) => {
      if (!menuProps?.collapsed || !item.icon) {
        return dom;
      }
      return renderCollapsedSubMenuTitle(item, {
        baseClassName: (menuProps as { baseClassName?: string } | undefined)?.baseClassName,
        hashId: (menuProps as { hashId?: string } | undefined)?.hashId,
      });
    },
    menuDataRender: () => initialState?.menuData || [],
    breadcrumbRender: () =>
      buildBreadcrumbItems(
        history.location.pathname,
        initialState?.routeMetaMap,
        initialState?.menuData || [],
      ),
    itemRender: (item, _, items) => {
      const isLast = item.title === items?.[items.length - 1]?.title;
      if (isLast || !item.path) {
        return <span>{item.title}</span>;
      }
      return <Link to={item.path}>{item.title}</Link>;
    },
    onPageChange: () => {
      const { location } = history;
      if (!initialState?.token && location.pathname !== LOGIN_PATH) {
        history.push(LOGIN_PATH);
        return;
      }
      if (
        initialState?.token &&
        location.pathname.startsWith('/platform/') &&
        location.pathname !== LOGIN_PATH &&
        !isAccessiblePlatformPath(
          location.pathname,
          initialState.routeMetaMap,
          initialState.permissions,
        )
      ) {
        appNotification.error({
          title: '无权访问当前页面',
          description: `没有访问 ${location.pathname} 的权限`,
        });
        history.replace('/403');
      }
    },
    bgLayoutImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    menuHeaderRender: undefined,
    pageTitleRender: (props) => {
      return props?.title
        ? `${props.title} - ${initialState?.platformInfo?.AppName || 'BudIot Admin'}`
        : initialState?.platformInfo?.AppName || 'BudIot Admin';
    },
    childrenRender: (children) => {
      if (
        isDev &&
        initialState?.missingMenuPaths?.length &&
        typeof window !== 'undefined'
      ) {
        console.warn(
          '[wk-ant-admin] backend menu href not registered in route manifest:',
          initialState.missingMenuPaths,
        );
      }
      return (
        <>
          <RuntimeStateSync
            authInfo={initialState?.authInfo}
            settings={initialState?.settings}
          />
          <AntdAppHolder />
          {children}
          <ThemeSettingDrawer
            initialState={initialState}
            setInitialState={setInitialState}
          />
        </>
      );
    },
    ...initialState?.settings,
  };
};

export const request: RequestConfig = {
  baseURL:
    typeof __API_BASE_URL__ !== 'undefined' ? __API_BASE_URL__ : '/api',
  timeout: 10000,
  requestInterceptors: [
    (config: Record<string, any>) => {
      const headers: Record<string, string> = {
        ...(config.headers || {}),
        lang: DEFAULT_LANG,
        appId: getStoredAppId() || APP_ID,
      };

      const token = getStoredToken();
      if (token) {
        headers.Authorization = token;
      }

      if (config.data instanceof FormData) {
        delete headers['Content-Type'];
      } else if (!headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
      }

      return {
        ...config,
        headers,
      };
    },
  ],
  ...errorConfig,
};
