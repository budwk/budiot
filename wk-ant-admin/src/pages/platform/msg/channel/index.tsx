import {
  CheckOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Divider,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Space,
  Switch,
  Tag,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createMsgChannel,
  deleteMsgChannel,
  getMsgChannelDetail,
  getMsgChannelMeta,
  getMsgChannelPage,
  setDefaultMsgChannel,
  updateMsgChannel,
} from '@/services/budiot/msg/channel';
import type { MsgChannelRecord, MsgOption } from '@/services/budiot/typing';

type MsgChannelFormValues = {
  id?: string;
  name: string;
  code: string;
  channelType: string;
  providerType: string;
  defaultFlag: boolean;
  disabled: boolean;
};

type MsgChannelTableParams = {
  current?: number;
  pageSize?: number;
  name?: string;
  providerType?: string;
  disabled?: boolean;
};

type ProviderConfigState = Record<string, string | number | boolean | undefined>;

const defaultChannelFormValues: MsgChannelFormValues = {
  name: '',
  code: '',
  channelType: '',
  providerType: '',
  defaultFlag: false,
  disabled: false,
};

const defaultConfigMap: Record<string, ProviderConfigState> = {
  ALIYUN_SMS: {
    accessKeyId: '',
    accessKeySecret: '',
    regionId: 'cn-hangzhou',
    signName: '',
  },
  TENCENT_SMS: {
    secretId: '',
    secretKey: '',
    sdkAppId: '',
    region: 'ap-guangzhou',
    signName: '',
  },
  SMTP: {
    host: '',
    port: 465,
    username: '',
    password: '',
    from: '',
    ssl: true,
    starttls: false,
  },
  DINGTALK_BOT: {
    webhookUrl: '',
    secret: '',
  },
  WECOM_BOT: {
    webhookUrl: '',
  },
};

const providerChannelMap: Record<string, string> = {
  ALIYUN_SMS: 'SMS',
  TENCENT_SMS: 'SMS',
  SMTP: 'EMAIL',
  DINGTALK_BOT: 'DINGTALK',
  WECOM_BOT: 'WECOM',
};

const cloneDefaultConfig = (providerType?: string): ProviderConfigState =>
  JSON.parse(JSON.stringify(defaultConfigMap[providerType || ''] || {}));

const resolveOptionValue = (value?: string | MsgOption) =>
  typeof value === 'string' ? value : value?.value || '';

const resolveOptionText = (options: MsgOption[], value?: string | MsgOption) => {
  const currentValue = resolveOptionValue(value);
  return options.find((item) => item.value === currentValue)?.text || currentValue || '-';
};

const parseConfigJson = (value?: string, providerType?: string): ProviderConfigState => {
  const defaults = cloneDefaultConfig(providerType);
  if (!value) {
    return defaults;
  }
  try {
    return {
      ...defaults,
      ...(JSON.parse(value) as ProviderConfigState),
    };
  } catch {
    return defaults;
  }
};

const configGridStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
  gap: 16,
};

const MsgChannelPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<MsgChannelFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [metaLoading, setMetaLoading] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [channelTypes, setChannelTypes] = React.useState<MsgOption[]>([]);
  const [providerTypes, setProviderTypes] = React.useState<MsgOption[]>([]);
  const [configForm, setConfigForm] = React.useState<ProviderConfigState>({});
  const currentProviderType = Form.useWatch('providerType', form);

  React.useEffect(() => {
    const initialize = async () => {
      setMetaLoading(true);
      try {
        const response = await getMsgChannelMeta();
        setChannelTypes(response.data.channelTypes || []);
        setProviderTypes(response.data.providerTypes || []);
      } finally {
        setMetaLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const updateConfigField = React.useCallback(
    (key: string, value: string | number | boolean) => {
      setConfigForm((prev) => ({
        ...prev,
        [key]: value,
      }));
    },
    [],
  );

  const handleProviderChange = React.useCallback(
    (providerType?: string) => {
      form.setFieldValue('channelType', providerChannelMap[providerType || ''] || '');
      setConfigForm(cloneDefaultConfig(providerType));
    },
    [form],
  );

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue(defaultChannelFormValues);
    setConfigForm({});
    setModalOpen(true);
  };

  const openEdit = async (record: MsgChannelRecord) => {
    const response = await getMsgChannelDetail(record.id);
    const detail = response.data;
    const providerType = resolveOptionValue(detail.providerType);
    setEditingId(record.id);
    form.setFieldsValue({
      id: detail.id,
      name: detail.name,
      code: detail.code,
      channelType: resolveOptionValue(detail.channelType),
      providerType,
      defaultFlag: detail.defaultFlag,
      disabled: detail.disabled,
    });
    setConfigForm(parseConfigJson(detail.configJson, providerType));
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      const payload = {
        ...values,
        id: editingId,
        configJson: JSON.stringify(configForm || {}),
      };
      if (editingId) {
        await updateMsgChannel(payload);
      } else {
        await createMsgChannel(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<MsgChannelRecord>[]>(
    () => [
      {
        title: '渠道名称',
        dataIndex: 'name',
      },
      {
        title: '提供商',
        dataIndex: 'providerType',
        valueType: 'select',
        fieldProps: {
          options: providerTypes.map((item) => ({
            label: item.text,
            value: item.value,
          })),
          allowClear: true,
        },
        render: (_, record) => resolveOptionText(providerTypes, record.providerType),
        width: 140,
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        valueType: 'select',
        fieldProps: {
          options: [
            { label: '启用', value: false },
            { label: '禁用', value: true },
          ],
          allowClear: true,
        },
        render: (_, record) =>
          record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
        width: 100,
      },
      {
        title: '渠道编码',
        dataIndex: 'code',
        search: false,
        width: 140,
      },
      {
        title: '渠道类型',
        dataIndex: 'channelType',
        search: false,
        render: (_, record) => resolveOptionText(channelTypes, record.channelType),
        width: 120,
      },
      {
        title: '默认',
        dataIndex: 'defaultFlag',
        search: false,
        width: 90,
        render: (_, record) =>
          record.defaultFlag ? <Tag color="success">默认</Tag> : <Tag>普通</Tag>,
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        search: false,
        valueType: 'dateTime',
        width: 180,
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 200,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('msg.manage.channel.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'default',
                label: '设默认',
                icon: <CheckOutlined />,
                hidden: record.defaultFlag,
                disabled: !access.hasPermission('msg.manage.channel.update'),
                onClick: () => void setDefaultMsgChannel(record.id).then(() => actionRef.current?.reload()),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('msg.manage.channel.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除渠道',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteMsgChannel(record.id);
                      actionRef.current?.reload();
                    },
                  });
                },
              },
            ]}
          />
        ),
      },
    ],
    [access, channelTypes, modal, providerTypes],
  );

  const renderConfigFields = () => {
    switch (currentProviderType) {
      case 'ALIYUN_SMS':
        return (
          <div style={configGridStyle}>
            <Form.Item label="AccessKeyId">
              <Input
                value={String(configForm.accessKeyId || '')}
                onChange={(event) => updateConfigField('accessKeyId', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="AccessKeySecret">
              <Input
                value={String(configForm.accessKeySecret || '')}
                onChange={(event) => updateConfigField('accessKeySecret', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="RegionId">
              <Input
                value={String(configForm.regionId || '')}
                onChange={(event) => updateConfigField('regionId', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="短信签名">
              <Input
                value={String(configForm.signName || '')}
                onChange={(event) => updateConfigField('signName', event.target.value)}
              />
            </Form.Item>
          </div>
        );
      case 'TENCENT_SMS':
        return (
          <div style={configGridStyle}>
            <Form.Item label="SecretId">
              <Input
                value={String(configForm.secretId || '')}
                onChange={(event) => updateConfigField('secretId', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="SecretKey">
              <Input
                value={String(configForm.secretKey || '')}
                onChange={(event) => updateConfigField('secretKey', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="SdkAppId">
              <Input
                value={String(configForm.sdkAppId || '')}
                onChange={(event) => updateConfigField('sdkAppId', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="Region">
              <Input
                value={String(configForm.region || '')}
                onChange={(event) => updateConfigField('region', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="短信签名">
              <Input
                value={String(configForm.signName || '')}
                onChange={(event) => updateConfigField('signName', event.target.value)}
              />
            </Form.Item>
          </div>
        );
      case 'SMTP':
        return (
          <div style={configGridStyle}>
            <Form.Item label="SMTP Host">
              <Input
                value={String(configForm.host || '')}
                onChange={(event) => updateConfigField('host', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="SMTP Port">
              <InputNumber
                style={{ width: '100%' }}
                value={Number(configForm.port || 0)}
                onChange={(value) => updateConfigField('port', value || 0)}
              />
            </Form.Item>
            <Form.Item label="用户名">
              <Input
                value={String(configForm.username || '')}
                onChange={(event) => updateConfigField('username', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="密码">
              <Input.Password
                value={String(configForm.password || '')}
                onChange={(event) => updateConfigField('password', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="发件邮箱">
              <Input
                value={String(configForm.from || '')}
                onChange={(event) => updateConfigField('from', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="StartTLS">
              <Switch
                checked={Boolean(configForm.starttls)}
                onChange={(checked) => updateConfigField('starttls', checked)}
              />
            </Form.Item>
            <Form.Item label="SSL">
              <Switch
                checked={Boolean(configForm.ssl)}
                onChange={(checked) => updateConfigField('ssl', checked)}
              />
            </Form.Item>
          </div>
        );
      case 'DINGTALK_BOT':
        return (
          <div style={{ display: 'grid', gap: 16 }}>
            <Form.Item label="Webhook">
              <Input
                value={String(configForm.webhookUrl || '')}
                onChange={(event) => updateConfigField('webhookUrl', event.target.value)}
              />
            </Form.Item>
            <Form.Item label="加签Secret">
              <Input
                value={String(configForm.secret || '')}
                onChange={(event) => updateConfigField('secret', event.target.value)}
              />
            </Form.Item>
          </div>
        );
      case 'WECOM_BOT':
        return (
          <Form.Item label="Webhook">
            <Input
              value={String(configForm.webhookUrl || '')}
              onChange={(event) => updateConfigField('webhookUrl', event.target.value)}
            />
          </Form.Item>
        );
      default:
        return <Tag color="default">请选择提供商后填写对应配置</Tag>;
    }
  };

  return (
    <PageContainer title="消息渠道">
      <PlatformProTable<MsgChannelRecord, MsgChannelTableParams>
        persistenceKey="platform-msg-channel-table"
        actionRef={actionRef}
        rowKey="id"
        loading={metaLoading}
        headerTitle="渠道列表"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('msg.manage.channel.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getMsgChannelPage({
            name: params.name || '',
            providerType: params.providerType || '',
            disabled: params.disabled,
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
            pageOrderName: 'updatedAt',
            pageOrderBy: 'descending',
          });
          return {
            data: response.data.list || [],
            total: response.data.totalCount || 0,
            success: true,
          };
        }}
      />

      <Modal
        title={editingId ? '修改渠道' : '新增渠道'}
        open={modalOpen}
        forceRender
        width={760}
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <div style={configGridStyle}>
            <Form.Item
              label="渠道名称"
              name="name"
              rules={[{ required: true, message: '请输入渠道名称' }]}
            >
              <Input placeholder="请输入渠道名称" />
            </Form.Item>
            <Form.Item
              label="渠道编码"
              name="code"
              rules={[{ required: true, message: '请输入渠道编码' }]}
            >
              <Input placeholder="请输入渠道编码" />
            </Form.Item>
            <Form.Item
              label="提供商"
              name="providerType"
              rules={[{ required: true, message: '请选择提供商' }]}
            >
              <Radio.Group
                options={providerTypes.map((item) => ({
                  label: item.text,
                  value: item.value,
                }))}
                onChange={(event) => handleProviderChange(event.target.value)}
              />
            </Form.Item>
            <Form.Item label="渠道类型" name="channelType">
              <Input disabled placeholder="根据提供商自动带出" />
            </Form.Item>
            <Form.Item label="默认渠道" name="defaultFlag">
              <Radio.Group
                options={[
                  { label: '是', value: true },
                  { label: '否', value: false },
                ]}
              />
            </Form.Item>
            <Form.Item label="启用状态" name="disabled">
              <Radio.Group
                options={[
                  { label: '启用', value: false },
                  { label: '禁用', value: true },
                ]}
              />
            </Form.Item>
          </div>
          <Divider>渠道配置</Divider>
          {renderConfigFields()}
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default MsgChannelPage;
