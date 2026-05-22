import { SaveOutlined } from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  Button,
  Card,
  Checkbox,
  Col,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Switch,
  Typography,
} from 'antd';
import * as React from 'react';
import {
  getSecurityConfig,
  saveSecurityConfig,
} from '@/services/budiot/sys/security';
import type { SysSecurityConfig } from '@/services/budiot/typing';

type SecurityFormValues = SysSecurityConfig & {
  pwdCharNotValues?: string[];
};

const defaultFormValues: SecurityFormValues = {
  id: '',
  hasEnabled: false,
  pwdLengthMin: 6,
  pwdLengthMax: 20,
  pwdCharMust: 0,
  pwdCharNot: '',
  pwdCharNotValues: [],
  pwdRepeatCheck: false,
  pwdRepeatNum: 0,
  pwdRetryLock: false,
  pwdRetryNum: 0,
  pwdRetryAction: 0,
  pwdRetryTime: 0,
  pwdTimeoutDay: 0,
  pwdResetChange: false,
  nameRetryLock: false,
  nameRetryNum: 3,
  nameTimeout: 60,
  userSessionOnlyOne: false,
  captchaHasEnabled: false,
  captchaType: 0,
};

const SecurityPage = () => {
  const access = useAccess();
  const [form] = Form.useForm<SecurityFormValues>();
  const [loading, setLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);

  const hasEnabled = Form.useWatch('hasEnabled', form);
  const pwdRepeatCheck = Form.useWatch('pwdRepeatCheck', form);
  const pwdRetryLock = Form.useWatch('pwdRetryLock', form);
  const pwdRetryAction = Form.useWatch('pwdRetryAction', form);
  const nameRetryLock = Form.useWatch('nameRetryLock', form);
  const captchaHasEnabled = Form.useWatch('captchaHasEnabled', form);

  const loadConfig = React.useCallback(async () => {
    setLoading(true);
    try {
      const response = await getSecurityConfig();
      const data = response.data;
      form.setFieldsValue({
        ...data,
        pwdCharNotValues: data.pwdCharNot ? data.pwdCharNot.split(',') : [],
      });
    } finally {
      setLoading(false);
    }
  }, [form]);

  React.useEffect(() => {
    form.setFieldsValue(defaultFormValues);
    loadConfig().catch(() => undefined);
  }, [form, loadConfig]);

  const submit = async () => {
    await form.validateFields();
    const values = form.getFieldsValue(true) as SecurityFormValues;
    setSubmitting(true);
    try {
      const { pwdCharNotValues = [], ...rest } = values;
      await saveSecurityConfig({
        ...rest,
        id: values.id,
        pwdCharNot: pwdCharNotValues.toString(),
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <PageContainer
      title="安全配置"
      extra={[
        <Button
          key="save"
          type="primary"
          icon={<SaveOutlined />}
          loading={submitting}
          disabled={!access.hasPermission('sys.config.security.save')}
          onClick={submit}
        >
          保存
        </Button>,
      ]}
    >
      <Form form={form} layout="vertical" disabled={loading}>
        <Form.Item name="id" hidden>
          <Input />
        </Form.Item>
        <ProCard ghost direction="column" gutter={[0, 16]}>
          <ProCard>
            <Card title="账户安全配置">
              <Form.Item label="是否启用账户安全配置" name="hasEnabled" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Card>
          </ProCard>

          {hasEnabled ? (
            <>
              <ProCard>
                <Card title="用户登录会话">
                  <Form.Item
                    label="是否启用用户单一登录"
                    name="userSessionOnlyOne"
                    valuePropName="checked"
                  >
                    <Space orientation="vertical" size={4}>
                      <Switch />
                      <Typography.Text type="secondary">
                        启用后用户新登录会踢下其他会话，建议配合 WebSocket 会话通知一起开启。
                      </Typography.Text>
                    </Space>
                  </Form.Item>
                </Card>
              </ProCard>

              <ProCard>
                <Card title="密码规则配置">
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item label="密码最小长度" name="pwdLengthMin">
                        <InputNumber min={1} max={50} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="密码最大长度" name="pwdLengthMax">
                        <InputNumber min={1} max={50} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="密码字符要求" name="pwdCharMust">
                        <Select
                          options={[
                            { label: '无要求', value: 0 },
                            { label: '必须同时包含字母和数字', value: 1 },
                            { label: '必须同时包含大写字母、小写字母、数字', value: 2 },
                            {
                              label: '必须同时包含大写、小写字母、特殊字符、数字',
                              value: 3,
                            },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="密码中不能包含的用户信息" name="pwdCharNotValues">
                        <Checkbox.Group
                          options={[
                            { label: '用户名', value: 'loginname' },
                            { label: '电子邮箱', value: 'email' },
                            { label: '手机号', value: 'mobile' },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        label="是否开启密码重复性检查"
                        name="pwdRepeatCheck"
                        valuePropName="checked"
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="重复性检查记录数" name="pwdRepeatNum">
                        <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                          <InputNumber
                            min={0}
                            max={99}
                            disabled={!pwdRepeatCheck}
                            style={{ width: '100%' }}
                          />
                          <Typography.Text type="secondary">
                            设置为 0 或空时不校验历史重复密码。
                          </Typography.Text>
                        </Space>
                      </Form.Item>
                    </Col>
                  </Row>
                </Card>
              </ProCard>

              <ProCard>
                <Card title="密码校检配置">
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                        label="一天内密码错误次数超过最大重试次数锁定账号"
                        name="pwdRetryLock"
                        valuePropName="checked"
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="密码错误最大重试次数" name="pwdRetryNum">
                        <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                          <InputNumber
                            min={0}
                            max={99}
                            disabled={!pwdRetryLock}
                            style={{ width: '100%' }}
                          />
                          <Typography.Text type="secondary">
                            不设置或为 0 时不启用登录密码重试校验。
                          </Typography.Text>
                        </Space>
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="超过最大重试次数处理方式" name="pwdRetryAction">
                        <Select
                          disabled={!pwdRetryLock}
                          options={[
                            { label: '不处理', value: 0 },
                            { label: '锁定账户', value: 1 },
                            { label: '指定时间内禁止登录', value: 2 },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="禁止登录时长(秒)" name="pwdRetryTime">
                        <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                          <InputNumber
                            min={0}
                            max={25920000}
                            disabled={!pwdRetryLock || pwdRetryAction !== 2}
                            style={{ width: '100%' }}
                          />
                          <Typography.Text type="secondary">
                            0 表示不锁定，最长支持 100 天。
                          </Typography.Text>
                        </Space>
                      </Form.Item>
                    </Col>
                  </Row>
                </Card>
              </ProCard>

              <ProCard>
                <Card title="密码变更通知">
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item label="指定密码过期时间(天)" name="pwdTimeoutDay">
                        <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                          <InputNumber min={0} max={10000000} style={{ width: '100%' }} />
                          <Typography.Text type="secondary">
                            0 或空表示密码永不过期。
                          </Typography.Text>
                        </Space>
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item
                        label="后台重置密码后下次登录强制修改"
                        name="pwdResetChange"
                        valuePropName="checked"
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>
                </Card>
              </ProCard>

              <ProCard>
                <Card title="防攻击配置">
                  <Row gutter={16}>
                    <Col span={12}>
                      <Form.Item
                        label="用户名/手机号超过最大输错次数锁定 IP"
                        name="nameRetryLock"
                        valuePropName="checked"
                      >
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="用户名/手机号最大重试次数" name="nameRetryNum">
                        <InputNumber
                          min={0}
                          max={99}
                          disabled={!nameRetryLock}
                          style={{ width: '100%' }}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={12}>
                      <Form.Item label="IP 禁止登录时长(秒)" name="nameTimeout">
                        <InputNumber
                          min={0}
                          max={25920000}
                          disabled={!nameRetryLock}
                          style={{ width: '100%' }}
                        />
                      </Form.Item>
                    </Col>
                  </Row>
                </Card>
              </ProCard>
            </>
          ) : null}

          <ProCard>
            <Card title="登录验证码配置">
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="是否启用登录验证码"
                    name="captchaHasEnabled"
                    valuePropName="checked"
                  >
                    <Switch />
                  </Form.Item>
                </Col>
                {captchaHasEnabled ? (
                  <Col span={12}>
                    <Form.Item label="验证码类型" name="captchaType">
                      <Select
                        options={[
                          { label: '数学题', value: 0 },
                          { label: '四位纯数字', value: 1 },
                          { label: '四位纯字母', value: 2 },
                          { label: '四位数字字母', value: 3 },
                          { label: '四位汉字', value: 4 },
                          { label: '两位汉字', value: 5 },
                          { label: '四位纯数字(动态图片)', value: 11 },
                          { label: '四位纯字母(动态图片)', value: 22 },
                          { label: '四位数字字母(动态图片)', value: 33 },
                          { label: '四位汉字(动态图片)', value: 44 },
                          { label: '两位汉字(动态图片)', value: 55 },
                        ]}
                      />
                    </Form.Item>
                  </Col>
                ) : null}
              </Row>
            </Card>
          </ProCard>
        </ProCard>
      </Form>
    </PageContainer>
  );
};

export default SecurityPage;
