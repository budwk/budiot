import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Radio,
  Row,
  Col,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  getPubUserPage,
  getPubUserUnitTree,
} from '@/services/budiot/pub/user';
import {
  createIotRule,
  deleteIotRule,
  getIotRuleDetail,
  getIotRuleMeta,
  getIotRulePage,
  getIotRuleProductMeta,
  updateIotRule,
} from '@/services/budiot/iot/rule';
import type {
  IotDeviceRecord,
  IotRuleCondition,
  IotRuleLinkageParam,
  IotRuleMeta,
  IotRuleProductMeta,
  IotRuleRecord,
  IotThingProperty,
} from '@/services/budiot/iot/typing';
import type { SysUnitRecord, SysUserRecord } from '@/services/budiot/typing';
import {
  formatDateTime,
  resolveIotOptionText,
  resolveIotOptionValue,
  safeJsonParse,
  safeJsonStringify,
  toSelectOptions,
} from '../shared';

type RuleFormValues = {
  id?: string;
  name: string;
  code: string;
  triggerScene: string;
  sourceProductId: string;
  sourceDeviceId?: string;
  triggerIdentifier?: string;
  targetType: string;
  actionTitle?: string;
  actionContent?: string;
  messageChannelId?: string;
  notifyUserIds: string[];
  targetUrl?: string;
  targetTopic?: string;
  linkageProductId?: string;
  linkageDeviceId?: string;
  linkageServiceIdentifier?: string;
  description?: string;
  disabled: boolean;
};

type RuleConditionRow = {
  field: string;
  fieldName: string;
  dataType: string;
  operator: string;
  value: string;
};

type RuleLinkageParamRow = {
  identifier: string;
  name: string;
  dataType: string;
  value: string;
};

type RuleFieldOption = {
  value: string;
  text: string;
  dataType: string;
};

type UserPickerParams = {
  current?: number;
  pageSize?: number;
  unitId?: string;
  keywords?: string;
};

type UnitTreeNode = {
  value: string;
  title: string;
  children?: UnitTreeNode[];
};

type RulePreviewExtra = {
  sourceDeviceText?: string;
  triggerText?: string;
  conditions: Array<RuleConditionRow & { operatorText: string }>;
  notifyUsersText?: string;
  smsChannelText?: string;
  linkageText?: string;
};

const defaultValues: RuleFormValues = {
  name: '',
  code: '',
  triggerScene: '',
  sourceProductId: '',
  sourceDeviceId: '',
  triggerIdentifier: '',
  targetType: '',
  actionTitle: '',
  actionContent: '',
  messageChannelId: '',
  notifyUserIds: [],
  targetUrl: '',
  targetTopic: '',
  linkageProductId: '',
  linkageDeviceId: '',
  linkageServiceIdentifier: '',
  description: '',
  disabled: false,
};

const emptyProductMeta: IotRuleProductMeta = {
  devices: [],
  propertyFields: [],
  events: [],
  services: [],
};

const sectionCardStyle = { marginBottom: 12 };
const emptyStyle: React.CSSProperties = {
  padding: '20px 0',
  textAlign: 'center',
  border: '1px dashed #d9d9d9',
  borderRadius: 6,
  background: '#fafafa',
  color: 'rgba(0, 0, 0, 0.45)',
};
const rowStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 12,
  marginBottom: 12,
};

const toTreeData = (items: SysUnitRecord[]): UnitTreeNode[] =>
  items.map((item) => ({
    value: item.id,
    title: item.name,
    children: item.children ? toTreeData(item.children) : undefined,
  }));

const getOperatorOptions = (dataType?: string) => {
  if (dataType === 'number') {
    return [
      { label: '大于', value: 'GT' },
      { label: '等于', value: 'EQ' },
      { label: '小于', value: 'LT' },
    ];
  }
  return [{ label: '等于', value: 'EQ' }];
};

const normalizeDataType = (value?: string) => {
  const raw = String(value || '').trim().toLowerCase();
  if (['int', 'integer', 'long', 'double', 'float', 'number', 'decimal'].includes(raw)) {
    return 'number';
  }
  if (['bool', 'boolean'].includes(raw)) {
    return 'boolean';
  }
  return raw || 'string';
};

const getProductLabel = (item?: { name?: string; productKey?: string }) =>
  item?.name ? `${item.name}${item.productKey ? ` (${item.productKey})` : ''}` : '-';

const getDeviceLabel = (item?: Partial<IotDeviceRecord>) => {
  if (!item?.id) {
    return '-';
  }
  const name = item.deviceName || item.name || item.deviceCode;
  return name ? `${name}${item.deviceCode ? ` (${item.deviceCode})` : ''}` : item.deviceCode || '-';
};

const getUserEchoText = (item: {
  username?: string;
  loginname?: string;
  mobile?: string;
  email?: string;
  id: string;
}) =>
  [
    item.username || item.loginname || item.id,
    item.mobile || '-',
    item.email || '-',
  ].join(' / ');

const getChannelLabel = (item: { name: string; code?: string }) =>
  `${item.name}${item.code ? ` (${item.code})` : ''}`;

const toNotifyUserRecord = (
  item?: IotRuleMeta['tenantUsers'][number],
): SysUserRecord | undefined => {
  if (!item?.id) {
    return undefined;
  }
  return {
    id: item.id,
    username: item.username || item.name || item.loginname || '',
    loginname: item.loginname || '',
    mobile: item.mobile,
    email: item.email,
    disabled: false,
  };
};

const toConditionRows = (value?: string) =>
  safeJsonParse<IotRuleCondition[]>(value, []).map((item) => ({
    field: String(item.field || ''),
    fieldName: String(item.fieldName || ''),
    dataType: normalizeDataType(String(item.dataType || '')),
    operator:
      String(item.operator || '') ||
      getOperatorOptions(normalizeDataType(String(item.dataType || '')))[0]?.value ||
      'EQ',
    value: String(item.value ?? ''),
  }));

const toLinkageParamRows = (value?: string) =>
  safeJsonParse<IotRuleLinkageParam[]>(value, []).map((item) => ({
    identifier: String(item.identifier || ''),
    name: String(item.name || ''),
    dataType: normalizeDataType(String(item.dataType || '')),
    value: String(item.value ?? ''),
  }));

const toFieldOption = (item: Partial<IotThingProperty> & { value?: string; text?: string }) => ({
  value: String(item.value || item.identifier || ''),
  text: String(item.text || item.name || item.identifier || ''),
  dataType: normalizeDataType(item.dataType),
});

const RulePage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const hydratingRef = React.useRef(false);
  const [form] = Form.useForm<RuleFormValues>();
  const [meta, setMeta] = React.useState<IotRuleMeta>({
    triggerScenes: [],
    targetTypes: [],
    products: [],
    tenantUsers: [],
    smsChannels: [],
  });
  const [unitTree, setUnitTree] = React.useState<SysUnitRecord[]>([]);
  const [sourceMeta, setSourceMeta] = React.useState<IotRuleProductMeta>(emptyProductMeta);
  const [linkageMeta, setLinkageMeta] = React.useState<IotRuleProductMeta>(emptyProductMeta);
  const [conditionRows, setConditionRows] = React.useState<RuleConditionRow[]>([]);
  const [linkageParamRows, setLinkageParamRows] = React.useState<RuleLinkageParamRow[]>([]);
  const [notifyUserCache, setNotifyUserCache] = React.useState<Record<string, SysUserRecord>>({});
  const [pickerOpen, setPickerOpen] = React.useState(false);
  const [pickerParams, setPickerParams] = React.useState<UserPickerParams>({
    current: 1,
    pageSize: 10,
  });
  const [pickerLoading, setPickerLoading] = React.useState(false);
  const [pickerTotal, setPickerTotal] = React.useState(0);
  const [pickerRows, setPickerRows] = React.useState<SysUserRecord[]>([]);
  const [pickerSelectedRowKeys, setPickerSelectedRowKeys] = React.useState<React.Key[]>([]);
  const [pickerSelectedRows, setPickerSelectedRows] = React.useState<SysUserRecord[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [previewOpen, setPreviewOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [previewData, setPreviewData] = React.useState<IotRuleRecord>();
  const [previewExtra, setPreviewExtra] = React.useState<RulePreviewExtra>({ conditions: [] });
  const [submitting, setSubmitting] = React.useState(false);

  const sourceProductId = Form.useWatch('sourceProductId', form);
  const triggerScene = Form.useWatch('triggerScene', form);
  const triggerIdentifier = Form.useWatch('triggerIdentifier', form);
  const targetType = Form.useWatch('targetType', form);
  const linkageProductId = Form.useWatch('linkageProductId', form);
  const linkageServiceIdentifier = Form.useWatch('linkageServiceIdentifier', form);
  const notifyUserIds = Form.useWatch('notifyUserIds', form) || [];

  const mergeNotifyUsers = React.useCallback((users: SysUserRecord[]) => {
    setNotifyUserCache((current) => {
      const next = { ...current };
      users.forEach((item) => {
        if (item?.id) {
          next[item.id] = item;
        }
      });
      return next;
    });
  }, []);

  const tenantUserMap = React.useMemo(() => {
    const entries = meta.tenantUsers
      .map((item) => toNotifyUserRecord(item))
      .filter((item): item is SysUserRecord => Boolean(item))
      .map((item) => [item.id, item] as const);
    return new Map(entries);
  }, [meta.tenantUsers]);

  const selectedNotifyUsers = React.useMemo(
    () =>
      notifyUserIds
        .map((id) => notifyUserCache[id] || tenantUserMap.get(id))
        .filter((item): item is SysUserRecord => Boolean(item)),
    [notifyUserCache, notifyUserIds, tenantUserMap],
  );

  React.useEffect(() => {
    const initialize = async () => {
      const [metaResponse, unitResponse] = await Promise.all([
        getIotRuleMeta(),
        getPubUserUnitTree(),
      ]);
      setMeta(metaResponse.data);
      setUnitTree(unitResponse.data || []);
      mergeNotifyUsers(
        (metaResponse.data.tenantUsers || [])
          .map((item) => toNotifyUserRecord(item))
          .filter((item): item is SysUserRecord => Boolean(item)),
      );
    };
    void initialize();
  }, [mergeNotifyUsers]);

  const loadUsers = React.useCallback(async (nextParams: UserPickerParams) => {
    setPickerLoading(true);
    try {
      const response = await getPubUserPage({
        unitId: nextParams.unitId || '',
        keywords: nextParams.keywords || '',
        pageNo: nextParams.current || 1,
        pageSize: nextParams.pageSize || 10,
      });
      const rows = response.data.list || [];
      setPickerRows(rows);
      setPickerTotal(response.data.totalCount || 0);
      mergeNotifyUsers(rows);
    } finally {
      setPickerLoading(false);
    }
  }, [mergeNotifyUsers]);

  React.useEffect(() => {
    if (!pickerOpen) {
      return;
    }
    loadUsers(pickerParams).catch(() => undefined);
  }, [loadUsers, pickerOpen, pickerParams]);

  const currentEvent = React.useMemo(
    () =>
      (sourceMeta.events || []).find(
        (item) => item.identifier === triggerIdentifier,
      ),
    [sourceMeta.events, triggerIdentifier],
  );

  const availableConditionFields = React.useMemo<RuleFieldOption[]>(() => {
    if (triggerScene === 'EVENT') {
      return (currentEvent?.outputParams || []).map((item) =>
        toFieldOption(item as IotThingProperty & { value?: string; text?: string }),
      );
    }
    return (sourceMeta.propertyFields || []).map((item) =>
      toFieldOption(item as IotThingProperty & { value?: string; text?: string }),
    );
  }, [currentEvent?.outputParams, sourceMeta.propertyFields, triggerScene]);

  const selectedLinkageService = React.useMemo(
    () =>
      (linkageMeta.services || []).find(
        (item) => item.identifier === linkageServiceIdentifier,
      ),
    [linkageMeta.services, linkageServiceIdentifier],
  );

  React.useEffect(() => {
    if (!modalOpen) {
      return;
    }
    if (!sourceProductId) {
      setSourceMeta(emptyProductMeta);
      if (!hydratingRef.current) {
        form.setFieldsValue({ sourceDeviceId: '', triggerIdentifier: '' });
        setConditionRows([]);
      }
      return;
    }
    if (!hydratingRef.current) {
      form.setFieldsValue({ sourceDeviceId: '', triggerIdentifier: '' });
      setConditionRows([]);
    }
    let cancelled = false;
    getIotRuleProductMeta(sourceProductId)
      .then((response) => {
        if (!cancelled) {
          setSourceMeta(response.data || emptyProductMeta);
        }
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [form, modalOpen, sourceProductId]);

  React.useEffect(() => {
    if (!modalOpen || hydratingRef.current) {
      return;
    }
    if (triggerScene !== 'EVENT') {
      form.setFieldValue('triggerIdentifier', '');
    }
    setConditionRows([]);
  }, [form, modalOpen, triggerScene]);

  React.useEffect(() => {
    if (!modalOpen) {
      return;
    }
    if (!linkageProductId) {
      setLinkageMeta(emptyProductMeta);
      if (!hydratingRef.current) {
        form.setFieldsValue({
          linkageDeviceId: '',
          linkageServiceIdentifier: '',
        });
        setLinkageParamRows([]);
      }
      return;
    }
    if (!hydratingRef.current) {
      form.setFieldsValue({
        linkageDeviceId: '',
        linkageServiceIdentifier: '',
      });
      setLinkageParamRows([]);
    }
    let cancelled = false;
    getIotRuleProductMeta(linkageProductId)
      .then((response) => {
        if (!cancelled) {
          setLinkageMeta(response.data || emptyProductMeta);
        }
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [form, linkageProductId, modalOpen]);

  React.useEffect(() => {
    if (!modalOpen || hydratingRef.current) {
      return;
    }
    const nextRows = (selectedLinkageService?.inputParams || []).map((item) => ({
      identifier: item.identifier,
      name: item.name || item.identifier,
      dataType: normalizeDataType(item.dataType),
      value: '',
    }));
    setLinkageParamRows(nextRows);
  }, [modalOpen, selectedLinkageService]);

  const resetEditorState = React.useCallback(() => {
    setSourceMeta(emptyProductMeta);
    setLinkageMeta(emptyProductMeta);
    setConditionRows([]);
    setLinkageParamRows([]);
  }, []);

  const openCreate = React.useCallback(() => {
    hydratingRef.current = true;
    setEditingId(undefined);
    resetEditorState();
    form.resetFields();
    setPickerSelectedRowKeys([]);
    setPickerSelectedRows([]);
    form.setFieldsValue({
      ...defaultValues,
      triggerScene: meta.triggerScenes[0]?.value || 'NORMALIZED_UPLINK',
      targetType: meta.targetTypes[0]?.value || 'SITE_MESSAGE',
    });
    setModalOpen(true);
    queueMicrotask(() => {
      hydratingRef.current = false;
    });
  }, [form, meta.targetTypes, meta.triggerScenes, resetEditorState]);

  const openEdit = React.useCallback(
    async (record: IotRuleRecord) => {
      hydratingRef.current = true;
      setEditingId(record.id);
      resetEditorState();
      const response = await getIotRuleDetail(record.id);
      const detail = response.data;
      const [sourceResponse, linkageResponse] = await Promise.all([
        detail.sourceProductId
          ? getIotRuleProductMeta(detail.sourceProductId)
          : Promise.resolve({ data: emptyProductMeta }),
        detail.linkageProductId
          ? getIotRuleProductMeta(detail.linkageProductId)
          : Promise.resolve({ data: emptyProductMeta }),
      ]);
      setSourceMeta(sourceResponse.data || emptyProductMeta);
      setLinkageMeta(linkageResponse.data || emptyProductMeta);
      setConditionRows(toConditionRows(detail.conditionJson));
      setLinkageParamRows(toLinkageParamRows(detail.linkageParamsJson));
      form.resetFields();
      setPickerSelectedRowKeys([]);
      setPickerSelectedRows([]);
      form.setFieldsValue({
        id: detail.id,
        name: detail.name,
        code: detail.code,
        triggerScene: resolveIotOptionValue(detail.triggerScene),
        sourceProductId: detail.sourceProductId || '',
        sourceDeviceId: detail.sourceDeviceId || '',
        triggerIdentifier: detail.triggerIdentifier || '',
        targetType: resolveIotOptionValue(detail.targetType),
        actionTitle: detail.actionTitle || '',
        actionContent: detail.actionContent || '',
        messageChannelId: detail.messageChannelId || '',
        notifyUserIds: safeJsonParse<string[]>(detail.notifyUserIdsJson, []),
        targetUrl: detail.targetUrl || '',
        targetTopic: detail.targetTopic || '',
        linkageProductId: detail.linkageProductId || '',
        linkageDeviceId: detail.linkageDeviceId || '',
        linkageServiceIdentifier: detail.linkageServiceIdentifier || '',
        description: detail.description || '',
        disabled: detail.disabled,
      });
      setModalOpen(true);
      queueMicrotask(() => {
        hydratingRef.current = false;
      });
    },
    [form, resetEditorState],
  );

  const openNotifyUserPicker = React.useCallback(() => {
    setPickerSelectedRowKeys(selectedNotifyUsers.map((item) => item.id));
    setPickerSelectedRows(selectedNotifyUsers);
    setPickerOpen(true);
  }, [selectedNotifyUsers]);

  const confirmNotifyUserPicker = React.useCallback(() => {
    mergeNotifyUsers(pickerSelectedRows);
    form.setFieldValue(
      'notifyUserIds',
      Array.from(new Set([...notifyUserIds, ...pickerSelectedRows.map((item) => item.id)])),
    );
    setPickerSelectedRowKeys([]);
    setPickerSelectedRows([]);
    setPickerOpen(false);
  }, [form, mergeNotifyUsers, notifyUserIds, pickerSelectedRows]);

  const removeNotifyUser = React.useCallback((userId: string) => {
    form.setFieldValue(
      'notifyUserIds',
      notifyUserIds.filter((item) => item !== userId),
    );
    setPickerSelectedRowKeys((current) => current.filter((item) => item !== userId));
    setPickerSelectedRows((current) => current.filter((item) => item.id !== userId));
  }, [form, notifyUserIds]);

  const handleConditionFieldChange = React.useCallback(
    (index: number, field: string) => {
      const selected = availableConditionFields.find((item) => item.value === field);
      setConditionRows((current) =>
        current.map((item, itemIndex) =>
          itemIndex === index
            ? {
                ...item,
                field,
                fieldName: selected?.text || '',
                dataType: selected?.dataType || 'string',
                operator: getOperatorOptions(selected?.dataType)[0]?.value || 'EQ',
                value: '',
              }
            : item,
        ),
      );
    },
    [availableConditionFields],
  );

  const updateConditionRow = React.useCallback(
    (index: number, patch: Partial<RuleConditionRow>) => {
      setConditionRows((current) =>
        current.map((item, itemIndex) =>
          itemIndex === index ? { ...item, ...patch } : item,
        ),
      );
    },
    [],
  );

  const updateLinkageParamRow = React.useCallback(
    (index: number, value: string) => {
      setLinkageParamRows((current) =>
        current.map((item, itemIndex) =>
          itemIndex === index ? { ...item, value } : item,
        ),
      );
    },
    [],
  );

  const openPreview = React.useCallback(
    async (record: IotRuleRecord) => {
      const response = await getIotRuleDetail(record.id);
      const detail = response.data;
      const [sourceResponse, linkageResponse] = await Promise.all([
        detail.sourceProductId
          ? getIotRuleProductMeta(detail.sourceProductId)
          : Promise.resolve({ data: emptyProductMeta }),
        detail.linkageProductId
          ? getIotRuleProductMeta(detail.linkageProductId)
          : Promise.resolve({ data: emptyProductMeta }),
      ]);
      const sourceDevices = sourceResponse.data.devices || [];
      const sourceEvents = sourceResponse.data.events || [];
      const linkageDevices = linkageResponse.data.devices || [];
      const linkageServices = linkageResponse.data.services || [];
      const conditions = toConditionRows(detail.conditionJson);
      setPreviewData(detail);
      setPreviewExtra({
        sourceDeviceText:
          sourceDevices.find((item) => item.id === detail.sourceDeviceId)?.name ||
          sourceDevices.find((item) => item.id === detail.sourceDeviceId)?.deviceCode ||
          '',
        triggerText:
          sourceEvents.find((item) => item.identifier === detail.triggerIdentifier)?.name ||
          '',
        conditions: conditions.map((item) => ({
          ...item,
          operatorText:
            getOperatorOptions(item.dataType).find((op) => op.value === item.operator)?.label ||
            item.operator,
        })),
        notifyUsersText: safeJsonParse<string[]>(detail.notifyUserIdsJson, [])
          .map((id) => notifyUserCache[id] || tenantUserMap.get(id))
          .filter((item): item is SysUserRecord => Boolean(item))
          .map((item) => getUserEchoText(item))
          .join('、'),
        smsChannelText:
          meta.smsChannels.find((item) => item.id === detail.messageChannelId)?.name || '',
        linkageText: [
          meta.products.find((item) => item.id === detail.linkageProductId)
            ? getProductLabel(
                meta.products.find((item) => item.id === detail.linkageProductId),
              )
            : '',
          linkageDevices.find((item) => item.id === detail.linkageDeviceId)
            ? getDeviceLabel(
                linkageDevices.find((item) => item.id === detail.linkageDeviceId),
              )
            : '',
          linkageServices.find(
            (item) => item.identifier === detail.linkageServiceIdentifier,
          )?.name || '',
        ]
          .filter(Boolean)
          .join(' / '),
      });
      setPreviewOpen(true);
    },
    [meta.products, meta.smsChannels, notifyUserCache, tenantUserMap],
  );

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      const payload: Record<string, unknown> = {
        ...values,
        triggerIdentifier: values.triggerScene === 'EVENT' ? values.triggerIdentifier || '' : '',
        conditionJson: safeJsonStringify(
          conditionRows.map((item) => ({
            field: item.field,
            fieldName: item.fieldName,
            dataType: item.dataType,
            operator: item.operator,
            value: String(item.value ?? ''),
          })),
        ),
        notifyUserIdsJson: safeJsonStringify(notifyUserIds),
        messageChannelId: values.targetType === 'SMS' ? values.messageChannelId || '' : '',
        targetUrl: values.targetType === 'HTTP_PUSH' ? values.targetUrl || '' : '',
        targetTopic: values.targetType === 'QUEUE_PUSH' ? values.targetTopic || '' : '',
        linkageProductId:
          values.targetType === 'SCENE_LINKAGE' ? values.linkageProductId || '' : '',
        linkageDeviceId:
          values.targetType === 'SCENE_LINKAGE' ? values.linkageDeviceId || '' : '',
        linkageServiceIdentifier:
          values.targetType === 'SCENE_LINKAGE'
            ? values.linkageServiceIdentifier || ''
            : '',
        linkageParamsJson:
          values.targetType === 'SCENE_LINKAGE'
            ? safeJsonStringify(
                linkageParamRows.map((item) => ({
                  identifier: item.identifier,
                  name: item.name,
                  dataType: item.dataType,
                  value: String(item.value ?? ''),
                })),
              )
            : '[]',
      };
      delete payload.notifyUserIds;
      if (editingId) {
        await updateIotRule({ ...payload, id: editingId });
      } else {
        await createIotRule(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const previewConditionColumns: ColumnsType<RulePreviewExtra['conditions'][number]> = [
    { title: '字段', dataIndex: 'fieldName' },
    { title: '运算', dataIndex: 'operatorText', width: 100 },
    { title: '比较值', dataIndex: 'value' },
  ];

  const notifyUserColumns = React.useMemo<ColumnsType<SysUserRecord>>(
    () => [
      {
        title: '用户名',
        render: (_, record) => record.username || record.loginname || '-',
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
        render: (value) => value || '-',
      },
      {
        title: 'Email',
        dataIndex: 'email',
        render: (value) => value || '-',
      },
      {
        title: '操作',
        width: 100,
        render: (_, record) => (
          <Button type="link" danger onClick={() => removeNotifyUser(record.id)}>
            移除
          </Button>
        ),
      },
    ],
    [removeNotifyUser],
  );

  const pickerColumns = React.useMemo<ColumnsType<SysUserRecord>>(
    () => [
      {
        title: '用户名',
        dataIndex: 'username',
      },
      {
        title: '登录名',
        dataIndex: 'loginname',
      },
      {
        title: '所属单位',
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
        render: (value) => value || '-',
      },
      {
        title: 'Email',
        dataIndex: 'email',
        render: (value) => value || '-',
      },
      {
        title: '状态',
        render: (_, record) =>
          record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
      },
    ],
    [],
  );

  const renderNotifyUserSelector = () => (
    <Col span={24}>
      <Form.Item hidden name="notifyUserIds">
        <Select mode="multiple" options={[]} />
      </Form.Item>
      <Form.Item label="通知用户" extra="留空默认通知租户全部用户">
        <Space style={{ marginBottom: 12 }}>
          <Button icon={<PlusOutlined />} onClick={openNotifyUserPicker}>
            选择用户
          </Button>
          <Typography.Text type="secondary">
            已选择 {selectedNotifyUsers.length} 个用户
          </Typography.Text>
        </Space>
        {selectedNotifyUsers.length ? (
          <Table
            rowKey="id"
            size="small"
            pagination={false}
            columns={notifyUserColumns}
            dataSource={selectedNotifyUsers}
          />
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂未选择用户" />
        )}
      </Form.Item>
    </Col>
  );

  return (
    <PageContainer title="规则引擎">
      <PlatformProTable<
        IotRuleRecord,
        {
          name?: string;
          code?: string;
          triggerScene?: string;
          targetType?: string;
          sourceProductId?: string;
          disabled?: boolean;
          current?: number;
          pageSize?: number;
        }
      >
        persistenceKey="platform-iot-rule-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="规则列表"
        columns={[
          { title: '规则名称', dataIndex: 'name' },
          { title: '规则编码', dataIndex: 'code', width: 160 },
          {
            title: '来源产品',
            dataIndex: 'sourceProductId',
            width: 220,
            valueType: 'select',
            fieldProps: {
              allowClear: true,
              options: meta.products.map((item) => ({
                label: getProductLabel(item),
                value: item.id,
              })),
            },
            render: (_, record) =>
              record.sourceProduct?.name ? getProductLabel(record.sourceProduct) : '-',
          },
          {
            title: '触发场景',
            dataIndex: 'triggerScene',
            width: 140,
            valueType: 'select',
            fieldProps: { options: toSelectOptions(meta.triggerScenes), allowClear: true },
            render: (_, record) => resolveIotOptionText(meta.triggerScenes, record.triggerScene),
          },
          {
            title: '目标类型',
            dataIndex: 'targetType',
            width: 140,
            valueType: 'select',
            fieldProps: { options: toSelectOptions(meta.targetTypes), allowClear: true },
            render: (_, record) => resolveIotOptionText(meta.targetTypes, record.targetType),
          },
          {
            title: '状态',
            dataIndex: 'disabled',
            width: 100,
            valueType: 'select',
            fieldProps: {
              allowClear: true,
              options: [
                { label: '启用', value: false },
                { label: '禁用', value: true },
              ],
            },
            render: (_, record) =>
              record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
          },
          {
            title: '更新时间',
            dataIndex: 'updatedAt',
            search: false,
            width: 180,
            render: (_, record) => formatDateTime(record.updatedAt),
          },
          {
            title: '操作',
            key: 'option',
            valueType: 'option',
            width: 190,
            render: (_, record) => (
              <TableRowActions
                actions={[
                  {
                    key: 'preview',
                    label: '详情',
                    icon: <EyeOutlined />,
                    onClick: () => void openPreview(record),
                  },
                  {
                    key: 'edit',
                    label: '修改',
                    icon: <EditOutlined />,
                    disabled: !access.hasPermission('iot.manage.rule.update'),
                    onClick: () => void openEdit(record),
                  },
                  {
                    key: 'delete',
                    label: '删除',
                    icon: <DeleteOutlined />,
                    danger: true,
                    disabled: !access.hasPermission('iot.manage.rule.delete'),
                    onClick: () => {
                      modal.confirm({
                        title: '确认删除规则',
                        content: `确定删除 ${record.name} 吗？`,
                        onOk: async () => {
                          await deleteIotRule(record.id);
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
            disabled={!access.hasPermission('iot.manage.rule.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getIotRulePage({
            name: params.name || '',
            code: params.code || '',
            sourceProductId: params.sourceProductId || '',
            triggerScene: params.triggerScene || '',
            targetType: params.targetType || '',
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

      <Modal
        title={editingId ? '修改规则' : '新增规则'}
        open={modalOpen}
        width={1080}
        confirmLoading={submitting}
        onOk={() => void submit()}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Card
            size="small"
            title="基础信息"
            style={sectionCardStyle}
          >
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item
                  label="规则名称"
                  name="name"
                  rules={[{ required: true, message: '请输入规则名称' }]}
                >
                  <Input />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="规则编码"
                  name="code"
                  rules={[{ required: true, message: '请输入规则编码' }]}
                >
                  <Input />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="来源产品"
                  name="sourceProductId"
                  rules={[{ required: true, message: '请选择来源产品' }]}
                >
                  <Select
                    showSearch
                    optionFilterProp="label"
                    options={meta.products.map((item) => ({
                      label: getProductLabel(item),
                      value: item.id,
                    }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="来源设备" name="sourceDeviceId">
                  <Select
                    allowClear
                    showSearch
                    optionFilterProp="label"
                    placeholder="留空表示该产品全部设备"
                    options={(sourceMeta.devices || []).map((item) => ({
                      label: getDeviceLabel(item),
                      value: item.id,
                    }))}
                  />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item
                  label="触发场景"
                  name="triggerScene"
                  rules={[{ required: true, message: '请选择触发场景' }]}
                >
                  <Select options={toSelectOptions(meta.triggerScenes)} />
                </Form.Item>
              </Col>
              {triggerScene === 'EVENT' ? (
                <Col span={12}>
                  <Form.Item
                    label="触发事件"
                    name="triggerIdentifier"
                    rules={[{ required: true, message: '请选择触发事件' }]}
                  >
                    <Select
                      allowClear
                      showSearch
                      optionFilterProp="label"
                      options={(sourceMeta.events || []).map((item) => ({
                        label: `${item.name || item.identifier} (${item.identifier})`,
                        value: item.identifier,
                      }))}
                    />
                  </Form.Item>
                </Col>
              ) : null}
              <Col span={12}>
                <Form.Item
                  label="目标类型"
                  name="targetType"
                  rules={[{ required: true, message: '请选择目标类型' }]}
                >
                  <Select options={toSelectOptions(meta.targetTypes)} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="启用状态" name="disabled">
                  <Radio.Group
                    options={[
                      { label: '启用', value: false },
                      { label: '禁用', value: true },
                    ]}
                  />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item label="说明" name="description">
                  <Input />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          <Card
            size="small"
            style={sectionCardStyle}
            title="规则条件"
            extra={
              <Typography.Text type="secondary">
                来源：
                {triggerScene === 'EVENT' ? '所选事件输出字段' : '产品物模型属性'}
              </Typography.Text>
            }
          >
            {availableConditionFields.length ? (
              <>
                {conditionRows.map((item, index) => (
                  <div key={`${item.field}-${index}`} style={rowStyle}>
                    <Select
                      style={{ width: 280 }}
                      showSearch
                      optionFilterProp="label"
                      placeholder="选择字段"
                      value={item.field}
                      options={availableConditionFields.map((field) => ({
                        label: field.text,
                        value: field.value,
                      }))}
                      onChange={(value) => handleConditionFieldChange(index, value)}
                    />
                    <Select
                      style={{ width: 140 }}
                      value={item.operator}
                      options={getOperatorOptions(item.dataType)}
                      onChange={(value) => updateConditionRow(index, { operator: value })}
                    />
                    {item.dataType === 'boolean' ? (
                      <Select
                        style={{ flex: 1 }}
                        value={item.value}
                        options={[
                          { label: 'true', value: 'true' },
                          { label: 'false', value: 'false' },
                        ]}
                        onChange={(value) => updateConditionRow(index, { value })}
                      />
                    ) : (
                      <Input
                        style={{ flex: 1 }}
                        placeholder="比较值"
                        value={item.value}
                        onChange={(event) =>
                          updateConditionRow(index, { value: event.target.value })
                        }
                      />
                    )}
                    <Button
                      danger
                      type="text"
                      icon={<DeleteOutlined />}
                      onClick={() =>
                        setConditionRows((current) =>
                          current.filter((_, currentIndex) => currentIndex !== index),
                        )
                      }
                    />
                  </div>
                ))}
                <Button
                  type="dashed"
                  onClick={() =>
                    setConditionRows((current) => [
                      ...current,
                      {
                        field: '',
                        fieldName: '',
                        dataType: 'string',
                        operator: 'EQ',
                        value: '',
                      },
                    ])
                  }
                >
                  新增条件
                </Button>
              </>
            ) : (
              <div style={emptyStyle}>
                请先选择来源产品{triggerScene === 'EVENT' ? '和触发事件' : ''}
              </div>
            )}
          </Card>

          <Card size="small" title="动作配置">
            {targetType === 'SMS' ? (
              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item label="短信渠道" name="messageChannelId">
                    <Select
                      allowClear
                      showSearch
                      optionFilterProp="label"
                      placeholder="留空使用默认短信渠道"
                      options={meta.smsChannels.map((item) => ({
                        label: getChannelLabel(item),
                        value: item.id,
                      }))}
                    />
                  </Form.Item>
                </Col>
                {renderNotifyUserSelector()}
                <Col span={24}>
                  <Form.Item
                    label="短信内容"
                    name="actionContent"
                    rules={
                      targetType === 'SMS'
                        ? [{ required: true, message: '请输入短信内容' }]
                        : undefined
                    }
                  >
                    <Input.TextArea
                      rows={4}
                      placeholder="支持占位符，如 设备${deviceCode}温度为${temperature}"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ) : null}

            {targetType === 'SITE_MESSAGE' ? (
              <Row gutter={12}>
                {renderNotifyUserSelector()}
                <Col span={12}>
                  <Form.Item
                    label="消息标题"
                    name="actionTitle"
                    rules={
                      targetType === 'SITE_MESSAGE'
                        ? [{ required: true, message: '请输入消息标题' }]
                        : undefined
                    }
                  >
                    <Input placeholder="支持占位符，如 告警通知-${deviceCode}" />
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item
                    label="消息内容"
                    name="actionContent"
                    rules={
                      targetType === 'SITE_MESSAGE'
                        ? [{ required: true, message: '请输入消息内容' }]
                        : undefined
                    }
                  >
                    <Input.TextArea
                      rows={4}
                      placeholder="支持占位符，如 设备${deviceCode}发生事件${eventCode}"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ) : null}

            {targetType === 'HTTP_PUSH' ? (
              <Row gutter={12}>
                <Col span={24}>
                  <Form.Item
                    label="目标地址"
                    name="targetUrl"
                    rules={
                      targetType === 'HTTP_PUSH'
                        ? [{ required: true, message: '请输入目标地址' }]
                        : undefined
                    }
                  >
                    <Input placeholder="如 https://example.com/webhook" />
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item label="推送内容" name="actionContent">
                    <Input.TextArea
                      rows={5}
                      placeholder="留空时推送默认JSON；也可自定义JSON并使用占位符"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ) : null}

            {targetType === 'QUEUE_PUSH' ? (
              <Row gutter={12}>
                <Col span={24}>
                  <Form.Item
                    label="目标主题"
                    name="targetTopic"
                    rules={
                      targetType === 'QUEUE_PUSH'
                        ? [{ required: true, message: '请输入目标主题' }]
                        : undefined
                    }
                  >
                    <Input placeholder="如 wk.device.forward.custom" />
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item label="消息内容" name="actionContent">
                    <Input.TextArea
                      rows={5}
                      placeholder="留空时转发默认JSON；也可自定义JSON并使用占位符"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ) : null}

            {targetType === 'SCENE_LINKAGE' ? (
              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item
                    label="目标产品"
                    name="linkageProductId"
                    rules={
                      targetType === 'SCENE_LINKAGE'
                        ? [{ required: true, message: '请选择目标产品' }]
                        : undefined
                    }
                  >
                    <Select
                      showSearch
                      optionFilterProp="label"
                      options={meta.products.map((item) => ({
                        label: getProductLabel(item),
                        value: item.id,
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="目标设备"
                    name="linkageDeviceId"
                    rules={
                      targetType === 'SCENE_LINKAGE'
                        ? [{ required: true, message: '请选择目标设备' }]
                        : undefined
                    }
                  >
                    <Select
                      showSearch
                      optionFilterProp="label"
                      options={(linkageMeta.devices || []).map((item) => ({
                        label: getDeviceLabel(item),
                        value: item.id,
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="联动服务"
                    name="linkageServiceIdentifier"
                    rules={
                      targetType === 'SCENE_LINKAGE'
                        ? [{ required: true, message: '请选择联动服务' }]
                        : undefined
                    }
                  >
                    <Select
                      showSearch
                      optionFilterProp="label"
                      options={(linkageMeta.services || []).map((item) => ({
                        label: `${item.name || item.identifier} (${item.identifier})`,
                        value: item.identifier,
                      }))}
                    />
                  </Form.Item>
                </Col>
                {renderNotifyUserSelector()}
                <Col span={24}>
                  <Typography.Text strong style={{ display: 'block', marginBottom: 12 }}>
                    联动参数
                  </Typography.Text>
                  {linkageParamRows.length ? (
                    linkageParamRows.map((item, index) => (
                      <div key={`${item.identifier}-${index}`} style={rowStyle}>
                        <Input
                          disabled
                          style={{ width: 280 }}
                          value={item.name || item.identifier}
                        />
                        <Input disabled style={{ width: 140 }} value={item.dataType} />
                        {item.dataType === 'boolean' ? (
                          <Select
                            style={{ flex: 1 }}
                            value={item.value}
                            options={[
                              { label: 'true', value: 'true' },
                              { label: 'false', value: 'false' },
                            ]}
                            onChange={(value) => updateLinkageParamRow(index, value)}
                          />
                        ) : (
                          <Input
                            style={{ flex: 1 }}
                            placeholder="支持占位符，如 ${temperature}"
                            value={item.value}
                            onChange={(event) =>
                              updateLinkageParamRow(index, event.target.value)
                            }
                          />
                        )}
                      </div>
                    ))
                  ) : (
                    <div style={emptyStyle}>所选服务无入参，或请先选择联动服务</div>
                  )}
                </Col>
                <Col span={12}>
                  <Form.Item label="通知标题" name="actionTitle">
                    <Input placeholder="留空使用默认站内信标题" />
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item label="通知内容" name="actionContent">
                    <Input.TextArea
                      rows={4}
                      placeholder="留空使用默认站内信内容；支持占位符"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ) : null}
          </Card>
        </Form>
      </Modal>

      <Modal
        title="选择用户"
        open={pickerOpen}
        width={960}
        onOk={confirmNotifyUserPicker}
        onCancel={() => {
          setPickerSelectedRowKeys([]);
          setPickerSelectedRows([]);
          setPickerOpen(false);
        }}
        destroyOnHidden
      >
        <Space orientation="vertical" style={{ display: 'flex' }} size={16}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '240px 1fr auto',
              gap: 12,
              alignItems: 'center',
            }}
          >
            <TreeSelect
              placeholder="按单位筛选"
              allowClear
              treeDefaultExpandAll
              style={{ width: '100%' }}
              treeData={toTreeData(unitTree)}
              value={pickerParams.unitId}
              onClear={() =>
                setPickerParams((current) => ({
                  ...current,
                  unitId: undefined,
                  current: 1,
                }))
              }
              onChange={(value) =>
                setPickerParams((current) => ({
                  ...current,
                  unitId: value,
                  current: 1,
                }))
              }
            />
            <Input.Search
              allowClear
              placeholder="请输入姓名/登录名"
              onSearch={(value) =>
                setPickerParams((current) => ({
                  ...current,
                  keywords: value,
                  current: 1,
                }))
              }
            />
            <Button onClick={() => loadUsers(pickerParams)}>刷新</Button>
          </div>
          <Table
            rowKey="id"
            loading={pickerLoading}
            columns={pickerColumns}
            dataSource={pickerRows}
            rowSelection={{
              selectedRowKeys: pickerSelectedRowKeys,
              onChange: (keys, rows) => {
                setPickerSelectedRowKeys(keys);
                setPickerSelectedRows(rows);
              },
            }}
            pagination={{
              current: pickerParams.current,
              pageSize: pickerParams.pageSize,
              total: pickerTotal,
              onChange: (page, pageSize) =>
                setPickerParams((current) => ({
                  ...current,
                  current: page,
                  pageSize,
                })),
            }}
          />
        </Space>
      </Modal>

      <Drawer
        title="规则详情"
        open={previewOpen}
        size="large"
        onClose={() => setPreviewOpen(false)}
      >
        {previewData ? (
          <Space orientation="vertical" size={12} style={{ width: '100%' }}>
            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="规则名称">{previewData.name}</Descriptions.Item>
              <Descriptions.Item label="规则编码">{previewData.code}</Descriptions.Item>
              <Descriptions.Item label="来源产品">
                {getProductLabel(
                  meta.products.find((item) => item.id === previewData.sourceProductId),
                )}
              </Descriptions.Item>
              <Descriptions.Item label="来源设备">
                {previewExtra.sourceDeviceText || '全部设备'}
              </Descriptions.Item>
              <Descriptions.Item label="触发场景">
                {resolveIotOptionText(meta.triggerScenes, previewData.triggerScene)}
              </Descriptions.Item>
              <Descriptions.Item label="触发事件">
                {previewExtra.triggerText || '--'}
              </Descriptions.Item>
              <Descriptions.Item label="目标类型">
                {resolveIotOptionText(meta.targetTypes, previewData.targetType)}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={previewData.disabled ? 'error' : 'success'}>
                  {previewData.disabled ? '禁用' : '启用'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="说明" span={2}>
                {previewData.description || '--'}
              </Descriptions.Item>
            </Descriptions>

            <Card size="small" title="规则条件">
              <Table
                rowKey={(record) =>
                  `${record.fieldName || record.field}-${record.operatorText}-${record.value}`
                }
                size="small"
                pagination={false}
                columns={previewConditionColumns}
                dataSource={previewExtra.conditions}
                locale={{ emptyText: <Empty description="未配置条件" /> }}
              />
            </Card>

            <Card size="small" title="动作配置">
              <Descriptions bordered size="small" column={1}>
                <Descriptions.Item label="通知用户">
                  {previewExtra.notifyUsersText || '全部租户用户'}
                </Descriptions.Item>
                {resolveIotOptionValue(previewData.targetType) === 'SMS' ? (
                  <Descriptions.Item label="短信渠道">
                    {previewExtra.smsChannelText || '默认短信渠道'}
                  </Descriptions.Item>
                ) : null}
                {resolveIotOptionValue(previewData.targetType) === 'HTTP_PUSH' ? (
                  <Descriptions.Item label="目标地址">
                    {previewData.targetUrl || '--'}
                  </Descriptions.Item>
                ) : null}
                {resolveIotOptionValue(previewData.targetType) === 'QUEUE_PUSH' ? (
                  <Descriptions.Item label="目标主题">
                    {previewData.targetTopic || '--'}
                  </Descriptions.Item>
                ) : null}
                {resolveIotOptionValue(previewData.targetType) === 'SCENE_LINKAGE' ? (
                  <Descriptions.Item label="联动目标">
                    {previewExtra.linkageText || '--'}
                  </Descriptions.Item>
                ) : null}
                <Descriptions.Item label="动作标题">
                  {previewData.actionTitle || '--'}
                </Descriptions.Item>
                <Descriptions.Item label="动作内容">
                  <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                    {previewData.actionContent || '--'}
                  </Typography.Paragraph>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          </Space>
        ) : (
          <Empty description="未找到规则详情" />
        )}
      </Drawer>
    </PageContainer>
  );
};

export default RulePage;
