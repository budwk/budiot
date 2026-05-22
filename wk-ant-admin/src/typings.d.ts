declare module 'slash2';
declare module '*.css';
declare module '*.less';
declare module '*.scss';
declare module '*.sass';
declare module '*.svg';
declare module '*.png';
declare module '*.jpg';
declare module '*.jpeg';
declare module '*.gif';
declare module '*.bmp';
declare module '*.tiff';
declare module 'omit.js';
declare module 'numeral';
declare module 'mockjs';
declare module 'jsencrypt';

declare const __API_BASE_URL__: string;
declare const __API_PROXY_TARGET__: string;
declare const __APP_ID__: string;
declare const __HOME_PATH__: string;
declare const __DEFAULT_LANG__: string;

declare namespace API {
  type CurrentUser = {
    id?: string;
    userid?: string;
    name?: string;
    username?: string;
    loginname?: string;
    email?: string;
    mobile?: string;
    avatar?: string;
    access?: string;
    loginIp?: string;
    loginAt?: number;
    themeConfig?: string;
  };

  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  type RuleListItem = {
    key: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };
}
