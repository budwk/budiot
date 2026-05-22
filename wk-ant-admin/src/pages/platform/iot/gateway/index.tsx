import {
  DeleteOutlined,
  EditOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  PoweroffOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
  Switch,
  Tag,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createIotGateway,
  deleteIotGateway,
  getIotGatewayDetail,
  getIotGatewayMeta,
  getIotGatewayPage,
  startIotGateway,
  stopIotGateway,
  suspendIotGateway,
  updateIotGateway,
} from '@/services/budiot/iot/gateway';
import type { IotGatewayMeta, IotGatewayRecord } from '@/services/budiot/iot/typing';
import {
  formatDateTime,
  resolveIotOptionText,
  resolveIotOptionValue,
  runtimeStatusColorMap,
  toSelectOptions,
} from '../shared';

type GatewayFormValues = {
  id?: string;
  name: string;
  networkProtocol: string;
  gatewayMode: string;
  protocolId?: string;
  host?: string;
  port?: number;
  path?: string;
  remoteHost?: string;
  remotePort?: number;
  clientId?: string;
  username?: string;
  password?: string;
  subscribeTopic?: string;
  publishTopic?: string;
  allowAnonymous: boolean;
  autoStart: boolean;
  disabled: boolean;
  description?: string;
};

const defaultValues: GatewayFormValues = {
  name: '',
  networkProtocol: '',
  gatewayMode: '',
  protocolId: '',
  host: '',
  port: undefined,
  path: '',
  remoteHost: '',
  remotePort: undefined,
  clientId: '',
  username: '',
  password: '',
  subscribeTopic: '',
  publishTopic: '',
  allowAnonymous: false,
  autoStart: true,
  disabled: false,
  description: '',
};

const GatewayPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<GatewayFormValues>();
  const [meta, setMeta] = React.useState<IotGatewayMeta>({
    networkProtocols: [],
    gatewayModes: [],
    runtimeStatuses: [],
    protocols: [],
  });
  const [loading, setLoading] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [submitting, setSubmitting] = React.useState(false);
  const networkProtocol = Form.useWatch('networkProtocol', form);

  React.useEffect(() => {
    const initialize = async () => {
      setLoading(true);
      try {
        const response = await getIotGatewayMeta();
        setMeta(response.data);
      } finally {
        setLoading(false);
      }
    };
    initialize().catch(() => undefined);
  }, []);

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({
      ...defaultValues,
      networkProtocol: meta.networkProtocols[0]?.value || '',
      gatewayMode: meta.gatewayModes[0]?.value || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: IotGatewayRecord) => {
    const response = await getIotGatewayDetail(record.id);
    setEditingId(record.id);
    form.setFieldsValue({
      id: response.data.id,
      name: response.data.name,
      networkProtocol: resolveIotOptionValue(response.data.networkProtocol),
      gatewayMode: resolveIotOptionValue(response.data.gatewayMode),
      protocolId: response.data.protocolId || '',
      host: response.data.host || '',
      port: response.data.port,
      path: response.data.path || '',
      remoteHost: response.data.remoteHost || '',
      remotePort: response.data.remotePort,
      clientId: response.data.clientId || '',
      username: response.data.username || '',
      password: response.data.password || '',
      subscribeTopic: response.data.subscribeTopic || '',
      publishTopic: response.data.publishTopic || '',
      allowAnonymous: Boolean(response.data.allowAnonymous),
      autoStart: Boolean(response.data.autoStart),
      disabled: response.data.disabled,
      description: response.data.description || '',
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotGateway({ ...values, id: editingId });
      } else {
        await createIotGateway(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<IotGatewayRecord>[]>(
    () => [
      { title: '网关名称', dataIndex: 'name' },
      {
        title: '通信协议',
        dataIndex: 'networkProtocol',
        width: 140,
        valueType: 'select',
        fieldProps: { options: toSelectOptions(meta.networkProtocols), allowClear: true },
        render: (_, record) => resolveIotOptionText(meta.networkProtocols, record.networkProtocol),
      },
      {
        title: '运行模式',
        dataIndex: 'gatewayMode',
        width: 140,
        search: false,
        render: (_, record) => resolveIotOptionText(meta.gatewayModes, record.gatewayMode),
      },
      {
        title: '状态',
        dataIndex: 'runtimeStatus',
        width: 120,
        valueType: 'select',
        fieldProps: { options: toSelectOptions(meta.runtimeStatuses), allowClear: true },
        render: (_, record) => {
          const text = resolveIotOptionText(meta.runtimeStatuses, record.runtimeStatus);
          const value = resolveIotOptionValue(record.runtimeStatus);
          return <Tag color={runtimeStatusColorMap[value] || 'default'}>{text}</Tag>;
        },
      },
      {
        title: '连接信息',
        search: false,
        render: (_, record) =>
          [record.host, record.port, record.path].filter(Boolean).join(' ') ||
          [record.remoteHost, record.remotePort].filter(Boolean).join(':') ||
          '-',
      },
      {
        title: '最后心跳',
        search: false,
        width: 180,
        render: (_, record) => formatDateTime(record.lastSeenAt),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 260,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'start',
                label: '启动',
                icon: <PlayCircleOutlined />,
                disabled: !access.hasPermission('iot.manage.gateway.update'),
                onClick: async () => {
                  await startIotGateway(record.id);
                  actionRef.current?.reload();
                },
              },
              {
                key: 'suspend',
                label: '挂起',
                icon: <PauseCircleOutlined />,
                disabled: !access.hasPermission('iot.manage.gateway.update'),
                onClick: async () => {
                  await suspendIotGateway(record.id);
                  actionRef.current?.reload();
                },
              },
              {
                key: 'stop',
                label: '停止',
                icon: <PoweroffOutlined />,
                disabled: !access.hasPermission('iot.manage.gateway.update'),
                onClick: async () => {
                  await stopIotGateway(record.id);
                  actionRef.current?.reload();
                },
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('iot.manage.gateway.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                disabled: !access.hasPermission('iot.manage.gateway.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除网关',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteIotGateway(record.id);
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
    [access, meta, modal],
  );

  return (
    <PageContainer title="网关管理" loading={loading}>
      <PlatformProTable<IotGatewayRecord, { name?: string; networkProtocol?: string; runtimeStatus?: string; current?: number; pageSize?: number }>
        persistenceKey="platform-iot-gateway-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="网关列表"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.gateway.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getIotGatewayPage({
            name: params.name || '',
            networkProtocol: params.networkProtocol || '',
            runtimeStatus: params.runtimeStatus || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
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
        title={editingId ? '修改网关' : '新增网关'}
        open={modalOpen}
        forceRender
        width={880}
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
            <Form.Item label="网关名称" name="name" rules={[{ required: true, message: '请输入网关名称' }]}>
              <Input />
            </Form.Item>
            <Form.Item label="协议脚本" name="protocolId">
              <Select
                allowClear
                options={meta.protocols.map((item) => ({ label: item.name, value: item.id }))}
              />
            </Form.Item>
            <Form.Item label="通信协议" name="networkProtocol" rules={[{ required: true, message: '请选择通信协议' }]}>
              <Select options={toSelectOptions(meta.networkProtocols)} />
            </Form.Item>
            <Form.Item label="运行模式" name="gatewayMode" rules={[{ required: true, message: '请选择运行模式' }]}>
              <Select options={toSelectOptions(meta.gatewayModes)} />
            </Form.Item>
            <Form.Item label="主机地址" name="host">
              <Input />
            </Form.Item>
            <Form.Item label="端口" name="port">
              <InputNumber style={{ width: '100%' }} />
            </Form.Item>
            {networkProtocol === 'HTTP' ? (
              <Form.Item label="路径" name="path">
                <Input />
              </Form.Item>
            ) : null}
            {networkProtocol === 'MQTT' ? (
              <>
                <Form.Item label="Broker 地址" name="remoteHost">
                  <Input />
                </Form.Item>
                <Form.Item label="Broker 端口" name="remotePort">
                  <InputNumber style={{ width: '100%' }} />
                </Form.Item>
                <Form.Item label="Client ID" name="clientId">
                  <Input />
                </Form.Item>
                <Form.Item label="用户名" name="username">
                  <Input />
                </Form.Item>
                <Form.Item label="密码" name="password">
                  <Input.Password />
                </Form.Item>
                <Form.Item label="订阅 Topic" name="subscribeTopic">
                  <Input />
                </Form.Item>
                <Form.Item label="发布 Topic" name="publishTopic">
                  <Input />
                </Form.Item>
              </>
            ) : null}
          </div>
          <Space size={24} style={{ marginBottom: 16 }}>
            <Form.Item label="匿名访问" name="allowAnonymous" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="自动启动" name="autoStart" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="禁用" name="disabled" valuePropName="checked">
              <Switch />
            </Form.Item>
          </Space>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default GatewayPage;
