const runtimeShellComponent = './platform/runtime-shell';

export interface AppRouteDefinition {
  path: string;
  title: string;
  component: string;
  icon?: string;
  permission?: string;
  hideInMenu?: boolean;
  alwaysAccessible?: boolean;
  activeMenu?: string;
  parentKeys?: string[];
}

export const APP_ROUTE_DEFINITIONS: AppRouteDefinition[] = [
  {
    path: '/platform/dashboard',
    title: '控制台',
    component: './platform/dashboard',
    icon: 'dashboard',
    alwaysAccessible: true,
  },
  {
    path: '/platform/home/msg',
    title: '首页消息',
    component: './platform/home/msg',
    icon: 'bell',
    permission: 'home.msg',
    hideInMenu: true,
  },
  {
    path: '/platform/home/user',
    title: '个人信息',
    component: './platform/home/user',
    icon: 'user',
    permission: 'home.user',
    hideInMenu: true,
  },
  {
    path: '/platform/home/user/resetPwd',
    title: '修改密码',
    component: './platform/home/user/resetPwd',
    permission: 'home.user.resetPwd',
    hideInMenu: true,
    activeMenu: '/platform/home/user',
    parentKeys: ['/platform/home/user'],
  },
  {
    path: '/platform/home/user/userAvatar',
    title: '修改头像',
    component: './platform/home/user/userAvatar',
    permission: 'home.user.update',
    hideInMenu: true,
    activeMenu: '/platform/home/user',
    parentKeys: ['/platform/home/user'],
  },
  {
    path: '/platform/home/user/userLog',
    title: '登录日志',
    component: './platform/home/user/userLog',
    permission: 'home.user.update',
    hideInMenu: true,
    activeMenu: '/platform/home/user',
    parentKeys: ['/platform/home/user'],
  },
  {
    path: '/platform/sys/user',
    title: '用户管理',
    component: './platform/sys/user',
    icon: 'team',
  },
  {
    path: '/platform/sys/role',
    title: '角色管理',
    component: './platform/sys/role',
    icon: 'safetyCertificate',
  },
  {
    path: '/platform/sys/menu',
    title: '菜单管理',
    component: './platform/sys/menu',
    icon: 'menu',
  },
  {
    path: '/platform/sys/unit',
    title: '单位管理',
    component: './platform/sys/unit',
    icon: 'apartment',
  },
  {
    path: '/platform/sys/post',
    title: '岗位管理',
    component: './platform/sys/post',
    icon: 'idcard',
  },
  {
    path: '/platform/sys/param',
    title: '参数管理',
    component: './platform/sys/param',
    icon: 'setting',
  },
  {
    path: '/platform/sys/dict',
    title: '字典管理',
    component: './platform/sys/dict',
    icon: 'book',
  },
  {
    path: '/platform/sys/area',
    title: '区域管理',
    component: './platform/sys/area',
    icon: 'environment',
  },
  {
    path: '/platform/sys/security',
    title: '安全配置',
    component: './platform/sys/security',
    icon: 'safety',
  },
  {
    path: '/platform/sys/log',
    title: '系统日志',
    component: './platform/sys/log',
    icon: 'fileText',
  },
  {
    path: '/platform/sys/task',
    title: '定时任务',
    component: runtimeShellComponent,
    icon: 'schedule',
  },
  {
    path: '/platform/sys/tenant',
    title: '租户管理',
    component: './platform/sys/tenant',
    icon: 'cluster',
  },
  {
    path: '/platform/sys/tenant/package',
    title: '租户套餐',
    component: './platform/sys/tenant/package',
    hideInMenu: true,
    activeMenu: '/platform/sys/tenant',
    parentKeys: ['/platform/sys/tenant'],
  },
  {
    path: '/platform/sys/app',
    title: '应用管理',
    component: './platform/sys/app',
    icon: 'appstore',
  },
  {
    path: '/platform/sys/msg',
    title: '站内消息',
    component: './platform/sys/msg',
    icon: 'message',
  },
  {
    path: '/platform/sys/monitor',
    title: '系统监控',
    component: runtimeShellComponent,
    icon: 'monitor',
  },
  {
    path: '/platform/sys/key',
    title: '密钥管理',
    component: './platform/sys/key',
    icon: 'key',
  },
  {
    path: '/platform/sys/server',
    title: '服务管理',
    component: runtimeShellComponent,
    icon: 'cloudServer',
  },
  {
    path: '/platform/sys/test',
    title: '接口测试',
    component: runtimeShellComponent,
    icon: 'experiment',
  },
  {
    path: '/platform/msg/template',
    title: '消息模板',
    component: './platform/msg/template',
    icon: 'mail',
  },
  {
    path: '/platform/message/template',
    title: '消息模板',
    component: './platform/msg/template',
    icon: 'mail',
    hideInMenu: true,
    activeMenu: '/platform/msg/template',
    parentKeys: ['/platform/msg/template'],
  },
  {
    path: '/platform/msg/channel',
    title: '消息渠道',
    component: './platform/msg/channel',
    icon: 'api',
  },
  {
    path: '/platform/message/channel',
    title: '消息渠道',
    component: './platform/msg/channel',
    icon: 'api',
    hideInMenu: true,
    activeMenu: '/platform/msg/channel',
    parentKeys: ['/platform/msg/channel'],
  },
  {
    path: '/platform/msg/send',
    title: '消息发送',
    component: './platform/msg/send',
    icon: 'send',
  },
  {
    path: '/platform/message/send',
    title: '消息发送',
    component: './platform/msg/send',
    icon: 'send',
    hideInMenu: true,
    activeMenu: '/platform/msg/send',
    parentKeys: ['/platform/msg/send'],
  },
  {
    path: '/platform/msg/history',
    title: '发送历史',
    component: './platform/msg/history',
    icon: 'history',
  },
  {
    path: '/platform/message/history',
    title: '发送历史',
    component: './platform/msg/history',
    icon: 'history',
    hideInMenu: true,
    activeMenu: '/platform/msg/history',
    parentKeys: ['/platform/msg/history'],
  },
  {
    path: '/platform/file/list',
    title: '文件管理',
    component: './platform/file/list',
    icon: 'folderOpen',
  },
  {
    path: '/platform/video/monitor/dashboard',
    title: '监控大屏',
    component: './platform/video/monitor/dashboard',
    icon: 'dashboard',
  },
  {
    path: '/platform/video/monitor/inspection',
    title: '图片巡检',
    component: './platform/video/monitor/inspection',
    icon: 'picture',
  },
  {
    path: '/platform/video/media/camera',
    title: '摄像头列表',
    component: './platform/video/media/camera',
    icon: 'videoCamera',
  },
  {
    path: '/platform/video/media/task',
    title: '算法任务',
    component: './platform/video/media/task',
    icon: 'deploymentUnit',
  },
  {
    path: '/platform/video/model/dataset',
    title: '数据标注',
    component: './platform/video/model/dataset',
    icon: 'tags',
  },
  {
    path: '/platform/video/model/dataset/:id',
    title: '数据集详情',
    component: './platform/video/model/dataset/detail',
    permission: 'video.manage.model.dataset',
    hideInMenu: true,
    activeMenu: '/platform/video/model/dataset',
    parentKeys: ['/platform/video/model/dataset'],
  },
  {
    path: '/platform/video/model/train',
    title: '模型训练',
    component: './platform/video/model/train',
    icon: 'experiment',
  },
  {
    path: '/platform/video/model/train/:id',
    title: '模型详情',
    component: './platform/video/model/train/detail',
    permission: 'video.manage.model.train',
    hideInMenu: true,
    activeMenu: '/platform/video/model/train',
    parentKeys: ['/platform/video/model/train'],
  },
  {
    path: '/platform/video/event/alarm',
    title: '告警事件',
    component: './platform/video/event/alarm',
    icon: 'warning',
  },
  {
    path: '/platform/video/event/workorder',
    title: '告警工单',
    component: './platform/video/event/workorder',
    icon: 'audit',
  },
  {
    path: '/platform/cms/site',
    title: '站点管理',
    component: runtimeShellComponent,
    icon: 'global',
  },
  {
    path: '/platform/cms/channel',
    title: '栏目管理',
    component: runtimeShellComponent,
    icon: 'bars',
  },
  {
    path: '/platform/cms/article',
    title: '文章管理',
    component: runtimeShellComponent,
    icon: 'read',
  },
  {
    path: '/platform/cms/links/class',
    title: '友情链接分类',
    component: runtimeShellComponent,
    icon: 'link',
  },
  {
    path: '/platform/cms/links/link',
    title: '友情链接',
    component: runtimeShellComponent,
    icon: 'link',
  },
  {
    path: '/platform/iot/dashboard',
    title: 'IoT 控制台',
    component: './platform/iot/dashboard',
    icon: 'dashboard',
  },
  {
    path: '/platform/iot/category',
    title: '设备分类',
    component: './platform/iot/category',
    icon: 'tags',
  },
  {
    path: '/platform/iot/vendor',
    title: '设备厂商',
    component: './platform/iot/vendor',
    icon: 'shop',
  },
  {
    path: '/platform/iot/protocol',
    title: '协议管理',
    component: './platform/iot/protocol',
    icon: 'deploymentUnit',
  },
  {
    path: '/platform/iot/product',
    title: '产品管理',
    component: './platform/iot/product',
    icon: 'appstore',
  },
  {
    path: '/platform/iot/device',
    title: '设备管理',
    component: './platform/iot/device',
    icon: 'tablet',
  },
  {
    path: '/platform/iot/device/:id',
    title: '设备详情',
    component: './platform/iot/device/detail',
    permission: 'iot.manage.device',
    hideInMenu: true,
    activeMenu: '/platform/iot/device',
    parentKeys: ['/platform/iot/device'],
  },
  {
    path: '/platform/iot/gateway',
    title: '网关管理',
    component: './platform/iot/gateway',
    icon: 'gateway',
  },
  {
    path: '/platform/iot/gateway-agent',
    title: '网关代理',
    component: './platform/iot/gateway-agent',
    icon: 'branches',
  },
  {
    path: '/platform/iot/gateway/agent',
    title: '网关代理',
    component: './platform/iot/gateway-agent',
    icon: 'branches',
    hideInMenu: true,
    activeMenu: '/platform/iot/gateway-agent',
    parentKeys: ['/platform/iot/gateway-agent'],
  },
  {
    path: '/platform/iot/rule',
    title: '规则引擎',
    component: './platform/iot/rule',
    icon: 'partition',
  },
];

export const APP_ROUTE_MAP = APP_ROUTE_DEFINITIONS.reduce<
  Record<string, AppRouteDefinition>
>((acc, route) => {
  acc[route.path] = route;
  return acc;
}, {});

export const PUBLIC_ROUTE_PATHS = new Set(['/platform/login', '/403', '/404']);

const matchRoutePath = (path: string, pattern: string) => {
  if (path === pattern) {
    return true;
  }
  const pathParts = path.split('/').filter(Boolean);
  const patternParts = pattern.split('/').filter(Boolean);
  if (pathParts.length !== patternParts.length) {
    return false;
  }
  return patternParts.every(
    (part, index) => part.startsWith(':') || part === pathParts[index],
  );
};

export const findRouteDefinition = (path: string) =>
  APP_ROUTE_MAP[path] ||
  APP_ROUTE_DEFINITIONS.find((route) => matchRoutePath(path, route.path));
