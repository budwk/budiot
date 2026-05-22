import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import defaultSettings from '../../config/defaultSettings';
import { getStoredAuthInfo } from '@/utils/session';

export type RuntimeLayoutThemeSettings = Partial<LayoutSettings> & {
  siderWidth?: number;
  siderMenuType?: 'sub' | 'group';
  colorPrimary?: string;
  defaultLoginAppId?: string;
};

export const THEME_SETTINGS_EVENT = 'wk-ant-admin-theme-change';
export const THEME_SETTINGS_OPEN_EVENT = 'wk-ant-admin-theme-open';

type PersistedAntThemeSettings = Pick<
  RuntimeLayoutThemeSettings,
  | 'navTheme'
  | 'layout'
  | 'contentWidth'
  | 'fixedHeader'
  | 'fixSiderbar'
  | 'colorWeak'
  | 'splitMenus'
  | 'siderMenuType'
  | 'siderWidth'
  | 'colorPrimary'
  | 'defaultLoginAppId'
>;

type LegacyVueThemeSettings = {
  themeColor?: string;
  sideTheme?: string;
  sideWidth?: string | number;
  topNav?: boolean;
  fixedHeader?: boolean;
};

const normalizeSiderWidth = (value?: string | number) => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number.parseInt(value, 10);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return undefined;
};

const isPersistedAntThemeSettings = (
  value: Record<string, unknown>,
): value is PersistedAntThemeSettings =>
  [
    'navTheme',
    'layout',
    'contentWidth',
    'fixedHeader',
    'fixSiderbar',
    'colorWeak',
    'splitMenus',
    'siderMenuType',
    'siderWidth',
    'colorPrimary',
  ].some((key) => key in value);

const isLegacyVueThemeSettings = (
  value: Record<string, unknown>,
): value is LegacyVueThemeSettings =>
  ['themeColor', 'sideTheme', 'sideWidth', 'topNav', 'fixedHeader'].some(
    (key) => key in value,
  );

const parseThemeConfig = (themeConfig?: string): PersistedAntThemeSettings => {
  if (!themeConfig) {
    return {};
  }
  try {
    const parsed = JSON.parse(themeConfig) as Record<string, unknown>;
    if (isPersistedAntThemeSettings(parsed)) {
      return {
        navTheme:
          parsed.navTheme === 'light' || parsed.navTheme === 'realDark'
            ? parsed.navTheme
            : undefined,
        layout:
          parsed.layout === 'side' || parsed.layout === 'top' || parsed.layout === 'mix'
            ? parsed.layout
            : undefined,
        contentWidth:
          parsed.contentWidth === 'Fluid' || parsed.contentWidth === 'Fixed'
            ? parsed.contentWidth
            : undefined,
        fixedHeader:
          typeof parsed.fixedHeader === 'boolean' ? parsed.fixedHeader : undefined,
        fixSiderbar:
          typeof parsed.fixSiderbar === 'boolean' ? parsed.fixSiderbar : undefined,
        colorWeak: typeof parsed.colorWeak === 'boolean' ? parsed.colorWeak : undefined,
        splitMenus: typeof parsed.splitMenus === 'boolean' ? parsed.splitMenus : undefined,
        siderMenuType:
          parsed.siderMenuType === 'sub' || parsed.siderMenuType === 'group'
            ? parsed.siderMenuType
            : undefined,
        siderWidth: normalizeSiderWidth(parsed.siderWidth as string | number | undefined),
        colorPrimary:
          typeof parsed.colorPrimary === 'string' ? parsed.colorPrimary : undefined,
        defaultLoginAppId:
          typeof parsed.defaultLoginAppId === 'string' ? parsed.defaultLoginAppId : undefined,
      };
    }
    if (isLegacyVueThemeSettings(parsed)) {
      return {
        colorPrimary:
          typeof parsed.themeColor === 'string' ? parsed.themeColor : undefined,
        navTheme: parsed.sideTheme === 'theme-dark' ? 'realDark' : 'light',
        layout: parsed.topNav ? 'top' : defaultSettings.layout,
        fixedHeader:
          typeof parsed.fixedHeader === 'boolean' ? parsed.fixedHeader : undefined,
        siderWidth: normalizeSiderWidth(parsed.sideWidth),
      };
    }
  } catch (error) {
    console.warn('[wk-ant-admin] failed to parse themeConfig', error);
  }
  return {};
};

export const resolveLayoutSettings = (
  themeConfig?: string,
): RuntimeLayoutThemeSettings => ({
  ...(defaultSettings as RuntimeLayoutThemeSettings),
  ...parseThemeConfig(themeConfig),
});

export const resolveStoredLayoutSettings = () =>
  resolveLayoutSettings(getStoredAuthInfo()?.user?.themeConfig);

export const serializeLayoutSettings = (
  settings?: RuntimeLayoutThemeSettings,
) =>
  JSON.stringify({
    navTheme: settings?.navTheme,
    colorPrimary: settings?.colorPrimary,
    layout: settings?.layout,
    contentWidth: settings?.contentWidth,
    fixedHeader: settings?.fixedHeader,
    fixSiderbar: settings?.fixSiderbar,
    colorWeak: settings?.colorWeak,
    splitMenus: settings?.splitMenus,
    siderMenuType: settings?.siderMenuType,
    siderWidth: settings?.siderWidth,
    defaultLoginAppId: settings?.defaultLoginAppId,
  } satisfies PersistedAntThemeSettings);

export const dispatchThemeSettingsChange = (
  settings: RuntimeLayoutThemeSettings,
) => {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(
    new CustomEvent<RuntimeLayoutThemeSettings>(THEME_SETTINGS_EVENT, {
      detail: settings,
    }),
  );
};

export const dispatchThemeSettingsOpen = () => {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(THEME_SETTINGS_OPEN_EVENT));
};

export const getDefaultLoginAppId = (themeConfig?: string) =>
  parseThemeConfig(themeConfig).defaultLoginAppId;
