import { LogoutOutlined, MailOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { Spin } from 'antd';
import { createStyles } from 'antd-style';
import React from 'react';
import { flushSync } from 'react-dom';
import { HOME_PATH, LOGIN_PATH } from '@/constants/app';
import { logout } from '@/services/budiot/auth';
import type { PlatformInfo } from '@/services/budiot/typing';
import { clearRuntimeSession } from '@/utils/session';
import { dispatchThemeSettingsOpen } from '@/utils/themeSettings';
import HeaderDropdown from '../HeaderDropdown';

export type GlobalHeaderRightProps = {
  menu?: boolean;
  children?: React.ReactNode;
};

export const AvatarName = () => {
  const { initialState } = useModel('@@initialState');
  const { currentUser } = initialState || {};
  const iconOnly = initialState?.settings?.layout === 'side';
  if (iconOnly) {
    return (
      <span
        className="anticon"
        title={currentUser?.name}
        aria-label={currentUser?.name}
      />
    );
  }
  return <span className="anticon">{currentUser?.name}</span>;
};

const useStyles = createStyles(({ token }) => {
  return {
    action: {
      display: 'flex',
      height: '48px',
      marginLeft: 'auto',
      overflow: 'hidden',
      alignItems: 'center',
      padding: '0 8px',
      cursor: 'pointer',
      borderRadius: token.borderRadius,
      '&:hover': {
        backgroundColor: token.colorBgTextHover,
      },
    },
  };
});

const emptyPlatformInfo: PlatformInfo = {
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

export const AvatarDropdown: React.FC<GlobalHeaderRightProps> = ({
  menu,
  children,
}) => {
  const loginOut = async () => {
    await logout().catch(() => undefined);
    clearRuntimeSession();
    const { search, pathname } = window.location;
    const searchParams = new URLSearchParams({
      redirect: pathname + search,
    });
    history.replace({
      pathname: LOGIN_PATH,
      search: searchParams.toString(),
    });
  };

  const { styles } = useStyles();
  const { initialState, setInitialState } = useModel('@@initialState');

  const onMenuClick: MenuProps['onClick'] = (event) => {
    if (event.key === 'logout') {
      flushSync(() => {
        setInitialState((s) => ({
          ...s,
          platformInfo: s?.platformInfo || emptyPlatformInfo,
          token: undefined,
          authInfo: undefined,
          currentUser: undefined,
          permissions: [],
          roles: [],
          menuData: [],
          routeMetaMap: s?.routeMetaMap || {},
          missingMenuPaths: [],
        }));
      });
      loginOut();
      return;
    }

    if (event.key === 'profile') {
      history.push('/platform/home/user');
      return;
    }

    if (event.key === 'messages') {
      history.push('/platform/home/msg');
      return;
    }

    if (event.key === 'theme-settings') {
      dispatchThemeSettingsOpen();
      return;
    }

    history.push(HOME_PATH);
  };

  const loading = (
    <span className={styles.action}>
      <Spin
        size="small"
        style={{
          marginLeft: 8,
          marginRight: 8,
        }}
      />
    </span>
  );

  if (!initialState) {
    return loading;
  }

  const { currentUser } = initialState;
  if (!currentUser?.name) {
    return loading;
  }

  const menuItems = [
    ...(menu
      ? [
          {
            key: 'center',
            icon: <UserOutlined />,
            label: '控制台',
          },
          {
            type: 'divider' as const,
          },
        ]
      : []),
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人资料',
    },
    {
      key: 'messages',
      icon: <MailOutlined />,
      label: '消息中心',
    },
    {
      key: 'theme-settings',
      icon: <SettingOutlined />,
      label: '主题设置',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
    },
  ];

  return (
    <HeaderDropdown
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: menuItems,
      }}
    >
      {children}
    </HeaderDropdown>
  );
};
