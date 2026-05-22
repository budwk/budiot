import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  UnorderedListOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Radio,
  Select,
  Space,
  Steps,
  Switch,
  Table,
  Tabs,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createIotProduct,
  deleteIotProduct,
  getIotProductDetail,
  getIotProductMeta,
  getIotProductThingModel,
  getIotProductPage,
  updateIotProduct,
  updateIotProductThingModel,
} from '@/services/budiot/iot/product';
import type {
  IotProductMeta,
  IotProductRecord,
  IotProductThingModelRecord,
  IotThingEvent,
  IotThingProperty,
  IotThingService,
  IotThingServiceParam,
} from '@/services/budiot/iot/typing';
import {
  formatDateTime,
  resolveIotOptionText,
  resolveIotOptionValue,
  safeJsonParse,
  toIotTreeSelectData,
  toSelectOptions,
} from '../shared';
import { buildTree } from '@/utils/tree';

type ProductFormValues = {
  id?: string;
  productKey: string;
  name: string;
  categoryId?: string;
  vendorId?: string;
  productType: string;
  networkProtocol: string;
  protocolId?: string;
  gatewayNodeId?: string;
  gatewayPort?: number;
  description?: string;
  disabled: boolean;
};

type ProductQueryValues = {
  name?: string;
  productKey?: string;
  productType?: string;
  networkProtocol?: string;
};

type ProductViewMode = 'table' | 'card';
type ProductTableParams = ProductQueryValues & { current?: number; pageSize?: number };

type ThingServiceItem = IotThingService & {
  inputParams: IotThingServiceParam[];
};

type ThingEventItem = IotThingEvent & {
  outputParams: IotThingServiceParam[];
};

const defaultValues: ProductFormValues = {
  productKey: '',
  name: '',
  categoryId: '',
  vendorId: '',
  productType: '',
  networkProtocol: '',
  protocolId: '',
  gatewayNodeId: '',
  gatewayPort: undefined,
  description: '',
  disabled: false,
};

const BASIC_STEP_FIELDS: Array<Array<keyof ProductFormValues>> = [
  ['name', 'productKey', 'productType', 'networkProtocol', 'categoryId', 'vendorId'],
  ['gatewayNodeId', 'protocolId', 'gatewayPort'],
  ['description', 'disabled'],
];

const BASIC_FORM_FIELDS = BASIC_STEP_FIELDS.flat();

const thingDataTypes = [
  { label: '字符串', value: 'string' },
  { label: '整数', value: 'int' },
  { label: '浮点数', value: 'double' },
  { label: '布尔值', value: 'boolean' },
  { label: '枚举', value: 'enum' },
  { label: 'JSON对象', value: 'object' },
];

const thingUnits = [
  { label: '摄氏度 (℃)', value: '℃' },
  { label: '华氏度 (℉)', value: '℉' },
  { label: '百分比 (%)', value: '%' },
  { label: '伏特 (V)', value: 'V' },
  { label: '安培 (A)', value: 'A' },
  { label: '瓦特 (W)', value: 'W' },
  { label: '千瓦时 (kWh)', value: 'kWh' },
  { label: '帕斯卡 (Pa)', value: 'Pa' },
  { label: '米 (m)', value: 'm' },
  { label: '立方米 (m3)', value: 'm3' },
  { label: '米/秒 (m/s)', value: 'm/s' },
  { label: 'ppm', value: 'ppm' },
  { label: '毫克/升 (mg/L)', value: 'mg/L' },
];

const serviceCallTypes = [
  { label: '异步调用', value: 'async' },
  { label: '同步调用', value: 'sync' },
  { label: '单向调用', value: 'oneway' },
];

const eventLevels = [
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '严重', value: 'critical' },
];

const createThingProperty = (): IotThingProperty => ({
  identifier: '',
  name: '',
  dataType: 'string',
  unit: '',
  description: '',
});

const createThingParam = (): IotThingServiceParam => ({
  identifier: '',
  name: '',
  required: false,
  dataType: 'string',
});

const createThingService = (): ThingServiceItem => ({
  identifier: '',
  name: '',
  callType: 'async',
  inputParams: [],
  outputDataType: 'string',
  description: '',
});

const createThingEvent = (): ThingEventItem => ({
  identifier: '',
  name: '',
  level: 'info',
  outputParams: [],
  description: '',
});

const trimObjectStrings = <T extends object>(value: T): T =>
  Object.fromEntries(
    Object.entries(value).map(([key, item]) => [
      key,
      typeof item === 'string' ? item.trim() : item,
    ]),
  ) as T;

const parseThingPropertyList = (raw?: string) =>
  safeJsonParse<IotThingProperty[]>(raw, []).map((item) => ({
    ...createThingProperty(),
    ...item,
  }));

const parseThingServiceList = (raw?: string): ThingServiceItem[] =>
  safeJsonParse<IotThingService[]>(raw, []).map((item) => ({
    ...createThingService(),
    ...item,
    inputParams: Array.isArray(item.inputParams)
      ? item.inputParams.map((param) => ({
          ...createThingParam(),
          ...param,
        }))
      : [],
  }));

const parseThingEventList = (raw?: string): ThingEventItem[] =>
  safeJsonParse<IotThingEvent[]>(raw, []).map((item) => ({
    ...createThingEvent(),
    ...item,
    outputParams: Array.isArray(item.outputParams)
      ? item.outputParams.map((param) => ({
          ...createThingParam(),
          ...param,
        }))
      : [],
  }));

const normalizeThingPropertyList = (list: IotThingProperty[]) =>
  list
    .map((item) => trimObjectStrings(item))
    .filter((item) => item.identifier || item.name);

const normalizeThingServiceList = (list: ThingServiceItem[]) =>
  list
    .map((item): ThingServiceItem => ({
      ...trimObjectStrings(item),
      inputParams: (item.inputParams || [])
        .map((param): IotThingServiceParam => trimObjectStrings(param))
        .filter((param) => param.identifier || param.name),
    }))
    .filter((item) => item.identifier || item.name);

const normalizeThingEventList = (list: ThingEventItem[]) =>
  list
    .map((item): ThingEventItem => ({
      ...trimObjectStrings(item),
      outputParams: (item.outputParams || [])
        .map((param): IotThingServiceParam => trimObjectStrings(param))
        .filter((param) => param.identifier || param.name),
    }))
    .filter((item) => item.identifier || item.name);

const normalizeNullableString = (value?: string) => {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
};

const buildProductPayload = (
  source: Partial<IotProductRecord> | undefined,
  overrides: Partial<ProductFormValues> = {},
) => ({
  id: source?.id,
  productKey: overrides.productKey ?? source?.productKey ?? '',
  name: overrides.name ?? source?.name ?? '',
  categoryId: normalizeNullableString(overrides.categoryId ?? source?.categoryId ?? ''),
  vendorId: normalizeNullableString(overrides.vendorId ?? source?.vendorId ?? ''),
  productType: normalizeNullableString(
    overrides.productType ?? resolveIotOptionValue(source?.productType),
  ),
  networkProtocol: normalizeNullableString(
    overrides.networkProtocol ?? resolveIotOptionValue(source?.networkProtocol),
  ),
  protocolId: normalizeNullableString(overrides.protocolId ?? source?.protocolId ?? ''),
  gatewayNodeId: normalizeNullableString(
    overrides.gatewayNodeId ?? source?.gatewayNodeId ?? '',
  ),
  gatewayPort: overrides.gatewayPort ?? source?.gatewayPort,
  description: overrides.description ?? source?.description ?? '',
  disabled: overrides.disabled ?? source?.disabled ?? false,
});

const buildThingModelPayload = (
  source: Partial<IotProductThingModelRecord> | undefined,
  overrides: Partial<IotProductThingModelRecord> = {},
) => ({
  id: overrides.id ?? source?.id,
  thingPropertyJson: overrides.thingPropertyJson ?? source?.thingPropertyJson ?? JSON.stringify([]),
  thingServiceJson: overrides.thingServiceJson ?? source?.thingServiceJson ?? JSON.stringify([]),
  thingEventJson: overrides.thingEventJson ?? source?.thingEventJson ?? JSON.stringify([]),
});

const getProductTypeTag = (type?: string) => {
  const tagMap: Record<string, string> = {
    DIRECT: 'processing',
    GATEWAY: 'success',
    GATEWAY_SUB: 'warning',
    SUBSET: 'warning',
  };
  return tagMap[type || ''] || 'default';
};

const ProductPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [basicForm] = Form.useForm<ProductFormValues>();
  const [meta, setMeta] = React.useState<IotProductMeta>({
    productTypes: [],
    networkProtocols: [],
    categories: [],
    vendors: [],
    protocols: [],
    gatewayNodes: [],
  });
  const [loading, setLoading] = React.useState(false);
  const [tableData, setTableData] = React.useState<IotProductRecord[]>([]);
  const [pageInfo, setPageInfo] = React.useState({ current: 1, pageSize: 10, total: 0 });
  const [viewMode, setViewMode] = React.useState<ProductViewMode>('table');
  const [basicModalOpen, setBasicModalOpen] = React.useState(false);
  const [thingModelOpen, setThingModelOpen] = React.useState(false);
  const [detailRecord, setDetailRecord] = React.useState<IotProductRecord>();
  const [basicStep, setBasicStep] = React.useState(0);
  const [editingId, setEditingId] = React.useState<string>();
  const [editingDetail, setEditingDetail] = React.useState<IotProductRecord>();
  const [thingModelRecord, setThingModelRecord] = React.useState<IotProductThingModelRecord>();
  const [submitting, setSubmitting] = React.useState(false);
  const [previewTab, setPreviewTab] = React.useState('base');
  const [thingProperties, setThingProperties] = React.useState<IotThingProperty[]>([]);
  const [thingServices, setThingServices] = React.useState<ThingServiceItem[]>([]);
  const [thingEvents, setThingEvents] = React.useState<ThingEventItem[]>([]);

  const currentNetworkProtocol = Form.useWatch('networkProtocol', basicForm);
  const currentGatewayNodeId = Form.useWatch('gatewayNodeId', basicForm);
  const categoryTree = React.useMemo(() => buildTree(meta.categories || []), [meta.categories]);
  const filteredGatewayNodes = React.useMemo(() => {
    if (!currentNetworkProtocol) {
      return meta.gatewayNodes;
    }
    return meta.gatewayNodes.filter(
      (item) => !item.protocol || item.protocol === currentNetworkProtocol,
    );
  }, [currentNetworkProtocol, meta.gatewayNodes]);
  const selectedGateway = React.useMemo(
    () =>
      meta.gatewayNodes.find(
        (item) =>
          (item.gatewayId || item.nodeId) === currentGatewayNodeId,
      ),
    [currentGatewayNodeId, meta.gatewayNodes],
  );
  const requestProducts = React.useCallback(async (params: ProductTableParams) => {
    const response = await getIotProductPage({
      name: params.name || '',
      productKey: params.productKey || '',
      productType: params.productType || '',
      networkProtocol: params.networkProtocol || '',
      pageNo: params.current || 1,
      pageSize: params.pageSize || 10,
      pageOrderName: 'updatedAt',
      pageOrderBy: 'descending',
    });
    setTableData(response.data.list || []);
    setPageInfo({
      current: params.current || 1,
      pageSize: params.pageSize || 10,
      total: response.data.totalCount || 0,
    });
    return {
      data: response.data.list || [],
      total: response.data.totalCount || 0,
      success: true,
    };
  }, []);

  React.useEffect(() => {
    const initialize = async () => {
      setLoading(true);
      try {
        const response = await getIotProductMeta();
        setMeta(response.data);
      } finally {
        setLoading(false);
      }
    };
    initialize().catch(() => undefined);
  }, []);

  React.useEffect(() => {
    if (!currentNetworkProtocol && !currentGatewayNodeId) {
      return;
    }
    const gateway = meta.gatewayNodes.find(
      (item) => (item.gatewayId || item.nodeId) === currentGatewayNodeId,
    );
    if (!gateway) {
      basicForm.setFieldValue('gatewayPort', undefined);
      basicForm.setFieldValue('protocolId', '');
      return;
    }
    if (gateway.protocol && currentNetworkProtocol && gateway.protocol !== currentNetworkProtocol) {
      basicForm.setFieldValue('gatewayNodeId', '');
      basicForm.setFieldValue('gatewayPort', undefined);
      basicForm.setFieldValue('protocolId', '');
      return;
    }
    basicForm.setFieldValue('gatewayPort', gateway.port || gateway.remotePort);
    basicForm.setFieldValue('protocolId', gateway.protocolId || '');
  }, [basicForm, currentGatewayNodeId, currentNetworkProtocol, meta.gatewayNodes]);

  const openCreate = () => {
    setEditingId(undefined);
    setEditingDetail(undefined);
    setBasicStep(0);
    basicForm.resetFields();
    basicForm.setFieldsValue({
      ...defaultValues,
      productType: meta.productTypes[0]?.value || '',
      networkProtocol: meta.networkProtocols[0]?.value || '',
    });
    setBasicModalOpen(true);
  };

  const openEdit = async (record: IotProductRecord) => {
    const response = await getIotProductDetail(record.id);
    setEditingDetail(response.data);
    setEditingId(record.id);
    setBasicStep(0);
    basicForm.setFieldsValue({
      id: response.data.id,
      productKey: response.data.productKey || '',
      name: response.data.name,
      categoryId: response.data.categoryId || '',
      vendorId: response.data.vendorId || '',
      productType: resolveIotOptionValue(response.data.productType),
      networkProtocol: resolveIotOptionValue(response.data.networkProtocol),
      protocolId: response.data.protocolId || '',
      gatewayNodeId: response.data.gatewayNodeId || '',
      gatewayPort: response.data.gatewayPort,
      description: response.data.description || '',
      disabled: response.data.disabled,
    });
    setBasicModalOpen(true);
  };

  const openThingModel = async (record: IotProductRecord) => {
    const response = await getIotProductThingModel(record.id);
    setThingModelRecord(response.data);
    setThingProperties(parseThingPropertyList(response.data.thingPropertyJson));
    setThingServices(parseThingServiceList(response.data.thingServiceJson));
    setThingEvents(parseThingEventList(response.data.thingEventJson));
    setThingModelOpen(true);
  };

  const openDetail = async (record: IotProductRecord) => {
    const response = await getIotProductDetail(record.id);
    setPreviewTab('base');
    setDetailRecord(response.data);
  };

  const handleNextBasicStep = async () => {
    await basicForm.validateFields(BASIC_STEP_FIELDS[basicStep]);
    setBasicStep((prev) => prev + 1);
  };

  const submitBasic = async () => {
    const values = await basicForm.validateFields(BASIC_FORM_FIELDS);
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotProduct(buildProductPayload(editingDetail, { ...values, id: editingId }));
      } else {
        await createIotProduct(buildProductPayload(undefined, values));
      }
      setBasicModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const toggleDisabled = async (record: IotProductRecord) => {
    const response = await getIotProductDetail(record.id);
    await updateIotProduct(
      buildProductPayload(response.data, {
        disabled: !response.data.disabled,
      }),
    );
    actionRef.current?.reload();
  };

  const submitThingModel = async () => {
    if (!thingModelRecord) {
      return;
    }
    setSubmitting(true);
    try {
      await updateIotProductThingModel(
        buildThingModelPayload(thingModelRecord, {
          thingPropertyJson: JSON.stringify(normalizeThingPropertyList(thingProperties)),
          thingServiceJson: JSON.stringify(normalizeThingServiceList(thingServices)),
          thingEventJson: JSON.stringify(normalizeThingEventList(thingEvents)),
        }),
      );
      setThingModelOpen(false);
        if (detailRecord?.id === thingModelRecord.id) {
          const refreshed = await getIotProductDetail(thingModelRecord.id);
          setDetailRecord(refreshed.data);
      }
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<IotProductRecord>[]>(
    () => [
      {
        title: '产品名称',
        dataIndex: 'name',
        render: (_: unknown, record: IotProductRecord) => (
          <Space>
            <Tag color="blue" variant="filled">
              产品
            </Tag>
            <span>{record.name}</span>
          </Space>
        ),
      },
      {
        title: 'ProductKey',
        dataIndex: 'productKey',
        width: 160,
        render: (_: unknown, record: IotProductRecord) => (
          <Tag>{record.productKey}</Tag>
        ),
      },
      {
        title: '产品类型',
        dataIndex: 'productType',
        width: 140,
        valueType: 'select',
        fieldProps: { options: toSelectOptions(meta.productTypes), allowClear: true },
        render: (_: unknown, record: IotProductRecord) => (
          <Tag color={getProductTypeTag(resolveIotOptionValue(record.productType))}>
            {resolveIotOptionText(meta.productTypes, record.productType)}
          </Tag>
        ),
      },
      {
        title: '网络协议',
        dataIndex: 'networkProtocol',
        width: 140,
        valueType: 'select',
        fieldProps: { options: toSelectOptions(meta.networkProtocols), allowClear: true },
        render: (_: unknown, record: IotProductRecord) =>
          resolveIotOptionText(meta.networkProtocols, record.networkProtocol),
      },
      {
        title: '分类',
        dataIndex: 'categoryId',
        width: 140,
        search: false,
        render: (_: unknown, record: IotProductRecord) => record.category?.name || '--',
      },
      {
        title: '厂商',
        dataIndex: 'vendorId',
        width: 140,
        search: false,
        render: (_: unknown, record: IotProductRecord) => record.vendor?.name || '--',
      },
      {
        title: '协议脚本',
        dataIndex: 'protocolId',
        width: 140,
        search: false,
        render: (_: unknown, record: IotProductRecord) => record.protocol?.name || '--',
      },
      {
        title: '设备数',
        dataIndex: 'deviceCount',
        width: 100,
        search: false,
      },
      {
        title: '网关节点',
        dataIndex: 'gatewayNodeId',
        width: 180,
        search: false,
        render: (_: unknown, record: IotProductRecord) =>
          `${record.gatewayName || record.gatewayNodeId || '--'}${
            record.gatewayPort ? `:${record.gatewayPort}` : ''
          }`,
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        width: 100,
        search: false,
        render: (_: unknown, record: IotProductRecord) => (
          <Switch
            checked={!record.disabled}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            disabled={!access.hasPermission('iot.manage.product.update')}
            onChange={() => void toggleDisabled(record)}
          />
        ),
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        width: 180,
        search: false,
        render: (_: unknown, record: IotProductRecord) => formatDateTime(record.updatedAt),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 220,
        render: (_: unknown, record: IotProductRecord) => (
          <TableRowActions
            actions={[
              {
                key: 'detail',
                label: '详情',
                icon: <EyeOutlined />,
                onClick: () => openDetail(record),
              },
              {
                key: 'thing-model',
                label: '物模型',
                icon: <AppstoreOutlined />,
                disabled: !access.hasPermission('iot.manage.product.update'),
                onClick: () => openThingModel(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('iot.manage.product.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('iot.manage.product.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除产品',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteIotProduct(record.id);
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

  const properties = safeJsonParse<IotThingProperty[]>(detailRecord?.thingPropertyJson, []);
  const services = safeJsonParse<IotThingService[]>(detailRecord?.thingServiceJson, []);
  const events = safeJsonParse<IotThingEvent[]>(detailRecord?.thingEventJson, []);
  const thingParamPreviewColumns = (
    withRequired = false,
  ): {
    title: string;
    dataIndex?: keyof IotThingServiceParam;
    width?: number;
    render?: (_: unknown, record: IotThingServiceParam) => React.ReactNode;
  }[] => [
    { title: '参数标识', dataIndex: 'identifier' },
    { title: '参数名称', dataIndex: 'name' },
    ...(withRequired
      ? [
          {
            title: '必填',
            width: 90,
            render: (_: unknown, record: IotThingServiceParam) =>
              record.required ? <Tag color="processing">必填</Tag> : <Tag>可选</Tag>,
          },
        ]
      : []),
    { title: '数据类型', dataIndex: 'dataType', width: 120 },
  ];

  const generateProductKey = () => {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let value = '';
    for (let index = 0; index < 16; index += 1) {
      value += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    basicForm.setFieldValue('productKey', value);
  };

  const thingParamColumns = (
    items: IotThingServiceParam[],
    update: (next: IotThingServiceParam[]) => void,
    withRequired = false,
  ) => [
    {
      title: '参数标识',
      render: (_: unknown, row: IotThingServiceParam, index: number) => (
        <Input
          value={row.identifier}
          onChange={(event) => {
            const next = [...items];
            next[index] = { ...next[index], identifier: event.target.value };
            update(next);
          }}
        />
      ),
    },
    {
      title: '参数名称',
      render: (_: unknown, row: IotThingServiceParam, index: number) => (
        <Input
          value={row.name}
          onChange={(event) => {
            const next = [...items];
            next[index] = { ...next[index], name: event.target.value };
            update(next);
          }}
        />
      ),
    },
    ...(withRequired
      ? [
          {
            title: '必填',
            width: 90,
            render: (_: unknown, row: IotThingServiceParam, index: number) => (
              <Switch
                checked={Boolean(row.required)}
                onChange={(checked) => {
                  const next = [...items];
                  next[index] = { ...next[index], required: checked };
                  update(next);
                }}
              />
            ),
          },
        ]
      : []),
    {
      title: '数据类型',
      width: 140,
      render: (_: unknown, row: IotThingServiceParam, index: number) => (
        <Select
          value={row.dataType}
          options={thingDataTypes}
          onChange={(value) => {
            const next = [...items];
            next[index] = { ...next[index], dataType: value };
            update(next);
          }}
        />
      ),
    },
    {
      title: '操作',
      width: 90,
      render: (_: unknown, __: IotThingServiceParam, index: number) => (
        <Button
          type="link"
          danger
          onClick={() => {
            const next = [...items];
            next.splice(index, 1);
            update(next);
          }}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <PageContainer title="产品管理" loading={loading}>
      <PlatformProTable<IotProductRecord, ProductTableParams>
        persistenceKey="platform-iot-product-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="产品列表"
        columns={columns}
        request={requestProducts}
        search={{
          defaultCollapsed: false,
        }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.product.create')}
            onClick={openCreate}
          >
            新增产品
          </Button>,
          <Radio.Group
            key="view-mode"
            value={viewMode}
            onChange={(event) => setViewMode(event.target.value)}
            optionType="button"
            buttonStyle="solid"
            options={[
              {
                label: (
                  <Space size={6}>
                    <UnorderedListOutlined />
                    列表
                  </Space>
                ),
                value: 'table',
              },
              {
                label: (
                  <Space size={6}>
                    <AppstoreOutlined />
                    平铺
                  </Space>
                ),
                value: 'card',
              },
            ]}
          />,
        ]}
        tableRender={(_, defaultDom, domList) =>
          viewMode === 'table' ? (
            defaultDom
          ) : (
            <>
              {domList.toolbar}
              {domList.alert}
              <Card>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                    gap: 16,
                  }}
                >
                  {tableData.map((item) => (
                    <Card
                      key={item.id}
                      hoverable
                      styles={{ body: { padding: 18 } }}
                      actions={[
                        <Button key="detail" type="link" icon={<EyeOutlined />} onClick={() => void openDetail(item)}>
                          详情
                        </Button>,
                        <Button
                          key="thing"
                          type="link"
                          icon={<AppstoreOutlined />}
                          disabled={!access.hasPermission('iot.manage.product.update')}
                          onClick={() => void openThingModel(item)}
                        >
                          物模型
                        </Button>,
                        <Button
                          key="edit"
                          type="link"
                          icon={<EditOutlined />}
                          disabled={!access.hasPermission('iot.manage.product.update')}
                          onClick={() => void openEdit(item)}
                        >
                          编辑
                        </Button>,
                      ]}
                    >
                      <Space orientation="vertical" size={14} style={{ width: '100%' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                          <div>
                            <Typography.Title level={5} style={{ margin: 0 }}>
                              {item.name}
                            </Typography.Title>
                            <Tag style={{ marginTop: 8 }}>{item.productKey}</Tag>
                          </div>
                          <Tag color={item.disabled ? 'error' : 'success'}>
                            {item.disabled ? '已禁用' : '已启用'}
                          </Tag>
                        </div>
                        <Descriptions size="small" column={1}>
                          <Descriptions.Item label="产品类型">
                            <Tag color={getProductTypeTag(resolveIotOptionValue(item.productType))}>
                              {resolveIotOptionText(meta.productTypes, item.productType)}
                            </Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="网络协议">
                            {resolveIotOptionText(meta.networkProtocols, item.networkProtocol)}
                          </Descriptions.Item>
                          <Descriptions.Item label="产品分类">
                            {item.category?.name || '--'}
                          </Descriptions.Item>
                          <Descriptions.Item label="设备厂家">
                            {item.vendor?.name || '--'}
                          </Descriptions.Item>
                          <Descriptions.Item label="设备数">
                            <Tag color={item.deviceCount ? 'blue' : 'default'}>
                              {item.deviceCount || 0}
                            </Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="网关节点">
                            {item.gatewayName || item.gatewayNodeId || '--'}
                            {item.gatewayPort ? `:${item.gatewayPort}` : ''}
                          </Descriptions.Item>
                        </Descriptions>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <Switch
                            checked={!item.disabled}
                            checkedChildren="启用"
                            unCheckedChildren="禁用"
                            disabled={!access.hasPermission('iot.manage.product.update')}
                            onChange={() => void toggleDisabled(item)}
                          />
                          <Typography.Text type="secondary">
                            {formatDateTime(item.updatedAt)}
                          </Typography.Text>
                        </div>
                      </Space>
                    </Card>
                  ))}
                </div>
                <Pagination
                  style={{ marginTop: 16, textAlign: 'right' }}
                  current={pageInfo.current}
                  pageSize={pageInfo.pageSize}
                  total={pageInfo.total}
                  showSizeChanger
                  onChange={(current, pageSize) => {
                    actionRef.current?.setPageInfo?.({ current, pageSize });
                  }}
                />
              </Card>
            </>
          )
        }
      />

      <Modal
        title={editingId ? '修改产品' : '新增产品'}
        open={basicModalOpen}
        forceRender
        width={980}
        confirmLoading={submitting}
        onOk={() => {
          if (basicStep < 2) {
            void handleNextBasicStep();
            return;
          }
          void submitBasic();
        }}
        onCancel={() => setBasicModalOpen(false)}
        okText={basicStep < 2 ? '下一步' : '确定'}
        cancelText="取消"
        destroyOnHidden
        footer={[
          basicStep > 0 ? (
            <Button key="prev" onClick={() => setBasicStep((prev) => prev - 1)}>
              上一步
            </Button>
          ) : null,
          <Button key="cancel" onClick={() => setBasicModalOpen(false)}>
            取消
          </Button>,
          <Button key="ok" type="primary" loading={submitting} onClick={() => {
            if (basicStep < 2) {
              void handleNextBasicStep();
              return;
            }
            void submitBasic();
          }}>
            {basicStep < 2 ? '下一步' : '确定'}
          </Button>,
        ]}
      >
        <Form form={basicForm} layout="vertical">
          <Steps
            current={basicStep}
            size="small"
            style={{ marginBottom: 24 }}
            items={[
              { title: '基本信息' },
              { title: '协议配置' },
              { title: '其他设置' },
            ]}
          />
          <div
            style={{
              display: basicStep === 0 ? 'grid' : 'none',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
              <Form.Item label="产品名称" name="name" rules={[{ required: true, message: '请输入产品名称' }]}>
                <Input />
              </Form.Item>
              <Form.Item label="ProductKey">
                <Space.Compact style={{ width: '100%' }}>
                  <Form.Item name="productKey" noStyle rules={[{ required: true, message: '请输入 ProductKey' }]}>
                    <Input />
                  </Form.Item>
                  <Button onClick={generateProductKey}>生成</Button>
                </Space.Compact>
              </Form.Item>
              <Form.Item label="产品类型" name="productType" rules={[{ required: true, message: '请选择产品类型' }]}>
                <Select options={toSelectOptions(meta.productTypes)} />
              </Form.Item>
              <Form.Item label="网络协议" name="networkProtocol" rules={[{ required: true, message: '请选择网络协议' }]}>
                <Select options={toSelectOptions(meta.networkProtocols)} />
              </Form.Item>
              <Form.Item label="产品分类" name="categoryId">
                <TreeSelect
                  allowClear
                  treeDefaultExpandAll
                  treeData={toIotTreeSelectData(categoryTree)}
                />
              </Form.Item>
              <Form.Item label="设备厂家" name="vendorId">
                <Select allowClear options={meta.vendors.map((item) => ({ label: item.name, value: item.id }))} />
              </Form.Item>
          </div>

          <div
            style={{
              display: basicStep === 1 ? 'grid' : 'none',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
              <Form.Item label="设备协议">
                <Input value={selectedGateway?.protocolName || '--'} readOnly />
              </Form.Item>
              <Form.Item label="运行网关" name="gatewayNodeId">
                <Select
                  allowClear
                  placeholder="请选择运行网关"
                  options={filteredGatewayNodes.map((item) => ({
                    label: `${item.name || item.nodeName || item.gatewayName || item.nodeId || ''} (${item.gatewayMode || item.protocol || 'UNKNOWN'} ${item.port || item.remotePort || ''})`,
                    value: item.gatewayId || item.nodeId || '',
                  }))}
                />
              </Form.Item>
              <Form.Item label="协议脚本" name="protocolId">
                <Select
                  allowClear
                  options={meta.protocols.map((item) => ({ label: item.name, value: item.id }))}
                />
              </Form.Item>
              <Form.Item label="网关端口" name="gatewayPort">
                <InputNumber style={{ width: '100%' }} />
              </Form.Item>
          </div>

          <div style={{ display: basicStep === 2 ? 'block' : 'none' }}>
              <Form.Item label="说明" name="description">
                <Input.TextArea rows={4} />
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
        </Form>
      </Modal>

      <Modal
        title="物模型管理"
        open={thingModelOpen}
        forceRender
        width={1440}
        confirmLoading={submitting}
        onOk={() => void submitThingModel()}
        onCancel={() => setThingModelOpen(false)}
        destroyOnHidden
      >
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Typography.Text>
            <strong>当前产品：</strong>
            {thingModelRecord?.name || '--'}
            <span style={{ marginInline: 8 }}>|</span>
            <strong>ProductKey：</strong>
            {thingModelRecord?.productKey || '--'}
          </Typography.Text>
          <Tabs
            items={[
              {
                key: 'property',
                label: '属性',
                children: (
                  <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography.Text type="secondary">
                        定义产品的属性模型，单位采用枚举选择。
                      </Typography.Text>
                      <Button onClick={() => setThingProperties((prev) => [...prev, createThingProperty()])}>
                        新增属性
                      </Button>
                    </div>
                    {thingProperties.map((item, index) => (
                      <Card
                        key={`property-${index}`}
                        title={`属性 ${index + 1}`}
                        extra={
                          <Button
                            type="link"
                            danger
                            onClick={() =>
                              setThingProperties((prev) =>
                                prev.filter((_, currentIndex) => currentIndex !== index),
                              )
                            }
                          >
                            删除
                          </Button>
                        }
                      >
                        <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1.1fr 0.9fr 0.9fr 1.8fr', gap: 12 }}>
                          <Input
                            placeholder="标识符"
                            value={item.identifier}
                            onChange={(event) =>
                              setThingProperties((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, identifier: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="名称"
                            value={item.name}
                            onChange={(event) =>
                              setThingProperties((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, name: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Select
                            placeholder="数据类型"
                            options={thingDataTypes}
                            value={item.dataType}
                            onChange={(value) =>
                              setThingProperties((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, dataType: value } : current,
                                ),
                              )
                            }
                          />
                          <Select
                            placeholder="单位"
                            allowClear
                            showSearch
                            options={thingUnits}
                            value={item.unit}
                            onChange={(value) =>
                              setThingProperties((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, unit: value } : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="说明"
                            value={item.description}
                            onChange={(event) =>
                              setThingProperties((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, description: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                        </div>
                      </Card>
                    ))}
                  </Space>
                ),
              },
              {
                key: 'service',
                label: '服务',
                children: (
                  <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography.Text type="secondary">
                        定义设备可调用服务，输入参数使用结构化表单维护。
                      </Typography.Text>
                      <Button onClick={() => setThingServices((prev) => [...prev, createThingService()])}>
                        新增服务
                      </Button>
                    </div>
                    {thingServices.map((item, index) => (
                      <Card
                        key={`service-${index}`}
                        title={`服务 ${index + 1}`}
                        extra={
                          <Button
                            type="link"
                            danger
                            onClick={() =>
                              setThingServices((prev) =>
                                prev.filter((_, currentIndex) => currentIndex !== index),
                              )
                            }
                          >
                            删除
                          </Button>
                        }
                      >
                        <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1.1fr 0.9fr 0.9fr 1.8fr', gap: 12 }}>
                          <Input
                            placeholder="标识符"
                            value={item.identifier}
                            onChange={(event) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, identifier: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="名称"
                            value={item.name}
                            onChange={(event) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, name: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Select
                            placeholder="调用方式"
                            options={serviceCallTypes}
                            value={item.callType}
                            onChange={(value) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, callType: value } : current,
                                ),
                              )
                            }
                          />
                          <Select
                            placeholder="输出类型"
                            options={thingDataTypes}
                            value={item.outputDataType}
                            onChange={(value) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, outputDataType: value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="说明"
                            value={item.description}
                            onChange={(event) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, description: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                        </div>
                        <div style={{ marginTop: 16, marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                          <Typography.Text strong>输入参数</Typography.Text>
                          <Button
                            type="link"
                            onClick={() =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? {
                                        ...current,
                                        inputParams: [...(current.inputParams || []), createThingParam()],
                                      }
                                    : current,
                                ),
                              )
                            }
                          >
                            新增参数
                          </Button>
                        </div>
                        <Table
                          rowKey={(row) =>
                            `service-param-${index}-${row.identifier || row.name || 'param'}`
                          }
                          size="small"
                          pagination={false}
                          columns={thingParamColumns(
                            item.inputParams || [],
                            (next) =>
                              setThingServices((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, inputParams: next } : current,
                                ),
                              ),
                            true,
                          ) as any}
                          dataSource={item.inputParams || []}
                        />
                      </Card>
                    ))}
                  </Space>
                ),
              },
              {
                key: 'event',
                label: '事件',
                children: (
                  <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography.Text type="secondary">
                        定义设备主动上报事件，输出参数使用结构化表单维护。
                      </Typography.Text>
                      <Button onClick={() => setThingEvents((prev) => [...prev, createThingEvent()])}>
                        新增事件
                      </Button>
                    </div>
                    {thingEvents.map((item, index) => (
                      <Card
                        key={`event-${index}`}
                        title={`事件 ${index + 1}`}
                        extra={
                          <Button
                            type="link"
                            danger
                            onClick={() =>
                              setThingEvents((prev) =>
                                prev.filter((_, currentIndex) => currentIndex !== index),
                              )
                            }
                          >
                            删除
                          </Button>
                        }
                      >
                        <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1.1fr 0.9fr 2.4fr', gap: 12 }}>
                          <Input
                            placeholder="标识符"
                            value={item.identifier}
                            onChange={(event) =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, identifier: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="名称"
                            value={item.name}
                            onChange={(event) =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, name: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                          <Select
                            placeholder="级别"
                            options={eventLevels}
                            value={item.level}
                            onChange={(value) =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, level: value } : current,
                                ),
                              )
                            }
                          />
                          <Input
                            placeholder="说明"
                            value={item.description}
                            onChange={(event) =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? { ...current, description: event.target.value }
                                    : current,
                                ),
                              )
                            }
                          />
                        </div>
                        <div style={{ marginTop: 16, marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
                          <Typography.Text strong>输出参数</Typography.Text>
                          <Button
                            type="link"
                            onClick={() =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index
                                    ? {
                                        ...current,
                                        outputParams: [...(current.outputParams || []), createThingParam()],
                                      }
                                    : current,
                                ),
                              )
                            }
                          >
                            新增参数
                          </Button>
                        </div>
                        <Table
                          rowKey={(row) =>
                            `event-param-${index}-${row.identifier || row.name || 'param'}`
                          }
                          size="small"
                          pagination={false}
                          columns={thingParamColumns(
                            item.outputParams || [],
                            (next) =>
                              setThingEvents((prev) =>
                                prev.map((current, currentIndex) =>
                                  currentIndex === index ? { ...current, outputParams: next } : current,
                                ),
                              ),
                          ) as any}
                          dataSource={item.outputParams || []}
                        />
                      </Card>
                    ))}
                  </Space>
                ),
              },
            ]}
          />
        </Space>
      </Modal>

      <Drawer
        title={detailRecord?.name || '产品详情'}
        open={Boolean(detailRecord)}
        size="large"
        onClose={() => setDetailRecord(undefined)}
      >
        {detailRecord ? (
          <Tabs
            activeKey={previewTab}
            onChange={setPreviewTab}
            items={[
              {
                key: 'base',
                label: '基本信息',
                children: (
                  <Descriptions bordered size="small" column={2}>
                    <Descriptions.Item label="产品名称">{detailRecord.name}</Descriptions.Item>
                    <Descriptions.Item label="ProductKey">{detailRecord.productKey || '-'}</Descriptions.Item>
                    <Descriptions.Item label="产品类型">
                      {resolveIotOptionText(meta.productTypes, detailRecord.productType)}
                    </Descriptions.Item>
                    <Descriptions.Item label="网络协议">
                      {resolveIotOptionText(meta.networkProtocols, detailRecord.networkProtocol)}
                    </Descriptions.Item>
                    <Descriptions.Item label="分类">{detailRecord.category?.name || '--'}</Descriptions.Item>
                    <Descriptions.Item label="厂家">{detailRecord.vendor?.name || '--'}</Descriptions.Item>
                    <Descriptions.Item label="协议">{detailRecord.protocol?.name || '--'}</Descriptions.Item>
                    <Descriptions.Item label="网关">
                      {detailRecord.gatewayName || detailRecord.gatewayNodeId || '--'}
                      {detailRecord.gatewayPort ? `:${detailRecord.gatewayPort}` : ''}
                    </Descriptions.Item>
                    <Descriptions.Item label="状态">
                      <Tag color={detailRecord.disabled ? 'error' : 'success'}>
                        {detailRecord.disabled ? '禁用' : '启用'}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="更新时间">
                      {formatDateTime(detailRecord.updatedAt)}
                    </Descriptions.Item>
                    <Descriptions.Item label="说明" span={2}>
                      {detailRecord.description || '--'}
                    </Descriptions.Item>
                  </Descriptions>
                ),
              },
              {
                key: 'property',
                label: `属性 (${properties.length})`,
                children: (
                  <Table
                    rowKey={(item) => item.identifier}
                    size="small"
                    pagination={false}
                    columns={[
                      { title: '标识符', dataIndex: 'identifier' },
                      { title: '名称', dataIndex: 'name' },
                      { title: '数据类型', dataIndex: 'dataType' },
                      { title: '单位', dataIndex: 'unit' },
                      { title: '说明', dataIndex: 'description' },
                    ]}
                    dataSource={properties}
                  />
                ),
              },
              {
                key: 'service',
                label: `服务 (${services.length})`,
                children: (
                  <Table
                    rowKey={(item) => item.identifier}
                    size="small"
                    pagination={false}
                    expandable={{
                      rowExpandable: (record) => Boolean(record.inputParams?.length),
                      expandedRowRender: (record) => (
                        <Table<IotThingServiceParam>
                          rowKey={(item) => `${record.identifier}-${item.identifier}`}
                          size="small"
                          pagination={false}
                          columns={thingParamPreviewColumns(true) as any}
                          dataSource={record.inputParams || []}
                        />
                      ),
                    }}
                    columns={[
                      { title: '标识符', dataIndex: 'identifier' },
                      { title: '名称', dataIndex: 'name' },
                      { title: '调用方式', dataIndex: 'callType' },
                      { title: '输出类型', dataIndex: 'outputDataType' },
                      {
                        title: '输入参数',
                        render: (_: unknown, record: IotThingService) =>
                          record.inputParams?.length ? `共 ${record.inputParams.length} 项` : '--',
                      },
                      { title: '说明', dataIndex: 'description' },
                    ]}
                    dataSource={services}
                  />
                ),
              },
              {
                key: 'event',
                label: `事件 (${events.length})`,
                children: (
                  <Table
                    rowKey={(item) => item.identifier}
                    size="small"
                    pagination={false}
                    expandable={{
                      rowExpandable: (record) => Boolean(record.outputParams?.length),
                      expandedRowRender: (record) => (
                        <Table<IotThingServiceParam>
                          rowKey={(item) => `${record.identifier}-${item.identifier}`}
                          size="small"
                          pagination={false}
                          columns={thingParamPreviewColumns() as any}
                          dataSource={record.outputParams || []}
                        />
                      ),
                    }}
                    columns={[
                      { title: '标识符', dataIndex: 'identifier' },
                      { title: '名称', dataIndex: 'name' },
                      { title: '级别', dataIndex: 'level' },
                      {
                        title: '输出参数',
                        render: (_: unknown, record: IotThingEvent) =>
                          record.outputParams?.length ? `共 ${record.outputParams.length} 项` : '--',
                      },
                      { title: '说明', dataIndex: 'description' },
                    ]}
                    dataSource={events}
                  />
                ),
              },
            ]}
          />
        ) : null}
      </Drawer>
    </PageContainer>
  );
};

export default ProductPage;
