import {
  AppstoreOutlined,
  BorderOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PauseCircleOutlined,
  SettingOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Alert,
  Button,
  Card,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Radio,
  Select,
  Space,
  Switch,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import { getPubUserPage } from '@/services/budiot/pub/user';
import {
  createVideoTask,
  deleteVideoTask,
  getVideoTaskDetail,
  getVideoTaskLogs,
  getVideoTaskMeta,
  getVideoTaskPage,
  getVideoTaskRuntime,
  startVideoTask,
  stopVideoTask,
  updateVideoTask,
} from '@/services/budiot/video/task';
import type { SysUserRecord } from '@/services/budiot/typing';
import RegionConfigModal from './region';
import type {
  VideoAlgoTaskMeta,
  VideoAlgoTaskLogEntry,
  VideoAlgoTaskRecord,
  VideoAlgoTaskRuntimeRecord,
  VideoTaskNotifyConfigRecord,
  VideoTaskRegionConfigRecord,
} from '@/services/budiot/video/typing';

type TaskFormValues = Partial<VideoAlgoTaskRecord> & {
  notifyChannelIds?: string[];
  notifyUserIds?: string[];
};
type ViewMode = 'table' | 'card';
type CronGeneratorMode = 'MINUTE' | 'HOUR' | 'DAY' | 'WEEK';

const defaultFormValues: TaskFormValues = {
  name: '',
  taskType: 'REALTIME',
  frameInterval: 25,
  trackingEnabled: true,
  trackingSimilarityThreshold: 0.2,
  trackingMaxAliveFrames: 25,
  trackingSmoothFactor: 0.25,
  allDayArming: true,
  alarmEnabled: true,
  alarmRecordSeconds: 5,
  sameAlarmWindowMinutes: 5,
  notifyEnabled: false,
  notifyChannelIds: [],
  notifyUserIds: [],
  notifyConfigs: [],
  cameraIds: [],
  modelIds: [],
  regionConfigs: [],
};

const enumValue = (item: any) => (typeof item === 'string' ? item : item?.value || item?.name || '');
const enumLabel = (item: any) => (typeof item === 'string' ? item : item?.text || item?.label || item?.value || '');

const getStatusColor = (status?: string) => {
  if (status === 'RUNNING') {
    return 'success';
  }
  if (status === 'FAILED') {
    return 'error';
  }
  if (status === 'STOPPED') {
    return 'default';
  }
  return 'processing';
};

const MONTH_ALIASES: Record<string, number> = {
  JAN: 1,
  FEB: 2,
  MAR: 3,
  APR: 4,
  MAY: 5,
  JUN: 6,
  JUL: 7,
  AUG: 8,
  SEP: 9,
  OCT: 10,
  NOV: 11,
  DEC: 12,
};

const WEEKDAY_ALIASES: Record<string, number> = {
  SUN: 1,
  MON: 2,
  TUE: 3,
  WED: 4,
  THU: 5,
  FRI: 6,
  SAT: 7,
};

const parseCronValue = (value: string, aliases?: Record<string, number>) => {
  const normalized = value.trim().toUpperCase();
  if (aliases?.[normalized] !== undefined) {
    return aliases[normalized];
  }
  if (!/^\d+$/.test(normalized)) {
    return undefined;
  }
  return Number(normalized);
};

const validateCronSegment = (
  segment: string,
  min: number,
  max: number,
  aliases?: Record<string, number>,
) => {
  const normalized = segment.trim().toUpperCase();
  if (!normalized) {
    return false;
  }
  if (normalized === '*') {
    return true;
  }
  if (normalized.includes('/')) {
    const [base, stepText] = normalized.split('/');
    if (!base || !stepText || !/^\d+$/.test(stepText) || Number(stepText) <= 0) {
      return false;
    }
    return validateCronSegment(base, min, max, aliases);
  }
  if (normalized.includes('-')) {
    const [startText, endText] = normalized.split('-');
    const start = parseCronValue(startText, aliases);
    const end = parseCronValue(endText, aliases);
    return start !== undefined && end !== undefined && start >= min && end <= max && start <= end;
  }
  const value = parseCronValue(normalized, aliases);
  return value !== undefined && value >= min && value <= max;
};

const validateCronField = (
  field: string,
  min: number,
  max: number,
  options?: { allowQuestion?: boolean; aliases?: Record<string, number> },
) => {
  const normalized = field.trim().toUpperCase();
  if (!normalized) {
    return false;
  }
  if (normalized === '?') {
    return Boolean(options?.allowQuestion);
  }
  return normalized.split(',').every((segment) => validateCronSegment(segment, min, max, options?.aliases));
};

const validateCronExpression = (value?: string) => {
  const expression = value?.trim();
  if (!expression) {
    return false;
  }
  const fields = expression.split(/\s+/);
  if (fields.length !== 6 && fields.length !== 7) {
    return false;
  }
  const [second, minute, hour, dayOfMonth, month, dayOfWeek, year] = fields;
  const valid =
    validateCronField(second, 0, 59) &&
    validateCronField(minute, 0, 59) &&
    validateCronField(hour, 0, 23) &&
    validateCronField(dayOfMonth, 1, 31, { allowQuestion: true }) &&
    validateCronField(month, 1, 12, { aliases: MONTH_ALIASES }) &&
    validateCronField(dayOfWeek, 1, 7, { allowQuestion: true, aliases: WEEKDAY_ALIASES }) &&
    (year === undefined || validateCronField(year, 1970, 2099));
  if (!valid) {
    return false;
  }
  return dayOfMonth === '?' || dayOfWeek === '?';
};

const normalizeNotifyConfigs = (configs?: VideoTaskNotifyConfigRecord[]) =>
  (configs || [])
    .filter((item): item is VideoTaskNotifyConfigRecord => Boolean(item?.channelId))
    .map((item) => ({
      channelId: item.channelId,
      templateId: item.templateId,
    }));

const parseNotifyUserIds = (value?: string) => {
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed.filter((item): item is string => typeof item === 'string' && item.trim().length > 0);
    }
  } catch (error) {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [];
};

const normalizeRegionConfigs = (configs?: VideoTaskRegionConfigRecord[]) =>
  (configs || [])
    .filter((item): item is VideoTaskRegionConfigRecord => Boolean(item?.cameraId && item?.modelId))
    .map((item, index) => ({
      ...item,
      location: item.location ?? index,
      enabled: item.enabled !== false,
    }));

const formatRuntimeTime = (value?: number) => (value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-');

const TaskPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<TaskFormValues>();
  const [meta, setMeta] = React.useState<VideoAlgoTaskMeta>({
    taskTypes: [],
    taskStatuses: [],
    cameras: [],
    models: [],
    notifyChannels: [],
    notifyTemplates: [],
  });
  const [notifyUsers, setNotifyUsers] = React.useState<SysUserRecord[]>([]);
  const [viewMode, setViewMode] = React.useState<ViewMode>('card');
  const [tableData, setTableData] = React.useState<VideoAlgoTaskRecord[]>([]);
  const [pageInfo, setPageInfo] = React.useState({ current: 1, pageSize: 10, total: 0 });
  const [editingId, setEditingId] = React.useState<string>();
  const [editingRegionConfigs, setEditingRegionConfigs] = React.useState<VideoTaskRegionConfigRecord[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [regionModalOpen, setRegionModalOpen] = React.useState(false);
  const [regionTaskId, setRegionTaskId] = React.useState<string>();
  const [runtimeOpen, setRuntimeOpen] = React.useState(false);
  const [runtimeLoading, setRuntimeLoading] = React.useState(false);
  const [runtimeTask, setRuntimeTask] = React.useState<VideoAlgoTaskRecord>();
  const [runtimeData, setRuntimeData] = React.useState<VideoAlgoTaskRuntimeRecord>();
  const [runtimeLogs, setRuntimeLogs] = React.useState<VideoAlgoTaskLogEntry[]>([]);
  const [runtimeCameraId, setRuntimeCameraId] = React.useState<string>();
  const [cronMode, setCronMode] = React.useState<CronGeneratorMode>('MINUTE');
  const [cronInterval, setCronInterval] = React.useState(5);
  const [cronHour, setCronHour] = React.useState(8);
  const [cronMinute, setCronMinute] = React.useState(0);
  const [cronWeekday, setCronWeekday] = React.useState(2);
  const taskType = Form.useWatch('taskType', form);
  const allDayArming = Form.useWatch('allDayArming', form);
  const alarmEnabled = Form.useWatch('alarmEnabled', form);
  const notifyEnabled = Form.useWatch('notifyEnabled', form);
  const notifyConfigs = (Form.useWatch('notifyConfigs', form) || []) as VideoTaskNotifyConfigRecord[];
  const notifyChannelIds = (Form.useWatch('notifyChannelIds', form) || []) as string[];
  const isSnapshotTask = taskType === 'SNAPSHOT';
  const previousTaskTypeRef = React.useRef<string | undefined>(defaultFormValues.taskType);
  const generatedCron = React.useMemo(() => {
    if (cronMode === 'MINUTE') {
      return `0 */${cronInterval} * * * ?`;
    }
    if (cronMode === 'HOUR') {
      return `0 ${cronMinute} */${cronInterval} * * ?`;
    }
    if (cronMode === 'DAY') {
      return `0 ${cronMinute} ${cronHour} */${cronInterval} * ?`;
    }
    return `0 ${cronMinute} ${cronHour} ? * ${cronWeekday}`;
  }, [cronHour, cronInterval, cronMinute, cronMode, cronWeekday]);

  React.useEffect(() => {
    getVideoTaskMeta()
      .then((response) => setMeta(response.data))
      .catch(() => undefined);
    getPubUserPage({ pageNo: 1, pageSize: 200, disabled: false })
      .then((response) => setNotifyUsers(response.data?.list || []))
      .catch(() => undefined);
  }, []);

  React.useEffect(() => {
    if (!notifyEnabled) {
      form.setFieldsValue({ notifyConfigs: [], notifyChannelIds: [], notifyUserIds: [] });
    }
  }, [form, notifyEnabled]);

  React.useEffect(() => {
    if (isSnapshotTask) {
      form.setFieldsValue({
        frameInterval: 1,
        trackingEnabled: false,
        alarmEnabled: false,
        notifyEnabled: false,
        notifyUserIds: [],
        notifyUserIdsJson: '[]',
        notifyChannelIds: [],
        notifyConfigs: [],
        modelIds: [],
      });
      setEditingRegionConfigs([]);
    }
  }, [form, isSnapshotTask]);

  React.useEffect(() => {
    const previousTaskType = previousTaskTypeRef.current;
    if (previousTaskType === 'SNAPSHOT' && taskType === 'REALTIME') {
      form.setFieldValue('frameInterval', defaultFormValues.frameInterval);
    }
    previousTaskTypeRef.current = taskType;
  }, [form, taskType]);

  React.useEffect(() => {
    if (!alarmEnabled || isSnapshotTask) {
      form.setFieldsValue({
        notifyEnabled: false,
        notifyUserIds: [],
        notifyUserIdsJson: '[]',
        notifyChannelIds: [],
        notifyConfigs: [],
      });
    }
  }, [alarmEnabled, form, isSnapshotTask]);

  const requestTasks = React.useCallback(async (params: Record<string, any>) => {
    const response = await getVideoTaskPage({
      name: params.name,
      taskType: params.taskType,
      status: params.status,
      pageNo: params.current,
      pageSize: params.pageSize,
      pageOrderName: 'id',
      pageOrderBy: 'descending',
    });
    const list = response.data?.list || [];
    setTableData(list);
    setPageInfo({
      current: params.current || 1,
      pageSize: params.pageSize || 10,
      total: response.data?.totalCount || 0,
    });
    return {
      data: list,
      total: response.data?.totalCount || 0,
      success: true,
    };
  }, []);

  const openCreate = React.useCallback(() => {
    setEditingId(undefined);
    setEditingRegionConfigs([]);
    setCronMode('MINUTE');
    setCronInterval(5);
    setCronHour(8);
    setCronMinute(0);
    setCronWeekday(2);
    previousTaskTypeRef.current = defaultFormValues.taskType;
    form.resetFields();
    form.setFieldsValue(defaultFormValues);
    setModalOpen(true);
  }, [form]);

  const openEdit = React.useCallback(
    async (record: VideoAlgoTaskRecord) => {
      const response = await getVideoTaskDetail(record.id);
      setEditingId(record.id);
      setEditingRegionConfigs(normalizeRegionConfigs(response.data.regionConfigs));
      previousTaskTypeRef.current = response.data.taskType || defaultFormValues.taskType;
      form.resetFields();
      form.setFieldsValue({
        ...defaultFormValues,
        ...response.data,
        sameAlarmWindowMinutes: response.data.sameAlarmWindowMinutes ?? defaultFormValues.sameAlarmWindowMinutes,
        notifyChannelIds: normalizeNotifyConfigs(response.data.notifyConfigs).map((item) => item.channelId),
        notifyUserIds: parseNotifyUserIds(response.data.notifyUserIdsJson),
        notifyConfigs: normalizeNotifyConfigs(response.data.notifyConfigs),
      });
      setModalOpen(true);
    },
    [form],
  );

  const submitForm = React.useCallback(async () => {
    await form.validateFields();
    const values = form.getFieldsValue(true) as TaskFormValues;
    const payload = {
      ...values,
      frameInterval: values.taskType === 'SNAPSHOT' ? 1 : values.frameInterval,
      modelIds: values.taskType === 'SNAPSHOT' ? [] : values.modelIds,
      trackingEnabled: values.taskType === 'SNAPSHOT' ? false : values.trackingEnabled,
      alarmEnabled: values.taskType === 'SNAPSHOT' ? false : values.alarmEnabled,
      notifyEnabled: values.taskType === 'SNAPSHOT' || !values.alarmEnabled ? false : values.notifyEnabled,
      notifyUserIdsJson:
        values.taskType === 'SNAPSHOT' || !values.alarmEnabled || !values.notifyEnabled
          ? '[]'
          : JSON.stringify(values.notifyUserIds || []),
      notifyConfigs:
        values.taskType === 'SNAPSHOT' || !values.alarmEnabled || !values.notifyEnabled
          ? []
          : normalizeNotifyConfigs(values.notifyConfigs),
      sameAlarmWindowMinutes:
        values.taskType === 'SNAPSHOT'
          ? defaultFormValues.sameAlarmWindowMinutes
          : values.sameAlarmWindowMinutes ?? defaultFormValues.sameAlarmWindowMinutes,
      regionConfigs: values.taskType === 'SNAPSHOT' ? [] : editingRegionConfigs,
    };
    setSubmitting(true);
    try {
      if (editingId) {
        await updateVideoTask({ ...payload, id: editingId });
      } else {
        await createVideoTask(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  }, [editingId, editingRegionConfigs, form]);

  const handleDelete = React.useCallback(
    (record: VideoAlgoTaskRecord) => {
      modal.confirm({
        title: `确认删除算法任务“${record.name}”吗？`,
        onOk: async () => {
          await deleteVideoTask(record.id);
          actionRef.current?.reload();
        },
      });
    },
    [modal],
  );

  const handleTaskStart = React.useCallback(async (record: VideoAlgoTaskRecord) => {
    await startVideoTask(record.id);
    actionRef.current?.reload();
  }, []);

  const handleTaskStop = React.useCallback(async (record: VideoAlgoTaskRecord) => {
    await stopVideoTask(record.id);
    actionRef.current?.reload();
  }, []);

  const handleNotifyChannelsChange = React.useCallback(
    (channelIds: string[]) => {
      const current = normalizeNotifyConfigs((form.getFieldValue('notifyConfigs') || []) as VideoTaskNotifyConfigRecord[]);
      const next = channelIds.map((channelId) => current.find((item) => item.channelId === channelId) || { channelId, templateId: undefined });
      form.setFieldsValue({ notifyChannelIds: channelIds, notifyConfigs: next });
    },
    [form],
  );

  const openRegionConfig = React.useCallback((record: VideoAlgoTaskRecord) => {
    setRegionTaskId(record.id);
    setRegionModalOpen(true);
  }, []);

  const closeRegionConfig = React.useCallback(() => {
    setRegionModalOpen(false);
    setRegionTaskId(undefined);
  }, []);

  const handleRegionConfigSaved = React.useCallback(() => {
    closeRegionConfig();
    actionRef.current?.reload();
  }, [closeRegionConfig]);

  const handleRegionSnapshotPersisted = React.useCallback((cameraId: string, snapshotImageUrl: string) => {
    setMeta((prev) => ({
      ...prev,
      cameras: prev.cameras.map((camera) =>
        camera.id === cameraId ? { ...camera, snapshotImageUrl } : camera,
      ),
    }));
  }, []);

  const loadTaskRuntimeDetail = React.useCallback(async (taskId: string, cameraId?: string) => {
    const [runtimeRes, logsRes] = await Promise.all([
      getVideoTaskRuntime(taskId),
      getVideoTaskLogs(taskId, { cameraId, limit: 200 }),
    ]);
    setRuntimeData(runtimeRes.data);
    setRuntimeLogs(logsRes.data?.entries || []);
  }, []);

  const openRuntimeModal = React.useCallback(async (record: VideoAlgoTaskRecord) => {
    setRuntimeTask(record);
    setRuntimeCameraId(undefined);
    setRuntimeOpen(true);
    setRuntimeLoading(true);
    try {
      await loadTaskRuntimeDetail(record.id);
    } finally {
      setRuntimeLoading(false);
    }
  }, [loadTaskRuntimeDetail]);

  React.useEffect(() => {
    if (!runtimeOpen || !runtimeTask?.id) {
      return;
    }
    setRuntimeLoading(true);
    loadTaskRuntimeDetail(runtimeTask.id, runtimeCameraId)
      .catch(() => undefined)
      .finally(() => setRuntimeLoading(false));
  }, [loadTaskRuntimeDetail, runtimeCameraId, runtimeOpen, runtimeTask?.id]);

  React.useEffect(() => {
    if (!runtimeOpen || !runtimeTask?.id || runtimeTask.status !== 'RUNNING') {
      return;
    }
    const timer = window.setInterval(() => {
      void loadTaskRuntimeDetail(runtimeTask.id, runtimeCameraId);
    }, 3000);
    return () => window.clearInterval(timer);
  }, [loadTaskRuntimeDetail, runtimeCameraId, runtimeOpen, runtimeTask?.id, runtimeTask?.status]);

  const runtimeCameraOptions = React.useMemo(
    () =>
      Object.entries(runtimeData?.cameras || {}).map(([cameraId, cameraState]) => ({
        label: `${cameraId}${cameraState.status ? ` (${cameraState.status})` : ''}`,
        value: cameraId,
      })),
    [runtimeData?.cameras],
  );

  const columns = React.useMemo<ProColumns<VideoAlgoTaskRecord>[]>(
    () => [
      { title: '任务名称', dataIndex: 'name' },
      {
        title: '任务类型',
        dataIndex: 'taskType',
        valueType: 'select',
        fieldProps: {
          options: meta.taskTypes.map((item) => ({ label: enumLabel(item), value: enumValue(item) })),
          allowClear: true,
        },
        render: (_, record) => enumLabel(meta.taskTypes.find((item) => enumValue(item) === record.taskType) || record.taskType),
      },
      {
        title: '状态',
        dataIndex: 'status',
        valueType: 'select',
        fieldProps: {
          options: meta.taskStatuses.map((item) => ({ label: enumLabel(item), value: enumValue(item) })),
          allowClear: true,
        },
        render: (_, record) => <Tag color={getStatusColor(record.status)}>{enumLabel(meta.taskStatuses.find((item) => enumValue(item) === record.status) || record.status)}</Tag>,
      },
      {
        title: '关联模型',
        search: false,
        render: (_, record) =>
          record.modelNames?.length ? (
            <Space size={[4, 4]} wrap>
              {record.modelNames.map((name) => (
                <Tag key={name}>{name}</Tag>
              ))}
            </Space>
          ) : (
            '-'
          ),
      },
      {
        title: '摄像头数',
        search: false,
        render: (_, record) => record.cameraIds?.length || 0,
      },
      {
        title: '区域数',
        search: false,
        render: (_, record) => record.regionConfigs?.length || 0,
      },
      {
        title: '抽帧间隔',
        dataIndex: 'frameInterval',
        search: false,
      },
      {
        title: '布防',
        search: false,
        render: (_, record) => (record.allDayArming ? '全天' : `${record.beginTime || '-'} ~ ${record.endTime || '-'}`),
      },
      {
        title: '操作',
        valueType: 'option',
        width: 320,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'start',
                label: '启动',
                icon: <PlayCircleOutlined />,
                disabled: record.status === 'RUNNING',
                onClick: async () => handleTaskStart(record),
              },
              {
                key: 'stop',
                label: '停止',
                icon: <PauseCircleOutlined />,
                disabled: record.status !== 'RUNNING',
                onClick: async () => handleTaskStop(record),
              },
              {
                key: 'runtime',
                label: '日志',
                icon: <EyeOutlined />,
                onClick: async () => openRuntimeModal(record),
              },
              {
                key: 'region',
                label: '配置',
                icon: <SettingOutlined />,
                onClick: async () => openRegionConfig(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                hidden: !access.hasPermission?.('video.manage.media.task'),
                onClick: async () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                hidden: !access.hasPermission?.('video.manage.media.task'),
                onClick: () => handleDelete(record),
              },
            ]}
          />
        ),
      },
    ],
    [access, handleDelete, handleTaskStart, handleTaskStop, meta.taskStatuses, meta.taskTypes, openEdit, openRegionConfig, openRuntimeModal],
  );

  return (
    <>
      <PlatformProTable<VideoAlgoTaskRecord, Record<string, any>>
        actionRef={actionRef}
        persistenceKey="video-task-table"
        rowKey="id"
        columns={columns}
        request={requestTasks}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增任务
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
                {tableData.length ? (
                  <>
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                        gap: 16,
                      }}
                    >
                      {tableData.map((record) => (
                        <Card key={record.id} hoverable styles={{ body: { padding: 18 } }}>
                          <Space direction="vertical" size={14} style={{ width: '100%' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
                              <div>
                                <Typography.Title level={5} style={{ margin: 0 }}>
                                  {record.name}
                                </Typography.Title>
                                <Typography.Text type="secondary">{enumLabel(meta.taskTypes.find((item) => enumValue(item) === record.taskType) || record.taskType)}</Typography.Text>
                              </div>
                              <Tag color={getStatusColor(record.status)}>
                                {enumLabel(meta.taskStatuses.find((item) => enumValue(item) === record.status) || record.status)}
                              </Tag>
                            </div>
                            <Typography.Text type="secondary">
                              摄像头 {record.cameraIds?.length || 0} 个 · 区域 {record.regionConfigs?.length || 0} 个
                            </Typography.Text>
                            <Space size={[4, 4]} wrap>
                              {(record.modelNames || []).length ? (
                                record.modelNames?.map((name) => <Tag key={`${record.id}-${name}`}>{name}</Tag>)
                              ) : (
                                <Tag>未关联模型</Tag>
                              )}
                            </Space>
                            <Typography.Text type="secondary">
                              {record.allDayArming ? '全天布防' : `布防时间：${record.beginTime || '-'} ~ ${record.endTime || '-'}`}
                            </Typography.Text>
                            <Space wrap>
                              <Tooltip title="启动">
                                <Button type="link" size="small" icon={<PlayCircleOutlined />} disabled={record.status === 'RUNNING'} style={{ paddingInline: 0 }} onClick={() => handleTaskStart(record)} />
                              </Tooltip>
                              <Tooltip title="停止">
                                <Button type="link" size="small" icon={<PauseCircleOutlined />} disabled={record.status !== 'RUNNING'} style={{ paddingInline: 0 }} onClick={() => handleTaskStop(record)} />
                              </Tooltip>
                              <Tooltip title="日志">
                                <Button type="link" size="small" icon={<EyeOutlined />} style={{ paddingInline: 0 }} onClick={() => openRuntimeModal(record)} />
                              </Tooltip>
                              <Tooltip title="配置">
                                <Button type="link" size="small" icon={<SettingOutlined />} style={{ paddingInline: 0 }} onClick={() => openRegionConfig(record)} />
                              </Tooltip>
                              {access.hasPermission?.('video.manage.media.task') ? (
                                <Tooltip title="修改">
                                  <Button type="link" size="small" icon={<EditOutlined />} style={{ paddingInline: 0 }} onClick={() => openEdit(record)} />
                                </Tooltip>
                              ) : null}
                              {access.hasPermission?.('video.manage.media.task') ? (
                                <Tooltip title="删除">
                                  <Button type="link" size="small" danger icon={<DeleteOutlined />} style={{ paddingInline: 0 }} onClick={() => handleDelete(record)} />
                                </Tooltip>
                              ) : null}
                            </Space>
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
                  </>
                ) : (
                  <Empty description="暂无算法任务" />
                )}
              </Card>
            </>
          )
        }
      />

      <Modal
        title={runtimeTask ? `算法任务运行态 - ${runtimeTask.name}` : '算法任务运行态'}
        open={runtimeOpen}
        onCancel={() => {
          setRuntimeOpen(false);
          setRuntimeTask(undefined);
          setRuntimeData(undefined);
          setRuntimeLogs([]);
          setRuntimeCameraId(undefined);
        }}
        footer={null}
        width={1080}
        destroyOnClose
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space wrap size={12}>
            <Tag color={getStatusColor(runtimeData?.status || runtimeTask?.status)}>{runtimeData?.status || runtimeTask?.status || 'UNKNOWN'}</Tag>
            <Typography.Text type="secondary">推理线程：{runtimeData?.inference_workers ?? '-'}</Typography.Text>
            <Typography.Text type="secondary">推理队列：{runtimeData?.inference_queue_size ?? '-'}</Typography.Text>
            <Typography.Text type="secondary">日志数：{runtimeData?.log_count ?? runtimeLogs.length}</Typography.Text>
            <Typography.Text type="secondary">更新时间：{formatRuntimeTime(runtimeData?.updated_at)}</Typography.Text>
          </Space>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
              gap: 12,
            }}
          >
            {Object.entries(runtimeData?.cameras || {}).map(([cameraId, cameraState]) => (
              <Card key={cameraId} size="small" loading={runtimeLoading}>
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Typography.Text strong>{cameraId}</Typography.Text>
                  <Space wrap size={[6, 6]}>
                    <Tag color={cameraState.status === 'ERROR' ? 'error' : cameraState.status === 'RUNNING' ? 'success' : 'processing'}>
                      {cameraState.status || 'IDLE'}
                    </Tag>
                    <Tag color={cameraState.overlay_status === 'RUNNING' ? 'success' : cameraState.overlay_status === 'ERROR' ? 'error' : 'default'}>
                      Overlay {cameraState.overlay_status || 'DISABLED'}
                    </Tag>
                  </Space>
                  <Typography.Text type="secondary">抓帧：{formatRuntimeTime(cameraState.last_capture_at)}</Typography.Text>
                  <Typography.Text type="secondary">推理：{formatRuntimeTime(cameraState.last_inference_at)}</Typography.Text>
                  <Typography.Text type="secondary">检测数：{cameraState.detection_count ?? 0}</Typography.Text>
                  <Typography.Text copyable={Boolean(cameraState.overlay_play_url)} ellipsis>
                    推理流：{cameraState.overlay_play_url || '-'}
                  </Typography.Text>
                  <Typography.Text type={cameraState.last_error || cameraState.overlay_last_error ? 'danger' : 'secondary'} ellipsis>
                    错误：{cameraState.last_error || cameraState.overlay_last_error || '-'}
                  </Typography.Text>
                </Space>
              </Card>
            ))}
          </div>

          <Space align="center" wrap>
            <Typography.Text>日志筛选：</Typography.Text>
            <Select
              allowClear
              placeholder="全部摄像头"
              style={{ width: 260 }}
              value={runtimeCameraId}
              options={runtimeCameraOptions}
              onChange={(value) => setRuntimeCameraId(value)}
            />
          </Space>

          <div
            style={{
              minHeight: 320,
              maxHeight: '60vh',
              overflow: 'auto',
              borderRadius: 8,
              background: '#0b1020',
              color: '#d7e3ff',
              padding: 12,
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
              fontSize: 12,
              lineHeight: 1.7,
              whiteSpace: 'pre-wrap',
            }}
          >
            {runtimeLogs.length
              ? runtimeLogs
                  .map((item) => `[${formatRuntimeTime(item.timestamp)}] [${item.level}]${item.camera_id ? ` [${item.camera_id}]` : ''} ${item.message}`)
                  .join('\n')
              : runtimeLoading
                ? '加载中...'
                : '暂无运行日志'}
          </div>
        </Space>
      </Modal>

      <Modal
        title={editingId ? '修改算法任务' : '新增算法任务'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={submitForm}
        confirmLoading={submitting}
        width={980}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={defaultFormValues}>
          <Space style={{ display: 'flex' }} align="start" wrap>
            <Form.Item name="name" label="任务名称" rules={[{ required: true, message: '请输入任务名称' }]}>
              <Input style={{ width: 240 }} />
            </Form.Item>
            <Form.Item name="taskType" label="任务类型" rules={[{ required: true, message: '请选择任务类型' }]}>
              <Select style={{ width: 180 }} options={meta.taskTypes.map((item) => ({ label: enumLabel(item), value: enumValue(item) }))} />
            </Form.Item>
            <Form.Item name="frameInterval" label="抽帧间隔">
              <InputNumber style={{ width: 160 }} min={1} addonAfter="秒" disabled={isSnapshotTask} />
            </Form.Item>
          </Space>

          {taskType === 'SNAPSHOT' ? (
            <Card size="small" style={{ marginBottom: 16 }}>
              <Space direction="vertical" size={14} style={{ width: '100%' }}>
                <Alert
                  type="info"
                  showIcon
                  message="启动顺序提示"
                  description="如果当前抓拍任务关联的摄像头同时绑定了实时任务，建议先启动实时任务，再启动抓拍任务。抓拍会优先复用实时任务的推理流；若实时任务未启动，则自动回退到摄像头播放流。"
                />
                <Form.Item
                  name="cron"
                  label="Cron 表达式"
                  rules={[
                    { required: true, message: '请输入 Cron 表达式' },
                    {
                      validator: async (_, value) => {
                        if (!value || validateCronExpression(value)) {
                          return;
                        }
                        throw new Error('Cron 表达式格式不正确，请输入 6 或 7 位 Quartz 表达式');
                      },
                    },
                  ]}
                  extra="支持手动填写，也可使用下方生成器自动生成 Quartz Cron 表达式"
                >
                  <Input placeholder="例如：0 */5 * * * ?" />
                </Form.Item>
                <Space wrap align="end">
                  <div>
                    <Typography.Text type="secondary">生成方式</Typography.Text>
                    <br />
                    <Radio.Group
                      value={cronMode}
                      onChange={(event) => setCronMode(event.target.value)}
                      optionType="button"
                      buttonStyle="solid"
                      options={[
                        { label: '每隔分钟', value: 'MINUTE' },
                        { label: '每隔小时', value: 'HOUR' },
                        { label: '按天', value: 'DAY' },
                        { label: '按周', value: 'WEEK' },
                      ]}
                    />
                  </div>
                  <Form.Item label="间隔" style={{ marginBottom: 0 }}>
                    <InputNumber min={1} max={59} value={cronInterval} onChange={(value) => setCronInterval(value || 1)} />
                  </Form.Item>
                  {cronMode !== 'MINUTE' ? (
                    <Form.Item label="分钟" style={{ marginBottom: 0 }}>
                      <InputNumber min={0} max={59} value={cronMinute} onChange={(value) => setCronMinute(value || 0)} />
                    </Form.Item>
                  ) : null}
                  {cronMode === 'DAY' || cronMode === 'WEEK' ? (
                    <Form.Item label="小时" style={{ marginBottom: 0 }}>
                      <InputNumber min={0} max={23} value={cronHour} onChange={(value) => setCronHour(value || 0)} />
                    </Form.Item>
                  ) : null}
                  {cronMode === 'WEEK' ? (
                    <Form.Item label="星期" style={{ marginBottom: 0 }}>
                      <Select
                        style={{ width: 120 }}
                        value={cronWeekday}
                        onChange={setCronWeekday}
                        options={[
                          { label: '周日', value: 1 },
                          { label: '周一', value: 2 },
                          { label: '周二', value: 3 },
                          { label: '周三', value: 4 },
                          { label: '周四', value: 5 },
                          { label: '周五', value: 6 },
                          { label: '周六', value: 7 },
                        ]}
                      />
                    </Form.Item>
                  ) : null}
                  <Button type="primary" onClick={() => form.setFieldValue('cron', generatedCron)}>
                    生成并填入
                  </Button>
                </Space>
                <Typography.Text type="secondary">当前生成结果：{generatedCron}</Typography.Text>
                <Typography.Text type="secondary">抓拍任务只按 Cron 定时抓拍，不显示告警配置，也不会生成告警事件。</Typography.Text>
              </Space>
            </Card>
          ) : null}

          <Space style={{ display: 'flex' }} align="start" wrap>
            <Form.Item name="cameraIds" label="关联摄像头" rules={[{ required: true, message: '请选择摄像头' }]}>
              <Select
                mode="multiple"
                style={{ width: 320 }}
                options={meta.cameras.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
              />
            </Form.Item>
            {!isSnapshotTask ? (
              <Form.Item name="modelIds" label="关联模型" rules={[{ required: true, message: '请选择已部署模型' }]}>
                <Select
                  mode="multiple"
                  style={{ width: 320 }}
                  options={meta.models.map((item) => ({
                    label: `${item.name}${item.currentVersion ? ` (${item.currentVersion})` : ''}`,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            ) : null}
          </Space>

          {!isSnapshotTask ? (
            <Space style={{ display: 'flex' }} align="start" wrap>
              <Form.Item name="trackingEnabled" label="启用追踪" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="trackingSimilarityThreshold" label="追踪相似度">
                <InputNumber style={{ width: 160 }} min={0} max={1} step={0.05} />
              </Form.Item>
              <Form.Item name="trackingMaxAliveFrames" label="最大存活帧数">
                <InputNumber style={{ width: 160 }} min={1} />
              </Form.Item>
              <Form.Item name="trackingSmoothFactor" label="平滑系数">
                <InputNumber style={{ width: 160 }} min={0} max={1} step={0.05} />
              </Form.Item>
            </Space>
          ) : null}

          <Space style={{ display: 'flex' }} align="start" wrap>
            <Form.Item name="allDayArming" label="全天布防" valuePropName="checked">
              <Switch />
            </Form.Item>
            {!allDayArming ? (
              <>
                <Form.Item name="beginTime" label="开始时间" rules={[{ required: true, message: '请输入开始时间' }]}>
                  <Input style={{ width: 160 }} placeholder="08:00" />
                </Form.Item>
                <Form.Item name="endTime" label="结束时间" rules={[{ required: true, message: '请输入结束时间' }]}>
                  <Input style={{ width: 160 }} placeholder="18:00" />
                </Form.Item>
              </>
            ) : null}
          </Space>

          <Space style={{ display: 'flex' }} align="start" wrap>
            {!isSnapshotTask ? (
              <Form.Item name="alarmEnabled" label="启用告警" valuePropName="checked">
                <Switch />
              </Form.Item>
            ) : null}
            {!isSnapshotTask && alarmEnabled ? (
              <>
                <Form.Item name="alarmRecordSeconds" label="告警录像秒数">
                  <InputNumber style={{ width: 180 }} min={1} />
                </Form.Item>
                <Form.Item
                  name="sameAlarmWindowMinutes"
                  label="同类告警去重(分钟)"
                  extra="默认 5 分钟；同一摄像头、同一事件类型、同一告警内容在 N 分钟内只记录一次。"
                  rules={[
                    { required: true, message: '请输入同类告警去重分钟数' },
                    { type: 'number', min: 1, max: 60, message: '同类告警去重分钟数必须在1到60之间' },
                  ]}
                >
                  <InputNumber style={{ width: 220 }} min={1} max={60} precision={0} />
                </Form.Item>
              </>
            ) : null}
          </Space>

          {alarmEnabled && !isSnapshotTask ? (
            <Space style={{ display: 'flex' }} align="start" wrap>
              <Form.Item name="notifyEnabled" label="启用通知" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Space>
          ) : null}

          {alarmEnabled && !isSnapshotTask && notifyEnabled ? (
            <>
              <Form.Item name="notifyChannelIds" label="通知渠道" rules={[{ required: true, message: '请选择通知渠道' }]}>
                <Select
                  mode="multiple"
                  placeholder="请选择通知渠道"
                  value={notifyChannelIds}
                  options={meta.notifyChannels.map((item) => ({ label: item.name, value: item.id }))}
                  onChange={handleNotifyChannelsChange}
                />
              </Form.Item>
              {notifyChannelIds.map((channelId, index) => {
                const config = normalizeNotifyConfigs(notifyConfigs).find((item) => item.channelId === channelId) || {
                  channelId,
                  templateId: undefined,
                };
                const channel = meta.notifyChannels.find((item) => item.id === config.channelId);
                const templateOptions = meta.notifyTemplates
                  .filter((item) => item.channelId === config.channelId)
                  .map((item) => ({ label: item.name, value: item.id }));
                return (
                  <Card key={config.channelId} size="small" style={{ marginBottom: 12 }}>
                    <Space direction="vertical" size={12} style={{ width: '100%' }}>
                      <Typography.Text strong>{channel?.name || config.channelId}</Typography.Text>
                      <Form.Item
                        name={['notifyConfigs', index, 'templateId']}
                        label="通知模版"
                        rules={[{ required: true, message: '请选择通知模版' }]}
                        style={{ marginBottom: 0 }}
                      >
                        <Select placeholder="请选择模版" options={templateOptions} />
                      </Form.Item>
                    </Space>
                  </Card>
                );
              })}
              <Form.Item name="notifyUserIds" label="通知人员">
                <Select
                  mode="multiple"
                  placeholder="请选择通知人员"
                  optionFilterProp="label"
                  options={notifyUsers.map((item) => ({
                    label: `${item.username}${item.loginname ? ` (${item.loginname})` : ''}`,
                    value: item.id,
                  }))}
                />
              </Form.Item>
            </>
          ) : null}
        </Form>
      </Modal>

      <RegionConfigModal
        open={regionModalOpen}
        taskId={regionTaskId}
        meta={meta}
        onClose={closeRegionConfig}
        onSaved={handleRegionConfigSaved}
        onSnapshotPersisted={handleRegionSnapshotPersisted}
      />
    </>
  );
};

export default TaskPage;
