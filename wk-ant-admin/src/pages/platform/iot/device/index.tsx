import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  SendOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ActionType } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import {
  App,
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Switch,
  Tag,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import { createIotCommand } from '@/services/budiot/iot/command';
import {
  batchCreateIotDevice,
  createIotDevice,
  deleteIotDevice,
  getIotDeviceDetail,
  getIotDeviceMeta,
  getIotDevicePage,
  updateIotDevice,
} from '@/services/budiot/iot/device';
import type {
  IotDeviceRecord,
  IotProductRecord,
  IotThingService,
  IotThingServiceParam,
} from '@/services/budiot/iot/typing';
import { history, useAccess } from '@umijs/max';
import { formatDateTime, safeJsonParse, safeJsonStringify } from '../shared';

type DeviceFormValues = {
  id?: string;
  deviceCode: string;
  name?: string;
  imei?: string;
  iccid?: string;
  productId: string;
  secretKey?: string;
  description?: string;
  disabled: boolean;
};

type BatchFormValues = {
  productId: string;
  deviceCodes: string;
  disabled: boolean;
};

type CommandFormValues = {
  deviceIds: string[];
  productId?: string;
  serviceIdentifier?: string;
  params?: Record<string, unknown>;
  replyRequired: boolean;
  deadlineAt?: dayjs.Dayjs | null;
};

const defaultDeviceValues: DeviceFormValues = {
  deviceCode: '',
  name: '',
  imei: '',
  iccid: '',
  productId: '',
  secretKey: '',
  description: '',
  disabled: false,
};

const defaultCommandValues: CommandFormValues = {
  deviceIds: [],
  productId: '',
  serviceIdentifier: undefined,
  params: {},
  replyRequired: false,
  deadlineAt: null,
};

const DevicePage: React.FC = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [deviceForm] = Form.useForm<DeviceFormValues>();
  const [batchForm] = Form.useForm<BatchFormValues>();
  const [commandForm] = Form.useForm<CommandFormValues>();
  const [products, setProducts] = React.useState<IotProductRecord[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [batchOpen, setBatchOpen] = React.useState(false);
  const [commandOpen, setCommandOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [submitting, setSubmitting] = React.useState(false);
  const [selectedRows, setSelectedRows] = React.useState<IotDeviceRecord[]>([]);
  const selectedCommandProductId = Form.useWatch('productId', commandForm);
  const selectedServiceIdentifier = Form.useWatch('serviceIdentifier', commandForm);

  const selectedCommandProduct = React.useMemo(
    () => products.find((item) => item.id === selectedCommandProductId),
    [products, selectedCommandProductId],
  );
  const thingServices = React.useMemo(
    () =>
      safeJsonParse<IotThingService[]>(
        selectedCommandProduct?.thingServiceJson,
        [],
      ),
    [selectedCommandProduct],
  );
  const selectedService = React.useMemo(
    () =>
      thingServices.find((item) => item.identifier === selectedServiceIdentifier) ||
      thingServices[0],
    [selectedServiceIdentifier, thingServices],
  );
  const selectedRowProductIds = React.useMemo(
    () => Array.from(new Set(selectedRows.map((item) => item.productId).filter(Boolean))),
    [selectedRows],
  );

  React.useEffect(() => {
    const initialize = async () => {
      const response = await getIotDeviceMeta();
      setProducts(response.data.products || []);
    };
    initialize().catch(() => undefined);
  }, []);

  React.useEffect(() => {
    if (!commandOpen) {
      return;
    }
    const currentIdentifier = commandForm.getFieldValue('serviceIdentifier');
    if (!thingServices.some((item) => item.identifier === currentIdentifier)) {
      commandForm.setFieldsValue({
        serviceIdentifier: thingServices[0]?.identifier,
        params: {},
      });
    }
  }, [commandForm, commandOpen, thingServices]);

  const openCreate = () => {
    setEditingId(undefined);
    deviceForm.resetFields();
    deviceForm.setFieldsValue({
      ...defaultDeviceValues,
      productId: products[0]?.id || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: IotDeviceRecord) => {
    const response = await getIotDeviceDetail(record.id);
    setEditingId(record.id);
    deviceForm.setFieldsValue({
      id: response.data.device.id,
      deviceCode: response.data.device.deviceCode,
      name: response.data.device.deviceName || response.data.device.name || '',
      imei: (response.data.device as any).imei || '',
      iccid: (response.data.device as any).iccid || '',
      productId: response.data.device.productId || '',
      secretKey: (response.data.device as any).secretKey || '',
      description: response.data.device.description || '',
      disabled: response.data.device.disabled,
    });
    setModalOpen(true);
  };

  const submitDevice = async () => {
    const values = await deviceForm.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotDevice({ ...values, id: editingId });
      } else {
        await createIotDevice(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const submitBatch = async () => {
    const values = await batchForm.validateFields();
    const deviceCodes = values.deviceCodes
      .split(/[\n,，;；]/)
      .map((item) => item.trim())
      .filter(Boolean);
    if (!deviceCodes.length) {
      message.warning('请至少输入一个设备编号');
      return;
    }
    await batchCreateIotDevice({
      productId: values.productId,
      deviceCodes,
      disabled: values.disabled,
    });
    setBatchOpen(false);
    actionRef.current?.reload();
  };

  const openCommand = (record?: IotDeviceRecord) => {
    const deviceIds = record ? [record.id] : selectedRows.map((item) => item.id);
    const productIds = Array.from(
      new Set((record ? [record.productId] : selectedRows.map((item) => item.productId)).filter(Boolean)),
    );
    if (!deviceIds.length) {
      message.warning('请先选择设备');
      return;
    }
    if (productIds.length > 1) {
      message.warning('批量下发仅支持同一产品设备');
      return;
    }
    const productId = record?.productId || selectedRows[0]?.productId || '';
    const product = products.find((item) => item.id === productId);
    const services = safeJsonParse<IotThingService[]>(product?.thingServiceJson, []);
    if (!services.length) {
      message.warning('当前产品未配置可下发服务');
      return;
    }
    commandForm.resetFields();
    commandForm.setFieldsValue({
      ...defaultCommandValues,
      deviceIds,
      productId,
      serviceIdentifier: services[0]?.identifier,
      params: {},
    });
    setCommandOpen(true);
  };

  const submitCommand = async () => {
    const values = await commandForm.validateFields();
    if (!selectedService) {
      message.warning('请选择服务指令');
      return;
    }
    const payload = { ...((values.params || {}) as Record<string, unknown>) };
    selectedService.inputParams?.forEach((param) => {
      if (param.dataType === 'object') {
        const currentValue = payload[param.identifier];
        if (typeof currentValue === 'string' && currentValue.trim()) {
          payload[param.identifier] = JSON.parse(currentValue);
        }
      }
    });
    await createIotCommand({
      deviceIds: values.deviceIds,
      productId: values.productId,
      commandCode: selectedService.identifier,
      payloadJson: safeJsonStringify(payload),
      replyRequired: values.replyRequired,
      deadlineAt: values.deadlineAt ? values.deadlineAt.valueOf() : undefined,
    });
    setCommandOpen(false);
  };

  const renderParamField = (param: IotThingServiceParam) => {
    const key = ['params', param.identifier];
    switch (param.dataType) {
      case 'int':
      case 'double':
        return (
          <Form.Item
            key={param.identifier}
            label={`${param.name} (${param.identifier})`}
            name={key}
            rules={
              param.required ? [{ required: true, message: `请输入${param.name}` }] : undefined
            }
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        );
      case 'boolean':
        return (
          <Form.Item
            key={param.identifier}
            label={`${param.name} (${param.identifier})`}
            name={key}
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>
        );
      case 'object':
        return (
          <Form.Item
            key={param.identifier}
            label={`${param.name} (${param.identifier})`}
            name={key}
            rules={[
              ...(param.required
                ? [{ required: true, message: `请输入${param.name}` }]
                : []),
              {
                validator: async (_, value) => {
                  if (!value) {
                    return;
                  }
                  try {
                    JSON.parse(String(value));
                  } catch {
                    throw new Error('请输入合法 JSON');
                  }
                },
              },
            ]}
          >
            <Input.TextArea rows={4} placeholder="请输入 JSON 文本" />
          </Form.Item>
        );
      default:
        return (
          <Form.Item
            key={param.identifier}
            label={`${param.name} (${param.identifier})`}
            name={key}
            rules={
              param.required ? [{ required: true, message: `请输入${param.name}` }] : undefined
            }
          >
            <Input />
          </Form.Item>
        );
    }
  };

  return (
    <PageContainer title="设备管理">
      <PlatformProTable<IotDeviceRecord, { deviceCode?: string; name?: string; productId?: string; disabled?: boolean; current?: number; pageSize?: number }>
        persistenceKey="platform-iot-device-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="设备列表"
        rowSelection={{
          onChange: (_, rows) => setSelectedRows(rows),
        }}
        columns={[
          { title: '设备编号', dataIndex: 'deviceCode', width: 160 },
          { title: '设备名称', dataIndex: 'name' },
          { title: '所属产品', dataIndex: 'productId', width: 180, valueType: 'select', fieldProps: { allowClear: true, options: products.map((item) => ({ label: item.name, value: item.id })) }, render: (_, record) => record.product?.name || '-' },
          { title: '在线状态', search: false, width: 100, render: (_, record) => record.online ? <Tag color="success">在线</Tag> : <Tag>离线</Tag> },
          { title: 'IP', search: false, width: 140, dataIndex: 'ip' },
          { title: '最后心跳', search: false, width: 180, render: (_, record) => formatDateTime(record.lastHeartbeatAt) },
          {
            title: '操作',
            key: 'option',
            valueType: 'option',
            width: 220,
            render: (_, record) => (
              <TableRowActions
                actions={[
                  {
                    key: 'detail',
                    label: '详情',
                    icon: <EyeOutlined />,
                    onClick: () => history.push(`/platform/iot/device/${record.id}`),
                  },
                  {
                    key: 'edit',
                    label: '修改',
                    icon: <EditOutlined />,
                    disabled: !access.hasPermission('iot.manage.device.update'),
                    onClick: () => openEdit(record),
                  },
                  {
                    key: 'command',
                    label: '下发指令',
                    icon: <SendOutlined />,
                    disabled: !access.hasPermission('iot.manage.device.command'),
                    onClick: () => openCommand(record),
                  },
                  {
                    key: 'delete',
                    label: '删除',
                    icon: <DeleteOutlined />,
                    danger: true,
                    disabled: !access.hasPermission('iot.manage.device.delete'),
                    onClick: () => {
                      modal.confirm({
                        title: '确认删除设备',
                        content: `确定删除 ${record.deviceCode} 吗？`,
                        onOk: async () => {
                          await deleteIotDevice(record.id);
                          actionRef.current?.reload();
                        },
                      });
                    },
                  },
                ]}
              />
            ),
          },
        ]}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.device.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
          <Button
            key="batch"
            icon={<UploadOutlined />}
            disabled={!access.hasPermission('iot.manage.device.create')}
            onClick={() => setBatchOpen(true)}
          >
            批量创建
          </Button>,
          <Button
            key="command"
            icon={<SendOutlined />}
            disabled={
              !selectedRows.length ||
              selectedRowProductIds.length > 1 ||
              !access.hasPermission('iot.manage.device.command')
            }
            onClick={() => openCommand()}
          >
            批量下发指令
          </Button>,
        ]}
        request={async (params) => {
          const response = await getIotDevicePage({
            deviceCode: params.deviceCode || '',
            name: params.name || '',
            productId: params.productId || '',
            disabled: params.disabled,
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

      <Modal title={editingId ? '修改设备' : '新增设备'} open={modalOpen} forceRender confirmLoading={submitting} onOk={() => void submitDevice()} onCancel={() => setModalOpen(false)} destroyOnHidden>
        <Form form={deviceForm} layout="vertical">
          <Form.Item label="设备编号" name="deviceCode" rules={[{ required: true, message: '请输入设备编号' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="设备名称" name="name">
            <Input />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
            <Form.Item label="IMEI" name="imei">
              <Input />
            </Form.Item>
            <Form.Item label="ICCID" name="iccid">
              <Input />
            </Form.Item>
            <Form.Item label="所属产品" name="productId" rules={[{ required: true, message: '请选择所属产品' }]}>
              <Select options={products.map((item) => ({ label: item.name, value: item.id }))} />
            </Form.Item>
            <Form.Item label="设备密钥" name="secretKey">
              <Input />
            </Form.Item>
          </div>
          <Form.Item label="状态" name="disabled">
            <Radio.Group options={[{ label: '启用', value: false }, { label: '禁用', value: true }]} />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="批量创建设备" open={batchOpen} onOk={() => void submitBatch()} onCancel={() => setBatchOpen(false)} destroyOnHidden>
        <Form form={batchForm} layout="vertical" initialValues={{ disabled: false }}>
          <Form.Item label="所属产品" name="productId" rules={[{ required: true, message: '请选择所属产品' }]}>
            <Select options={products.map((item) => ({ label: item.name, value: item.id }))} />
          </Form.Item>
          <Form.Item label="设备编号列表" name="deviceCodes" rules={[{ required: true, message: '请输入设备编号列表' }]}>
            <Input.TextArea rows={6} placeholder="一行一个，或用逗号/分号分隔" />
          </Form.Item>
          <Form.Item label="状态" name="disabled">
            <Radio.Group options={[{ label: '启用', value: false }, { label: '禁用', value: true }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="下发设备指令" open={commandOpen} onOk={() => void submitCommand()} onCancel={() => setCommandOpen(false)} destroyOnHidden>
        <Form form={commandForm} layout="vertical">
          <Form.Item name="deviceIds" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="productId" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="设备数量" shouldUpdate noStyle>
            <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="目标设备">{commandForm.getFieldValue('deviceIds')?.length || 0} 台</Descriptions.Item>
              <Descriptions.Item label="所属产品">{selectedCommandProduct?.name || '-'}</Descriptions.Item>
            </Descriptions>
          </Form.Item>
          <Form.Item label="服务指令" name="serviceIdentifier" rules={[{ required: true, message: '请选择服务指令' }]}>
            <Select
              onChange={() => commandForm.setFieldValue('params', {})}
              options={thingServices.map((item) => ({
                label: `${item.name} (${item.identifier})`,
                value: item.identifier,
              }))}
            />
          </Form.Item>
          {selectedService?.inputParams?.map((param) => renderParamField(param))}
          <Form.Item label="到期时间" name="deadlineAt">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="需要回执" name="replyRequired" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default DevicePage;
