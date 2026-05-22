import {
  AppstoreOutlined,
  DownOutlined,
} from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { App, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import { createStyles } from 'antd-style';
import React from 'react';
import { flushSync } from 'react-dom';
import { renderNamedIcon } from '@/components/IconPicker';
import { getUserInfo } from '@/services/budiot/auth';
import { getPlatformInfo } from '@/services/budiot/common';
import type { BudiotAuthInfo } from '@/services/budiot/typing';
import {
  getAppById,
  getAvailableApps,
  resolveActiveAppId,
} from '@/utils/app';
import { resolveRuntimeAppHomePath } from '@/utils/appRuntime';
import { buildRuntimeMenuData } from '@/utils/menu';
import {
  setStoredAppId,
  setStoredAuthInfo,
  setStoredPlatformInfo,
} from '@/utils/session';
import {
  dispatchThemeSettingsChange,
  resolveLayoutSettings,
} from '@/utils/themeSettings';
import { toCurrentUser } from '@/utils/user';

const useStyles = createStyles(({ token }) => ({
  action: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 6,
    height: 40,
    paddingInline: 12,
    color: token.colorTextSecondary,
    cursor: 'pointer',
    borderRadius: token.borderRadius,
    transition: 'all 0.2s ease',
    '&:hover': {
      backgroundColor: token.colorBgTextHover,
      color: token.colorPrimary,
    },
  },
  triggerText: {
    maxWidth: 108,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    [`@media screen and (max-width: ${token.screenSM}px)`]: {
      display: 'none',
    },
  },
  triggerIcon: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 16,
    height: 16,
    '.anticon, img': {
      width: 16,
      height: 16,
    },
  },
}));

const AppSwitcher: React.FC = () => {
  const { styles } = useStyles();
  const { message } = App.useApp();
  const { initialState, setInitialState } = useModel('@@initialState');
  const authInfo = initialState?.authInfo;
  const iconOnly = initialState?.settings?.layout === 'side';
  const apps = getAvailableApps(authInfo);
  const activeAppId = resolveActiveAppId(authInfo, initialState?.activeAppId);
  const currentApp = getAppById(authInfo, activeAppId);
  const [switching, setSwitching] = React.useState(false);

  const switchApp = React.useCallback(
    async (nextAppId: string) => {
      if (switching || !authInfo || nextAppId === activeAppId) {
        return;
      }
      const messageKey = 'wk-ant-admin-app-switch';
      setSwitching(true);
      message.loading({
        key: messageKey,
        content: '正在切换应用，请稍候...',
        duration: 0,
      });
      try {
        setStoredAppId(nextAppId);
        const [platformResponse, authResponse] = await Promise.all([
          getPlatformInfo(nextAppId),
          getUserInfo(),
        ]);
        const nextAuthInfo = authResponse.data as BudiotAuthInfo;
        const nextActiveAppId = resolveActiveAppId(nextAuthInfo, nextAppId);
        if (nextActiveAppId !== nextAppId) {
          setStoredAppId(nextActiveAppId);
        }
        const runtimeMenu = buildRuntimeMenuData(nextAuthInfo, nextActiveAppId);
        const nextPlatformInfo = platformResponse.data;
        const nextSettings = resolveLayoutSettings(nextAuthInfo.user.themeConfig);
        const nextPath = resolveRuntimeAppHomePath(
          nextAuthInfo,
          nextActiveAppId,
          history.location.pathname,
        );

        setStoredAuthInfo(nextAuthInfo);
        setStoredPlatformInfo(nextPlatformInfo);
        flushSync(() => {
          setInitialState((prev) =>
            prev
              ? {
                  ...prev,
                  activeAppId: nextActiveAppId,
                  authInfo: nextAuthInfo,
                  currentUser: toCurrentUser(nextAuthInfo),
                  settings: nextSettings,
                  platformInfo: nextPlatformInfo,
                  permissions: nextAuthInfo.permissions || [],
                  roles: nextAuthInfo.roles || [],
                  menuData: runtimeMenu.menuData,
                  routeMetaMap: runtimeMenu.routeMetaMap,
                  missingMenuPaths: runtimeMenu.missingPaths,
                }
              : prev,
          );
        });
        dispatchThemeSettingsChange(nextSettings);

        if (/^(https?:)?\/\//.test(nextPath)) {
          window.location.href = nextPath;
          return;
        }
        history.replace(nextPath);
        message.success({
          key: messageKey,
          content: '应用已切换',
          duration: 1,
        });
      } catch (error) {
        console.error('[wk-ant-admin] failed to switch app', error);
        message.error({
          key: 'wk-ant-admin-app-switch',
          content: '应用切换失败',
        });
      } finally {
        setSwitching(false);
      }
    },
    [activeAppId, authInfo, setInitialState, switching],
  );

  const menuItems: MenuProps['items'] = apps.map((item) => ({
    key: item.id,
    label: item.name,
    icon: renderNamedIcon(item.icon) || <AppstoreOutlined />,
    disabled: switching,
  }));

  if (apps.length <= 1 || !currentApp) {
    return null;
  }

  return (
    <Dropdown
      menu={{
        selectedKeys: [currentApp.id],
        items: menuItems,
        onClick: ({ key }) => {
          switchApp(String(key)).catch(() => undefined);
        },
      }}
      placement="bottomRight"
      trigger={['click']}
    >
      <span className={styles.action}>
        <span className={styles.triggerIcon}>
          {renderNamedIcon(currentApp.icon) || <AppstoreOutlined />}
        </span>
        {!iconOnly ? <span className={styles.triggerText}>{currentApp.name}</span> : null}
        {!iconOnly ? <DownOutlined style={{ fontSize: 10 }} /> : null}
      </span>
    </Dropdown>
  );
};

export default AppSwitcher;
