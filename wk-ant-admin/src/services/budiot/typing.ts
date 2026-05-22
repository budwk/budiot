export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
}

export interface PlatformInfo {
  AppSessionOnlyOne: boolean;
  AppDemoEnv: boolean;
  AppWebSocket: boolean;
  AppUploadBase: string;
  AppUploadSize: string;
  AppFileDomain: string;
  AppDomain: string;
  AppName: string;
  AppShrotName: string;
  AppVersion: string;
  AppDefault: string;
}

export interface BudiotUserProfile {
  id: string;
  username: string;
  loginname: string;
  email: string;
  mobile: string;
  avatar: string;
  loginAt: number;
  loginIp: string;
  themeConfig: string;
}

export interface BackendMenuItem {
  id: string;
  parentId?: string;
  name: string;
  path: string;
  href?: string;
  permission?: string;
  icon?: string;
  type: string;
  showit?: boolean;
  alias?: string;
}

export type BudiotAuthAppItem = string | BudiotAppOption;

export interface BudiotAuthInfo {
  user: BudiotUserProfile;
  conf: Record<string, unknown>;
  apps: BudiotAuthAppItem[];
  menus: Record<string, BackendMenuItem[]>;
  token: string;
  roles: string[];
  permissions: string[];
}

export type LoginPayload =
  | {
      tenantName: string;
      loginname: string;
      password: string;
      captchaCode?: string;
      captchaKey?: string;
      rsaKey?: string;
      appId: string;
      type: 'password';
    }
  | {
      tenantName: string;
      mobile: string;
      smscode: string;
      appId: string;
      type: 'mobile';
    };

export interface LoginResult {
  token: string;
  tenantId?: string;
  tenantNotice?: string;
}

export interface CaptchaResult {
  key: string;
  code: string;
  captchaHasEnabled: boolean;
}

export interface RsaResult {
  rsaKey: string;
  rsaPublicKey: string;
}

export interface OauthProvider {
  text: string;
  value: string;
}

export interface BudiotAppOption {
  id: string;
  name: string;
  path?: string;
  icon?: string;
  hidden?: boolean;
  disabled?: boolean;
}

export interface SysMenuRecord {
  id: string;
  parentId?: string;
  name: string;
  alias?: string;
  appId?: string;
  href?: string;
  type: 'menu' | 'data' | string;
  permission?: string;
  icon?: string;
  path: string;
  showit: boolean;
  disabled: boolean;
  parentType?: string;
  parentName?: string;
  buttons?: SysMenuButtonRecord[];
  children?: SysMenuRecord[];
}

export interface SysMenuButtonRecord {
  id?: string;
  key?: string | number;
  name: string;
  permission: string;
}

export interface SysMenuDetail extends Omit<SysMenuRecord, 'children'> {
  children: 'true' | 'false' | string;
  createdAt?: number;
}

export interface SysUnitRecord {
  id: string;
  parentId?: string;
  path: string;
  name: string;
  leaderName?: string;
  disabled?: boolean;
  createdAt?: number;
  type?: {
    value?: string;
    text?: string;
  };
  children?: SysUnitRecord[];
}

export interface SysPostRecord {
  id: string;
  name: string;
}

export interface SysUserRecord {
  id: string;
  username: string;
  loginname: string;
  serialNo?: string;
  unit?: {
    id: string;
    name: string;
  };
  mobile?: string;
  disabled: boolean;
  createdAt?: number;
  loginAt?: number;
  loginIp?: string;
  postId?: string;
  email?: string;
  sex?: number;
}

export interface SysUserDetail {
  id?: string;
  unitId: string;
  unitPath?: string;
  postId?: string;
  roleIds: string[];
  username: string;
  loginname: string;
  password?: string;
  email?: string;
  mobile?: string;
  serialNo?: string;
  sex: number;
  disabled: boolean;
}

export interface HomeUserRole {
  id?: string;
  name: string;
}

export interface HomeUserInfo {
  id: string;
  avatar?: string;
  username: string;
  loginname: string;
  email?: string;
  mobile?: string;
  sex?: number;
  createdAt?: number;
  unit?: {
    id?: string;
    name?: string;
  };
  post?: {
    id?: string;
    name?: string;
  } | string;
  roles?: HomeUserRole[];
}

export interface PagedList<T> {
  list: T[];
  totalCount: number;
}

export interface HomeMsgNoticeItem {
  msgId?: string;
  msgid?: string;
  title: string;
  time?: string;
  url?: string;
}

export interface HomeMsgNoticePayload {
  action?: 'notice' | 'offline' | string;
  size?: number;
  notify?: boolean;
  list?: HomeMsgNoticeItem[];
}

export interface HomeMsgListRecord {
  id: string;
  msgid?: string;
  title: string;
  type?: string;
  status?: number;
  sendat?: number;
}

export interface HomeMsgDetail {
  id: string;
  title: string;
  url?: string;
  sendAt?: number;
  note?: string;
}

export interface HomeMsgInitData {
  types: SysMsgTypeOption[];
  scopes?: SysMsgScopeOption[];
}

export interface SysRoleRecord {
  id: string;
  groupId?: string;
  unitId?: string;
  name: string;
  code: string;
  note?: string;
  disabled: boolean;
}

export interface SysRoleGroupRecord {
  id: string;
  name: string;
  roles: SysRoleRecord[];
}

export interface SysPostListRecord {
  id: string;
  name: string;
  code: string;
  location: number;
}

export interface SysParamTypeOption {
  text: string;
  value: string;
}

export interface SysParamRecord {
  id: string;
  appId: string;
  configKey: string;
  configValue: string;
  type: SysParamTypeOption | string;
  note?: string;
  opened: boolean;
  createdAt?: number;
}

export interface SysUserOption {
  value: string;
  label: string;
}

export interface SysUnitUserRelation {
  userId: string;
  leaderType?: {
    value: 'LEADER' | 'HIGHER' | 'ASSIGNER' | string;
    text?: string;
  };
  user: {
    username: string;
  };
}

export interface SysUnitDetail extends SysUnitRecord {
  aliasName?: string;
  unitcode?: string;
  address?: string;
  telephone?: string;
  email?: string;
  website?: string;
  leaderName?: string;
  leaderMobile?: string;
  note?: string;
  disabled: boolean;
  leader?: string;
  higher?: string;
  assigner?: string;
}

export interface SysDictRecord {
  id: string;
  parentId?: string;
  path?: string;
  name: string;
  code: string;
  disabled: boolean;
  createdAt?: number;
  children?: SysDictRecord[];
}

export interface SysAreaRecord {
  id: string;
  parentId?: string;
  parentName?: string;
  path?: string;
  name: string;
  code: string;
  disabled: boolean;
  createdAt?: number;
  hasChildren?: boolean;
  children?: SysAreaRecord[];
}

export interface SysSecurityConfig {
  id?: string;
  hasEnabled: boolean;
  pwdLengthMin: number;
  pwdLengthMax: number;
  pwdCharMust: number;
  pwdCharNot?: string;
  pwdRepeatCheck: boolean;
  pwdRepeatNum: number;
  pwdRetryLock: boolean;
  pwdRetryNum: number;
  pwdRetryAction: number;
  pwdRetryTime: number;
  pwdTimeoutDay: number;
  pwdResetChange: boolean;
  nameRetryLock: boolean;
  nameRetryNum: number;
  nameTimeout: number;
  userSessionOnlyOne: boolean;
  captchaHasEnabled: boolean;
  captchaType: number;
}

export interface SysAppRecord {
  id: string;
  name: string;
  path?: string;
  icon?: string;
  hidden: boolean;
  disabled: boolean;
  location: number;
}

export interface SysLogTypeOption {
  text: string;
  value: string;
}

export interface SysLogRecord {
  id: string;
  type?: string;
  url?: string;
  tag?: string;
  msg?: string;
  exception?: string;
  loginname?: string;
  username?: string;
  createdAt?: number;
  ip?: string;
  executeTime?: number;
  method?: string;
  os?: string;
  browser?: string;
  params?: string;
  result?: string;
}

export interface SysKeyRecord {
  id?: string;
  name: string;
  appid: string;
  appkey: string;
  disabled: boolean;
}

export interface SysMsgTypeOption {
  text: string;
  value: string;
}

export interface SysMsgScopeOption {
  text: string;
  value: string;
}

export interface SysMsgRecord {
  id: string;
  title: string;
  url?: string;
  type: string;
  note?: string;
  all_num?: number;
  unread_num?: number;
  sendAt?: number;
  delFlag?: boolean;
  createdByUser?: {
    username?: string;
    loginname?: string;
  };
}

export interface MsgOption {
  text: string;
  value: string;
}

export interface MsgChannelRecord {
  id: string;
  name: string;
  code: string;
  channelType: string | MsgOption;
  providerType: string | MsgOption;
  configJson?: string;
  defaultFlag: boolean;
  disabled: boolean;
  updatedAt?: number;
  createdAt?: number;
}

export interface MsgTemplateRecord {
  id: string;
  channelId: string;
  channelType?: string | MsgOption;
  providerType?: string | MsgOption;
  bizType: string | MsgOption;
  name: string;
  templateCode?: string;
  content?: string;
  paramsJson?: string;
  disabled: boolean;
  updatedAt?: number;
  createdAt?: number;
  channel?: {
    id?: string;
    name?: string;
  };
  title?: string;
}

export interface MsgHistoryRecord {
  id: string;
  channelId?: string;
  templateId?: string;
  channelType?: string | MsgOption;
  providerType?: string | MsgOption;
  bizType?: string | MsgOption;
  receiver?: string;
  receiverName?: string;
  title?: string;
  content?: string;
  paramsJson?: string;
  status?: string | MsgOption;
  providerCode?: string;
  providerMsg?: string;
  providerRequestId?: string;
  sendAt?: number;
  successAt?: number;
  failAt?: number;
  createdAt?: number;
  channel?: {
    id?: string;
    name?: string;
  };
  channelName?: string;
}

export interface MsgSendResultRecord {
  success: boolean;
  receiver: string;
  requestId?: string;
  code?: string;
  message?: string;
  sendAt?: number;
}

export interface SysTenantPackageRecord {
  id: string;
  name: string;
  note?: string;
  disabled: boolean;
  createdAt?: number;
}

export interface SysTenantRecord {
  id: string;
  name: string;
  packageId?: string;
  adminLoginname?: string;
  adminPassword?: string;
  hasExpire: boolean;
  expireAt?: number | string | null;
  disabled: boolean;
  createdAt?: number;
  tenantPackage?: {
    id?: string;
    name?: string;
  };
}

export interface TenantMenuTreeNode {
  id: string;
  label?: string;
  name?: string;
  type?: string;
  children?: TenantMenuTreeNode[];
}

export interface ManagedFileRecord {
  id: string;
  fileName: string;
  category?: string;
  storageType?: string;
  publicFlag?: boolean;
  createdByLoginname?: string;
  createdByUsername?: string;
  contentType?: string;
  size?: number;
  createdAt?: number;
  imageFlag?: boolean;
  accessPath?: string;
}
