import { SendOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess, useParams } from '@umijs/max';
import dayjs from 'dayjs';
import {
  App,
  Button,
  Card,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import { createIotCommand } from '@/services/budiot/iot/command';
import {
  getIotDeviceCommandLogs,
  getIotDeviceDataLogs,
  getIotDeviceDetail,
  getIotDeviceEventLogs,
  getIotDevicePendingCommands,
  getIotDeviceRawLogs,
} from '@/services/budiot/iot/device';
import type {
  IotCommandRecord,
  IotDeviceDataLogRecord,
  IotDeviceDetail,
  IotDeviceEventLogRecord,
  IotDeviceLatestProperty,
  IotDeviceRawLogRecord,
  IotThingEvent,
  IotThingProperty,
  IotThingService,
  IotThingServiceParam,
} from '@/services/budiot/iot/typing';
import {
  formatDateTime,
  resolveIotOptionValue,
  safeJsonParse,
  safeJsonStringify,
} from '../shared';

type TabKey = 'basic' | 'raw' | 'data' | 'event' | 'command';
type CommandTabKey = 'pending' | 'history';

type ManualCommandValues = {
  serviceIdentifier?: string;
  params?: Record<string, unknown>;
  replyRequired?: boolean;
  deadlineAt?: dayjs.Dayjs | null;
};

type DetailTableParams = {
  current?: number;
  pageSize?: number;
  startAt?: number;
  endAt?: number;
  timeRange?: [string, string];
};

const prettyJson = (value?: string) => {
  if (!value) {
    return '-';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

const formatValueJson = (valueJson?: string) => {
  if (!valueJson) {
    return '-';
  }
  try {
    const value = JSON.parse(valueJson) as unknown;
    if (value === null || value === undefined || value === '') {
      return '-';
    }
    if (typeof value === 'boolean') {
      return value ? '是' : '否';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value);
  } catch {
    return valueJson;
  }
};

const renderJsonBlock = (value?: string) => {
  const text = prettyJson(value);
  return (
    <Typography.Paragraph
      style={{ marginBottom: 0, whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}
      copyable={text !== '-' ? { text } : false}
    >
      {text}
    </Typography.Paragraph>
  );
};

const parseThingProperties = (detail?: IotDeviceDetail) =>
  safeJsonParse<IotThingProperty[]>(
    detail?.device?.product?.thingPropertyJson,
    [],
  );

const parseThingServices = (detail?: IotDeviceDetail) =>
  safeJsonParse<IotThingService[]>(
    detail?.device?.product?.thingServiceJson,
    [],
  );

const parseThingEvents = (detail?: IotDeviceDetail) =>
  safeJsonParse<IotThingEvent[]>(
    detail?.device?.product?.thingEventJson,
    [],
  );

const buildTimeRangeSearchColumn = <T extends object>(): ProColumns<T> => ({
  title: '时间范围',
  dataIndex: 'timeRange',
  valueType: 'dateTimeRange',
  hideInTable: true,
  fieldProps: {
    showTime: true,
  },
  search: {
    transform: (value) => ({
      startAt: value?.[0] ? dayjs(value[0]).valueOf() : undefined,
      endAt: value?.[1] ? dayjs(value[1]).valueOf() : undefined,
    }),
  },
});

const formatDataPointValue = (valueJson?: string, unit?: string) => {
  const value = formatValueJson(valueJson);
  if (value === '-') {
    return '-';
  }
  return unit ? `${value} ${unit}` : value;
};

const DeviceDetailPage: React.FC = () => {
  const access = useAccess();
  const { message } = App.useApp();
  const params = useParams<{ id: string }>();
  const deviceId = params.id || '';
  const [commandForm] = Form.useForm<ManualCommandValues>();
  const [detail, setDetail] = React.useState<IotDeviceDetail>();
  const [loading, setLoading] = React.useState(true);
  const [activeTab, setActiveTab] = React.useState<TabKey>('basic');
  const [activeCommandTab, setActiveCommandTab] =
    React.useState<CommandTabKey>('pending');
  const [commandOpen, setCommandOpen] = React.useState(false);
  const [submittingCommand, setSubmittingCommand] = React.useState(false);
  const rawActionRef = React.useRef<ActionType>(null);
  const dataActionRef = React.useRef<ActionType>(null);
  const eventActionRef = React.useRef<ActionType>(null);
  const pendingActionRef = React.useRef<ActionType>(null);
  const commandActionRef = React.useRef<ActionType>(null);

  const thingProperties = React.useMemo(() => parseThingProperties(detail), [detail]);
  const thingServices = React.useMemo(() => parseThingServices(detail), [detail]);
  const thingEvents = React.useMemo(() => parseThingEvents(detail), [detail]);
  const propertyMap = React.useMemo(
    () =>
      Object.fromEntries(
        thingProperties.map((item) => [item.identifier, item]),
      ) as Record<string, IotThingProperty>,
    [thingProperties],
  );
  const eventMap = React.useMemo(
    () =>
      Object.fromEntries(
        thingEvents.map((item) => [item.identifier, item]),
      ) as Record<string, IotThingEvent>,
    [thingEvents],
  );
  const serviceMap = React.useMemo(
    () =>
      Object.fromEntries(
        thingServices.map((item) => [item.identifier, item]),
      ) as Record<string, IotThingService>,
    [thingServices],
  );

  const selectedServiceIdentifier = Form.useWatch('serviceIdentifier', commandForm);
  const selectedService = React.useMemo(
    () =>
      thingServices.find((item) => item.identifier === selectedServiceIdentifier) ||
      thingServices[0],
    [selectedServiceIdentifier, thingServices],
  );

  const loadDetail = React.useCallback(async () => {
    if (!deviceId) {
      return;
    }
    setLoading(true);
    try {
      const response = await getIotDeviceDetail(deviceId);
      setDetail(response.data);
    } finally {
      setLoading(false);
    }
  }, [deviceId]);

  React.useEffect(() => {
    void loadDetail();
  }, [loadDetail]);

  React.useEffect(() => {
    if (commandOpen) {
      commandForm.setFieldsValue({
        serviceIdentifier: thingServices[0]?.identifier,
        params: {},
        replyRequired: false,
        deadlineAt: null,
      });
    }
  }, [commandForm, commandOpen, thingServices]);

  const openCommandModal = () => {
    if (!thingServices.length) {
      message.warning('当前产品未配置可下发服务');
      return;
    }
    setCommandOpen(true);
  };

  const submitCommand = async () => {
    if (!detail?.device?.id || !selectedService) {
      return;
    }
    const values = await commandForm.validateFields();
    const payload = { ...((values.params || {}) as Record<string, unknown>) };
    selectedService.inputParams?.forEach((param) => {
      if (param.dataType === 'object') {
        const currentValue = payload[param.identifier];
        if (typeof currentValue === 'string' && currentValue.trim()) {
          payload[param.identifier] = JSON.parse(currentValue);
        }
      }
    });
    setSubmittingCommand(true);
    try {
      await createIotCommand({
        deviceIds: [detail.device.id],
        productId: detail.device.productId,
        commandCode: selectedService.identifier,
        payloadJson: safeJsonStringify(payload),
        replyRequired: values.replyRequired,
        deadlineAt: values.deadlineAt ? values.deadlineAt.valueOf() : undefined,
      });
      setCommandOpen(false);
      pendingActionRef.current?.reload();
      commandActionRef.current?.reload();
    } finally {
      setSubmittingCommand(false);
    }
  };

  const latestProperties = detail?.latestProperties || [];

  const latestPropertyColumns: ColumnsType<IotDeviceLatestProperty> = [
    { title: '标识符', dataIndex: 'identifier', width: 160 },
    { title: '名称', dataIndex: 'name', width: 160 },
    {
      title: '数值',
      dataIndex: 'valueJson',
      render: (_, record) => formatValueJson(record.valueJson),
    },
    { title: '单位', dataIndex: 'unit', width: 100 },
    {
      title: '设备时间',
      dataIndex: 'deviceAt',
      width: 180,
      render: (value) => formatDateTime(value as number | undefined),
    },
  ];

  const rawColumns: ProColumns<IotDeviceRawLogRecord>[] = [
    buildTimeRangeSearchColumn<IotDeviceRawLogRecord>(),
    {
      title: '方向',
      dataIndex: 'direction',
      search: false,
      width: 90,
      render: (_, record) =>
        resolveIotOptionValue(record.direction) === 'U' ? (
          <Tag color="processing">上行</Tag>
        ) : resolveIotOptionValue(record.direction) === 'D' ? (
          <Tag color="success">下行</Tag>
        ) : (
          <Tag>-</Tag>
        ),
    },
    {
      title: '消息类型',
      dataIndex: 'messageType',
      search: false,
      width: 120,
      render: (_, record) => resolveIotOptionValue(record.messageType) || '-',
    },
    {
      title: '通信时间',
      dataIndex: 'deviceAt',
      search: false,
      width: 180,
      render: (value, record) => formatDateTime((value as number) || record.createdAt),
    },
    {
      title: '原始报文',
      dataIndex: 'payload',
      search: false,
      render: (value) =>
        value ? (
          <Typography.Text copyable={{ text: String(value) }}>{String(value)}</Typography.Text>
        ) : (
          '-'
        ),
    },
    {
      title: '解析后 JSON',
      dataIndex: 'parsedJson',
      search: false,
      render: (value) => {
        const text = typeof value === 'string' ? value.trim() : '';
        return text ? (
          <Typography.Text copyable={{ text }}>{text}</Typography.Text>
        ) : (
          '-'
        );
      },
    },
  ];

  const dataColumnKeys = React.useMemo(() => {
    return thingProperties
      .map((item) => item.identifier)
      .filter((item): item is string => Boolean(item));
  }, [thingProperties]);

  const dataColumns: ProColumns<IotDeviceDataLogRecord>[] = React.useMemo(
    () => [
      buildTimeRangeSearchColumn<IotDeviceDataLogRecord>(),
      {
        title: '通信时间',
        dataIndex: 'deviceAt',
        search: false,
        width: 180,
        render: (value, record) => formatDateTime((value as number) || record.createdAt),
      },
      ...dataColumnKeys.map<ProColumns<IotDeviceDataLogRecord>>((identifier) => ({
        title: propertyMap[identifier]?.name || identifier,
        key: identifier,
        search: false,
        width: 160,
        render: (_, record) => {
          const point = record.properties?.[identifier];
          return formatDataPointValue(
            point?.valueJson,
            point?.unit || propertyMap[identifier]?.unit,
          );
        },
      })),
    ],
    [dataColumnKeys, propertyMap],
  );

  const eventColumns: ProColumns<IotDeviceEventLogRecord>[] = [
    buildTimeRangeSearchColumn<IotDeviceEventLogRecord>(),
    {
      title: '通信时间',
      dataIndex: 'deviceAt',
      search: false,
      width: 180,
      render: (value, record) => formatDateTime((value as number) || record.createdAt),
    },
    { title: '事件标识', dataIndex: 'eventCode', width: 160, search: false },
    {
      title: '事件名称',
      dataIndex: 'eventName',
      search: false,
      width: 160,
      render: (_, record) =>
        eventMap[record.eventCode || '']?.name || record.eventName || '-',
    },
    {
      title: '级别',
      dataIndex: 'level',
      search: false,
      width: 100,
      render: (_, record) => resolveIotOptionValue(record.level) || '-',
    },
    {
      title: '内容',
      dataIndex: 'contentJson',
      search: false,
      render: (value) => {
        const text = typeof value === 'string' ? value : '';
        return text ? (
          <Typography.Text copyable={{ text }}>{text}</Typography.Text>
        ) : (
          '-'
        );
      },
    },
  ];

  const pendingCommandColumns: ProColumns<IotCommandRecord>[] = [
    buildTimeRangeSearchColumn<IotCommandRecord>(),
    {
      title: '指令',
      dataIndex: 'commandCode',
      search: false,
      width: 180,
      render: (_, record) =>
        serviceMap[record.commandCode || '']?.name || record.commandCode || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      search: false,
      width: 120,
      render: (_, record) => <Tag color="processing">{resolveIotOptionValue(record.status) || '-'}</Tag>,
    },
    {
      title: '指令参数',
      dataIndex: 'payloadJson',
      search: false,
      render: (value) => {
        const text = typeof value === 'string' ? value : '';
        return text ? (
          <Typography.Text copyable={{ text }}>{text}</Typography.Text>
        ) : (
          '-'
        );
        },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      search: false,
      width: 200,
      render: (value, record) =>
        formatDateTime((value as number | undefined) || record.createdAt),
    },
    {
      title: '到期时间',
      dataIndex: 'deadlineAt',
      search: false,
      width: 200,
      render: (value) => formatDateTime(value as number | undefined),
    },
  ];

  const historyCommandColumns: ProColumns<IotCommandRecord>[] = [
    buildTimeRangeSearchColumn<IotCommandRecord>(),
    {
      title: '指令',
      dataIndex: 'commandCode',
      search: false,
      width: 180,
      render: (_, record) =>
        serviceMap[record.commandCode || '']?.name || record.commandCode || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      search: false,
      width: 120,
      render: (_, record) => <Tag>{resolveIotOptionValue(record.status) || '-'}</Tag>,
    },
    {
      title: '指令参数',
      dataIndex: 'payloadJson',
      search: false,
      render: (value) => renderJsonBlock(typeof value === 'string' ? value : ''),
    },
    {
      title: '回执',
      dataIndex: 'responseJson',
      search: false,
      render: (value) => renderJsonBlock(typeof value === 'string' ? value : ''),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      search: false,
      width: 180,
      render: (value) => formatDateTime(value as number | undefined),
    },
    {
      title: '完成时间',
      dataIndex: 'finishedAt',
      search: false,
      width: 180,
      render: (value, record) => formatDateTime((value as number) || record.replyAt),
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      search: false,
      width: 200,
    },
  ];

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
    <PageContainer
      title={detail?.device?.deviceCode || '设备详情'}
      onBack={() => history.push('/platform/iot/device')}
    >
      <Spin spinning={loading}>
        {detail ? (
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as TabKey)}
            destroyOnHidden
            items={[
              {
                key: 'basic',
                label: '基本信息',
                children: (
                  <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                    <Card>
                      <Descriptions bordered size="small" column={3}>
                        <Descriptions.Item label="所属产品">
                          {detail.device.product?.name || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="设备编号">
                          {detail.device.deviceCode}
                        </Descriptions.Item>
                        <Descriptions.Item label="设备名称">
                          {detail.device.deviceName || detail.device.name || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="在线状态">
                          {detail.device.online ? (
                            <Tag color="success">在线</Tag>
                          ) : (
                            <Tag>离线</Tag>
                          )}
                        </Descriptions.Item>
                        <Descriptions.Item label="最后通信时间">
                          {formatDateTime(detail.device.lastHeartbeatAt)}
                        </Descriptions.Item>
                        <Descriptions.Item label="设备时间">
                          {formatDateTime(detail.device.lastDeviceAt)}
                        </Descriptions.Item>
                        <Descriptions.Item label="ICCID">
                          {detail.device.iccid || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="IMEI">
                          {detail.device.imei || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="IP">{detail.device.ip || '-'}</Descriptions.Item>
                        <Descriptions.Item label="网关节点">
                          {detail.device.gatewayNodeId || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="设备密钥">
                          {detail.device.secretKey || '-'}
                        </Descriptions.Item>
                        <Descriptions.Item label="描述">
                          {detail.device.description || '-'}
                        </Descriptions.Item>
                      </Descriptions>
                    </Card>
                    <Card title="最后一条上报数据">
                      <Table<IotDeviceLatestProperty>
                        rowKey="identifier"
                        size="small"
                        pagination={false}
                        columns={latestPropertyColumns}
                        dataSource={latestProperties}
                        locale={{ emptyText: <Empty description="暂无上报数据" /> }}
                      />
                    </Card>
                  </Space>
                ),
              },
              {
                key: 'raw',
                label: '通信报文',
                children: (
                  <PlatformProTable<IotDeviceRawLogRecord, DetailTableParams>
                    persistenceKey="platform-iot-device-detail-raw-table"
                    actionRef={rawActionRef}
                    rowKey="id"
                    size="small"
                    search={{ defaultCollapsed: false }}
                    headerTitle="通信报文"
                    columns={rawColumns}
                    request={async (tableParams) => {
                      const response = await getIotDeviceRawLogs({
                        deviceId,
                        startAt: tableParams.startAt,
                        endAt: tableParams.endAt,
                        pageNo: tableParams.current || 1,
                        pageSize: tableParams.pageSize || 10,
                      });
                      return {
                        data: response.data.list || [],
                        total: response.data.totalCount || 0,
                        success: true,
                      };
                    }}
                  />
                ),
              },
              {
                key: 'data',
                label: '上报数据',
                children: (
                  <PlatformProTable<IotDeviceDataLogRecord, DetailTableParams>
                    persistenceKey="platform-iot-device-detail-data-table"
                    actionRef={dataActionRef}
                    rowKey="id"
                    size="small"
                    search={{ defaultCollapsed: false }}
                    headerTitle="上报数据"
                    columns={dataColumns}
                    scroll={{ x: 'max-content' }}
                    request={async (tableParams) => {
                      const response = await getIotDeviceDataLogs({
                        deviceId,
                        startAt: tableParams.startAt,
                        endAt: tableParams.endAt,
                        pageNo: tableParams.current || 1,
                        pageSize: tableParams.pageSize || 10,
                      });
                      return {
                        data: response.data.list || [],
                        total: response.data.totalCount || 0,
                        success: true,
                      };
                    }}
                  />
                ),
              },
              {
                key: 'event',
                label: '事件数据',
                children: (
                  <PlatformProTable<IotDeviceEventLogRecord, DetailTableParams>
                    persistenceKey="platform-iot-device-detail-event-table"
                    actionRef={eventActionRef}
                    rowKey="id"
                    size="small"
                    search={{ defaultCollapsed: false }}
                    headerTitle="事件数据"
                    columns={eventColumns}
                    request={async (tableParams) => {
                      const response = await getIotDeviceEventLogs({
                        deviceId,
                        startAt: tableParams.startAt,
                        endAt: tableParams.endAt,
                        pageNo: tableParams.current || 1,
                        pageSize: tableParams.pageSize || 10,
                      });
                      return {
                        data: response.data.list || [],
                        total: response.data.totalCount || 0,
                        success: true,
                      };
                    }}
                  />
                ),
              },
              {
                key: 'command',
                label: '指令下发',
                children: (
                  <Tabs
                    activeKey={activeCommandTab}
                    onChange={(key) => setActiveCommandTab(key as CommandTabKey)}
                    destroyOnHidden
                    items={[
                      {
                        key: 'pending',
                        label: '待下发指令',
                        children: (
                          <PlatformProTable<IotCommandRecord, DetailTableParams>
                            persistenceKey="platform-iot-device-detail-pending-command-table"
                            actionRef={pendingActionRef}
                            rowKey="id"
                            size="small"
                            search={{ defaultCollapsed: false }}
                            headerTitle="待下发指令"
                            columns={pendingCommandColumns}
                            toolBarRender={() =>
                              access.hasPermission('iot.manage.device.command')
                                ? [
                                    <Button
                                      key="send"
                                      type="primary"
                                      icon={<SendOutlined />}
                                      onClick={openCommandModal}
                                    >
                                      下发指令
                                    </Button>,
                                  ]
                                : []
                            }
                            request={async (tableParams) => {
                              const response = await getIotDevicePendingCommands({
                                deviceId,
                                startAt: tableParams.startAt,
                                endAt: tableParams.endAt,
                                pageNo: tableParams.current || 1,
                                pageSize: tableParams.pageSize || 10,
                              });
                              return {
                                data: response.data.list || [],
                                total: response.data.totalCount || 0,
                                success: true,
                              };
                            }}
                          />
                        ),
                      },
                      {
                        key: 'history',
                        label: '历史指令',
                        children: (
                          <PlatformProTable<IotCommandRecord, DetailTableParams>
                            persistenceKey="platform-iot-device-detail-history-command-table"
                            actionRef={commandActionRef}
                            rowKey="id"
                            size="small"
                            search={{ defaultCollapsed: false }}
                            headerTitle="历史指令"
                            columns={historyCommandColumns}
                            request={async (tableParams) => {
                              const response = await getIotDeviceCommandLogs({
                                deviceId,
                                startAt: tableParams.startAt,
                                endAt: tableParams.endAt,
                                pageNo: tableParams.current || 1,
                                pageSize: tableParams.pageSize || 10,
                              });
                              return {
                                data: response.data.list || [],
                                total: response.data.totalCount || 0,
                                success: true,
                              };
                            }}
                          />
                        ),
                      },
                    ]}
                  />
                ),
              },
            ]}
          />
        ) : (
          <Empty description="未找到设备信息" />
        )}
      </Spin>

      <Modal
        title="下发设备指令"
        open={commandOpen}
        onOk={() => void submitCommand()}
        onCancel={() => setCommandOpen(false)}
        confirmLoading={submittingCommand}
        destroyOnHidden
      >
        <Form form={commandForm} layout="vertical">
          <Form.Item
            label="服务指令"
            name="serviceIdentifier"
            rules={[{ required: true, message: '请选择服务指令' }]}
          >
            <Select
              onChange={() => commandForm.setFieldValue('params', {})}
              options={thingServices.map((item) => ({
                label: `${item.name} (${item.identifier})`,
                value: item.identifier,
              }))}
            />
          </Form.Item>
          {selectedService?.inputParams?.map((param) => renderParamField(param))}
          <Form.Item label="需要回执" name="replyRequired" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item label="到期时间" name="deadlineAt">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default DeviceDetailPage;
