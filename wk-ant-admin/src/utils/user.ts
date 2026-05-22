import type { BudiotAuthInfo } from '@/services/budiot/typing';

export const toCurrentUser = (authInfo: BudiotAuthInfo): API.CurrentUser => ({
  id: authInfo.user.id,
  userid: authInfo.user.id,
  name: authInfo.user.username || authInfo.user.loginname,
  username: authInfo.user.username,
  loginname: authInfo.user.loginname,
  email: authInfo.user.email,
  mobile: authInfo.user.mobile,
  avatar: authInfo.user.avatar,
  access: authInfo.roles.includes('superadmin') ? 'admin' : 'user',
  loginIp: authInfo.user.loginIp,
  loginAt: authInfo.user.loginAt,
  themeConfig: authInfo.user.themeConfig,
});
