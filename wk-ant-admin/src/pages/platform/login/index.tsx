import {
  AlipayCircleOutlined,
  LockOutlined,
  MessageOutlined,
  MobileOutlined,
  QqOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  WechatOutlined,
} from '@ant-design/icons';
import { history, useLocation, useModel } from '@umijs/max';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Form,
  Image,
  Input,
  Modal,
  Radio,
  Row,
  Space,
  Steps,
  Tabs,
  Typography,
} from 'antd';
import JSEncrypt from 'jsencrypt';
import * as React from 'react';
import {
  APP_ID,
  DEFAULT_TENANT_NAME,
  HOME_PATH,
  LOGIN_PATH,
} from '@/constants/app';
import {
  checkLoginname,
  doLogin,
  getCaptcha,
  getRsa,
  getSmsCode,
  saveNewPwd,
  sendResetPwdCode,
} from '@/services/budiot/auth';
import {
  bindOauthLogin,
  exchangeOauthLoginTicket,
  getOauthAuthorizeUrl,
  getOauthProviders,
} from '@/services/budiot/oauth';
import type {
  CaptchaResult,
  BudiotAuthInfo,
  LoginResult,
  OauthProvider,
  RsaResult,
} from '@/services/budiot/typing';
import { resolveActiveAppId } from '@/utils/app';
import { resolveRuntimeAppHomePath } from '@/utils/appRuntime';
import {
  setStoredAppId,
  setStoredAuthInfo,
  setStoredToken,
} from '@/utils/session';
import { getDefaultLoginAppId } from '@/utils/themeSettings';

type LoginTabKey = 'password' | 'sms';

type PasswordLoginValues = {
  tenantName: string;
  loginname: string;
  password: string;
  captchaCode?: string;
};

type SmsLoginValues = {
  tenantName: string;
  mobile: string;
  smscode: string;
};

type ResetPasswordValues = {
  loginname: string;
  type?: string;
  password?: string;
  code?: string;
};

type OauthState = {
  bindMode: boolean;
  provider: string;
  providerText: string;
  ticket: string;
  redirect: string;
};

const oauthIconMap: Record<string, React.ReactNode> = {
  wechat: <WechatOutlined style={{ color: '#07c160', fontSize: 18 }} />,
  qq: <QqOutlined style={{ color: '#1677ff', fontSize: 18 }} />,
  alipay: <AlipayCircleOutlined style={{ color: '#1677ff', fontSize: 18 }} />,
};

const getSafeRedirectUrl = (redirect: string | null): string => {
  if (!redirect?.startsWith('/')) return HOME_PATH;
  if (redirect.startsWith('//')) return HOME_PATH;

  try {
    const parsed = new URL(redirect, window.location.origin);
    if (parsed.origin !== window.location.origin) return HOME_PATH;
    return `${parsed.pathname}${parsed.search}${parsed.hash}`;
  } catch {
    return HOME_PATH;
  }
};

const resolvePostLoginRedirect = (redirect: string | null, defaultHomePath: string) => {
  const safeRedirect = getSafeRedirectUrl(redirect);
  if (!redirect || safeRedirect === HOME_PATH || safeRedirect === LOGIN_PATH) {
    return defaultHomePath;
  }
  return safeRedirect;
};

const getProviderLabel = (provider: OauthProvider) =>
  oauthIconMap[provider.value] ? (
    <Space size={8}>
      {oauthIconMap[provider.value]}
      <span>{provider.text}</span>
    </Space>
  ) : (
    provider.text
  );

const LoginPage = () => {
  const location = useLocation();
  const { initialState, setInitialState } = useModel('@@initialState');
  const { message, notification } = App.useApp();
  const [passwordForm] = Form.useForm<PasswordLoginValues>();
  const [smsForm] = Form.useForm<SmsLoginValues>();
  const [resetForm] = Form.useForm<ResetPasswordValues>();
  const [activeTab, setActiveTab] = React.useState<LoginTabKey>('password');
  const [submitting, setSubmitting] = React.useState(false);
  const [rsaResult, setRsaResult] = React.useState<RsaResult>();
  const [captchaResult, setCaptchaResult] = React.useState<CaptchaResult>();
  const [errorMessage, setErrorMessage] = React.useState<string>();
  const [oauthProviders, setOauthProviders] = React.useState<OauthProvider[]>([]);
  const [oauthState, setOauthState] = React.useState<OauthState>({
    bindMode: false,
    provider: '',
    providerText: '',
    ticket: '',
    redirect: '',
  });
  const [smsSending, setSmsSending] = React.useState(false);
  const [smsCountdown, setSmsCountdown] = React.useState(0);
  const [resetVisible, setResetVisible] = React.useState(false);
  const [resetStep, setResetStep] = React.useState(0);
  const [resetSubmitting, setResetSubmitting] = React.useState(false);
  const [resetHint, setResetHint] = React.useState('');
  const [resetTypeOptions, setResetTypeOptions] = React.useState<
    Array<{ label: string; value: string }>
  >([]);

  const title = initialState?.platformInfo?.AppName || 'BudIot Admin';
  const version = initialState?.platformInfo?.AppVersion || '';
  const initialTenantName = DEFAULT_TENANT_NAME;
  const isDemoEnv = initialState?.platformInfo?.AppDemoEnv;

  const refreshCaptcha = React.useCallback(async () => {
    const response = await getCaptcha();
    setCaptchaResult(response.data);
  }, []);

  const applyLoginState = React.useCallback(
    async (loginResult: LoginResult, successMsg: string) => {
      setStoredToken(loginResult.token);
      setStoredAppId(APP_ID);
      const authInfo = await initialState?.fetchAuthInfo?.();
      if (!authInfo) {
        setErrorMessage('登录成功，但用户初始化失败。');
        return false;
      }

      const nextAuthInfo = authInfo as BudiotAuthInfo;
      const defaultLoginAppId = getDefaultLoginAppId(nextAuthInfo.user.themeConfig);
      const activeAppId = resolveActiveAppId(nextAuthInfo, defaultLoginAppId || APP_ID);
      setStoredAppId(activeAppId);
      setStoredAuthInfo(nextAuthInfo);

      message.success(successMsg);
      if (loginResult.tenantNotice) {
        notification.warning({
          title: '租户到期提醒',
          description: loginResult.tenantNotice,
        });
      }

      const search = new URLSearchParams(location.search);
      const defaultHomePath = resolveRuntimeAppHomePath(nextAuthInfo, activeAppId, HOME_PATH);
      const redirect = oauthState.redirect || search.get('redirect');
      window.location.replace(resolvePostLoginRedirect(redirect, defaultHomePath));
      return true;
    },
    [
      initialState?.fetchAuthInfo,
      location.search,
      message,
      notification,
      oauthState.redirect,
    ],
  );

  React.useEffect(() => {
    refreshCaptcha().catch(() => undefined);
    getRsa().then((response) => setRsaResult(response.data)).catch(() => undefined);
    getOauthProviders()
      .then((response) => setOauthProviders(response.data || []))
      .catch(() => undefined);
  }, [refreshCaptcha]);

  React.useEffect(() => {
    if (!smsCountdown) return undefined;
    const timer = window.setTimeout(() => {
      setSmsCountdown((prev) => prev - 1);
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [smsCountdown]);

  React.useEffect(() => {
    if (initialState?.token) {
      history.replace(
        resolveRuntimeAppHomePath(
          initialState?.authInfo,
          initialState?.activeAppId,
          HOME_PATH,
        ),
      );
    }
  }, [initialState?.activeAppId, initialState?.authInfo, initialState?.token]);

  React.useEffect(() => {
    const search = new URLSearchParams(location.search);
    const oauthMode = search.get('oauthMode') || '';
    const oauthMsg = search.get('oauthMsg') || '';
    const ticket = search.get('ticket') || '';
    const provider = search.get('provider') || '';
    const providerText = search.get('providerText') || provider;
    const redirect = search.get('redirect') || '';

    if (oauthMsg) {
      notification.error({ title: oauthMsg });
    }

    if (oauthMode === 'login' && ticket) {
      exchangeOauthLoginTicket(ticket)
        .then((response) => applyLoginState(response.data, '登录成功！'))
        .catch(() => {
          history.replace('/platform/login');
        });
      return;
    }

    if (oauthMode === 'bind' && ticket) {
      setOauthState({
        bindMode: true,
        ticket,
        provider,
        providerText,
        redirect,
      });
      setActiveTab('password');
    }
  }, [applyLoginState, location.search, notification]);

  const clearOauthQuery = () => {
    const search = new URLSearchParams(location.search);
    const redirect = search.get('redirect');
    setOauthState({
      bindMode: false,
      provider: '',
      providerText: '',
      ticket: '',
      redirect: '',
    });
    history.replace(
      redirect ? `/platform/login?redirect=${encodeURIComponent(redirect)}` : '/platform/login',
    );
  };

  const handlePasswordLogin = async () => {
    if (!rsaResult) {
      setErrorMessage('RSA 密钥尚未初始化，请刷新页面后重试。');
      return;
    }

    const values = await passwordForm.validateFields();
    setSubmitting(true);
    try {
      setErrorMessage(undefined);
      const encrypt = new JSEncrypt();
      encrypt.setPublicKey(rsaResult.rsaPublicKey);
      const encryptedPassword = encrypt.encrypt(values.password);

      if (!encryptedPassword) {
        setErrorMessage('密码加密失败，请重试。');
        return;
      }

      const requestApi = oauthState.bindMode
        ? bindOauthLogin({
            tenantName: values.tenantName,
            loginname: values.loginname,
            password: encryptedPassword,
            captchaCode: values.captchaCode || '',
            captchaKey: captchaResult?.key,
            rsaKey: rsaResult.rsaKey,
            appId: APP_ID,
            type: 'password',
            ticket: oauthState.ticket,
            provider: oauthState.provider,
          })
        : doLogin({
            tenantName: values.tenantName,
            loginname: values.loginname,
            password: encryptedPassword,
            captchaCode: values.captchaCode || '',
            captchaKey: captchaResult?.key,
            rsaKey: rsaResult.rsaKey,
            appId: APP_ID,
            type: 'password',
          });

      const response = await requestApi;
      await applyLoginState(
        response.data,
        oauthState.bindMode ? '账号绑定成功！' : '登录成功！',
      );
    } catch (error) {
      setErrorMessage(
        (error as { msg?: string })?.msg || '登录失败，请检查输入后重试。',
      );
      passwordForm.setFieldValue('captchaCode', '');
      await refreshCaptcha().catch(() => undefined);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSmsSend = async () => {
    const values = await smsForm.validateFields(['tenantName', 'mobile']);
    setSmsSending(true);
    try {
      await getSmsCode(values.mobile, values.tenantName);
      notification.success({ title: '短信发送成功，请注意查收' });
      setSmsCountdown(60);
      smsForm.setFieldValue('smscode', '');
    } finally {
      setSmsSending(false);
    }
  };

  const handleSmsLogin = async () => {
    const values = await smsForm.validateFields();
    setSubmitting(true);
    try {
      setErrorMessage(undefined);
      const response = await doLogin({
        tenantName: values.tenantName,
        mobile: values.mobile,
        smscode: values.smscode,
        appId: APP_ID,
        type: 'mobile',
      });
      await applyLoginState(response.data, '登录成功！');
    } catch (error) {
      setErrorMessage(
        (error as { msg?: string })?.msg || '短信登录失败，请检查输入后重试。',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const resetModal = () => {
    resetForm.resetFields();
    setResetVisible(false);
    setResetStep(0);
    setResetHint('');
    setResetTypeOptions([]);
  };

  const handleResetPassword = async () => {
    const values = await resetForm.validateFields();
    setResetSubmitting(true);
    try {
      if (resetStep === 0) {
        const response = await checkLoginname(values.loginname);
        const options = Object.entries(response.data || {}).map(([key, value]) => ({
          value: key,
          label: value,
        }));
        setResetTypeOptions(options);
        setResetStep(1);
        if (options[0]?.value) {
          resetForm.setFieldValue('type', options[0].value);
        }
        return;
      }

      if (resetStep === 1) {
        const response = await sendResetPwdCode({
          loginname: values.loginname,
          type: values.type,
        });
        setResetHint(response.msg);
        setResetStep(2);
        return;
      }

      await saveNewPwd({
        loginname: values.loginname,
        type: values.type,
        password: values.password,
        code: values.code,
      });
      notification.success({
        title: '密码重置成功，请重新登录',
      });
      resetModal();
    } finally {
      setResetSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
        background:
          'radial-gradient(circle at top left, rgba(22,119,255,0.18), transparent 32%), linear-gradient(135deg, #eef4ff 0%, #f8fbff 40%, #f5f7fa 100%)',
      }}
    >
      <Card
        styles={{ body: { padding: 0 } }}
        style={{
          width: 'min(1040px, 100%)',
          overflow: 'hidden',
          borderRadius: 24,
          boxShadow: '0 20px 60px rgba(15, 23, 42, 0.12)',
        }}
      >
        <Row>
          <Col
            xs={0}
            md={11}
            style={{
              padding: 40,
              color: '#fff',
              background:
                'linear-gradient(160deg, #1677ff 0%, #0958d9 45%, #003eb3 100%)',
              minHeight: 620,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <Typography.Title level={2} style={{ color: '#fff', marginBottom: 8 }}>
                {title}
              </Typography.Title>
              <Typography.Paragraph style={{ color: 'rgba(255,255,255,0.88)' }}>
                {version || '统一认证中心'}
              </Typography.Paragraph>
              <Typography.Paragraph style={{ color: 'rgba(255,255,255,0.88)' }}>
                基于 SpringBoot 4 + SpringCloud + Nacos 3 + Dubbo 3 微服务分布式开发框架
              </Typography.Paragraph>
            </div>

            <div
              style={{
                borderRadius: 20,
                padding: 24,
                background: 'rgba(255,255,255,0.12)',
                backdropFilter: 'blur(12px)',
              }}
            >
              <Space orientation="vertical" size={16} style={{ display: 'flex' }}>
                <Space>
                  <SafetyCertificateOutlined style={{ fontSize: 18 }} />
                  <span>RSA 密码加密 + 图形验证码校验</span>
                </Space>
                <Space>
                  <MobileOutlined style={{ fontSize: 18 }} />
                  <span>短信验证码登录与密码找回</span>
                </Space>
                <Space>
                  <UserOutlined style={{ fontSize: 18 }} />
                  <span>第三方 OAuth 登录与账号绑定</span>
                </Space>
              </Space>
            </div>
          </Col>

          <Col xs={24} md={13} style={{ padding: 40 }}>
            <div style={{ maxWidth: 420, margin: '0 auto' }}>
              <Typography.Title level={3} style={{ marginBottom: 8 }}>
                欢迎登录
              </Typography.Title>

              {oauthState.bindMode ? (
                <Alert
                  style={{ marginBottom: 16 }}
                  type="success"
                  showIcon
                  title={`${oauthState.providerText} 已授权，请登录并绑定现有账号`}
                />
              ) : null}

              {errorMessage ? (
                <Alert
                  style={{ marginBottom: 16 }}
                  title={errorMessage}
                  type="error"
                  showIcon
                />
              ) : null}

              <Tabs
                activeKey={activeTab}
                onChange={(key) => setActiveTab(key as LoginTabKey)}
                items={[
                  {
                    key: 'password',
                    label: '用户登录',
                    children: (
                      <Form
                        form={passwordForm}
                        layout="vertical"
                        initialValues={{
                          tenantName: initialTenantName,
                          loginname: isDemoEnv ? 'superadmin' : '',
                          password: isDemoEnv ? '1' : '',
                          captchaCode: '',
                        }}
                        onFinish={handlePasswordLogin}
                      >
                        <Form.Item
                          name="tenantName"
                          rules={[{ required: true, message: '请输入租户名称' }]}
                        >
                          <Input size="large" placeholder="请输入租户名称" />
                        </Form.Item>
                        <Form.Item
                          name="loginname"
                          rules={[{ required: true, message: '请输入账号' }]}
                        >
                          <Input
                            size="large"
                            prefix={<UserOutlined />}
                            placeholder="请输入账号"
                          />
                        </Form.Item>
                        <Form.Item
                          name="password"
                          rules={[{ required: true, message: '请输入密码' }]}
                        >
                          <Input.Password
                            size="large"
                            prefix={<LockOutlined />}
                            placeholder="请输入密码"
                          />
                        </Form.Item>
                        {captchaResult?.captchaHasEnabled ? (
                          <Row gutter={12}>
                            <Col span={14}>
                              <Form.Item
                                name="captchaCode"
                                rules={[{ required: true, message: '请输入验证码' }]}
                              >
                                <Input
                                  size="large"
                                  prefix={<SafetyCertificateOutlined />}
                                  placeholder="请输入验证码"
                                />
                              </Form.Item>
                            </Col>
                            <Col span={10}>
                              {captchaResult.code ? (
                                <Image
                                  src={captchaResult.code}
                                  preview={false}
                                  alt="captcha"
                                  onClick={() => refreshCaptcha().catch(() => undefined)}
                                  style={{
                                    width: '100%',
                                    height: 40,
                                    borderRadius: 8,
                                    border: '1px solid #f0f0f0',
                                    cursor: 'pointer',
                                  }}
                                />
                              ) : null}
                            </Col>
                          </Row>
                        ) : null}
                        <Button
                          type="primary"
                          htmlType="submit"
                          size="large"
                          block
                          loading={submitting}
                        >
                          {oauthState.bindMode ? '登录并绑定' : '登录'}
                        </Button>
                      </Form>
                    ),
                  },
                  ...(oauthState.bindMode
                    ? []
                    : [
                        {
                          key: 'sms',
                          label: '短信登录',
                          children: (
                            <Form
                              form={smsForm}
                              layout="vertical"
                              initialValues={{
                                tenantName: initialTenantName,
                                mobile: '',
                                smscode: '',
                              }}
                              onFinish={handleSmsLogin}
                            >
                              <Form.Item
                                name="tenantName"
                                rules={[{ required: true, message: '请输入租户名称' }]}
                              >
                                <Input size="large" placeholder="请输入租户名称" />
                              </Form.Item>
                              <Form.Item
                                name="mobile"
                                rules={[
                                  { required: true, message: '请输入手机号' },
                                  {
                                    pattern: /^1\d{10}$/,
                                    message: '请输入正确的手机号',
                                  },
                                ]}
                              >
                                <Input
                                  size="large"
                                  prefix={<MobileOutlined />}
                                  placeholder="请输入手机号"
                                />
                              </Form.Item>
                              <Form.Item
                                name="smscode"
                                rules={[
                                  { required: true, message: '请输入短信验证码' },
                                  { pattern: /^\d{4,6}$/, message: '请输入正确的验证码' },
                                ]}
                              >
                                <Input
                                  size="large"
                                  prefix={<MessageOutlined />}
                                  placeholder="请输入短信验证码"
                                  suffix={
                                    <Button
                                      type="link"
                                      size="small"
                                      loading={smsSending}
                                      disabled={smsCountdown > 0}
                                      onClick={() => handleSmsSend().catch(() => undefined)}
                                    >
                                      {smsCountdown > 0
                                        ? `${smsCountdown}秒后重发`
                                        : '发送验证码'}
                                    </Button>
                                  }
                                />
                              </Form.Item>
                              <Button
                                type="primary"
                                htmlType="submit"
                                size="large"
                                block
                                loading={submitting}
                              >
                                登录
                              </Button>
                            </Form>
                          ),
                        },
                      ]),
                ]}
              />

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  marginTop: 8,
                }}
              >
                <Button type="link" onClick={() => setResetVisible(true)}>
                  忘记密码
                </Button>
                {oauthState.bindMode ? (
                  <Button type="link" onClick={clearOauthQuery}>
                    取消绑定
                  </Button>
                ) : null}
              </div>

              {oauthProviders.length > 0 && !oauthState.bindMode ? (
                <div style={{ marginTop: 24 }}>
                  <div
                    style={{
                      display: 'flex',
                      gap: 12,
                      flexWrap: 'wrap',
                      marginTop: 12,
                    }}
                  >
                    {oauthProviders.map((provider) => (
                      <Button
                        key={provider.value}
                        size="large"
                        onClick={async () => {
                          const response = await getOauthAuthorizeUrl(provider.value, {
                            frontUrl: `${window.location.origin}/platform/login`,
                            appId: APP_ID,
                            redirect:
                              new URLSearchParams(location.search).get('redirect') || '',
                          });
                          window.location.href = response.data.authorizeUrl;
                        }}
                      >
                        {getProviderLabel(provider)}
                      </Button>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          </Col>
        </Row>
      </Card>

      <Modal
        title="重置密码"
        open={resetVisible}
        forceRender
        onCancel={resetModal}
        onOk={() => handleResetPassword().catch(() => undefined)}
        confirmLoading={resetSubmitting}
        destroyOnHidden
      >
        <Steps
          size="small"
          current={resetStep}
          items={[
            { title: '验证账号' },
            { title: '发送验证码' },
            { title: '设置新密码' },
          ]}
          style={{ marginBottom: 24 }}
        />
        <Form form={resetForm} layout="vertical">
          <Form.Item
            name="loginname"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" disabled={resetStep > 0} />
          </Form.Item>
          {resetStep >= 1 ? (
            <Form.Item
              name="type"
              label="验证方式"
              rules={[{ required: true, message: '请选择验证方式' }]}
            >
              <Radio.Group options={resetTypeOptions} />
            </Form.Item>
          ) : null}
          {resetStep >= 2 ? (
            <>
              <Form.Item
                name="password"
                label="新密码"
                rules={[{ required: true, message: '请输入新密码' }]}
              >
                <Input.Password placeholder="请输入新密码" />
              </Form.Item>
              <Form.Item
                name="code"
                label="验证码"
                rules={[{ required: true, message: '请输入验证码' }]}
              >
                <Input placeholder="请输入验证码" />
              </Form.Item>
            </>
          ) : null}
        </Form>
        {resetHint ? (
          <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
            {resetHint}
          </Typography.Paragraph>
        ) : null}
      </Modal>
    </div>
  );
};

export default LoginPage;
