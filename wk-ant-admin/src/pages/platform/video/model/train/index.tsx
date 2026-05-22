import {
  AppstoreOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
  SnippetsOutlined,
  StopOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import {
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
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
  Table,
  InputNumber as AntInputNumber,
  Tabs,
  Tag,
  Checkbox,
  Tooltip,
  Typography,
} from 'antd';
import type { TabsProps, TableColumnsType } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import InferenceWorkbench from './inference';
import { ManagedFileImagePreview, VideoCoverUploadField, VideoPackageUploadField } from '../../shared';
import { getVideoCameraPage } from '@/services/budiot/video/camera';
import {
  createVideoModel,
  deleteVideoModel,
  getVideoModelDetail,
  getVideoModelMeta,
  getVideoModelPage,
  importVideoModelPackage,
  updateVideoModel,
} from '@/services/budiot/video/model';
import {
  createVideoDeployment,
  createVideoInferenceTask,
  deleteVideoDeployment,
  deleteVideoInferenceTask,
  getVideoDeploymentDetail,
  getVideoDeploymentPage,
  getVideoInferencePage,
  getVideoInferenceResult,
  getVideoTrainTaskPage,
  getVideoTrainMeta,
  operateVideoDeployment,
} from '@/services/budiot/video/train';
import type {
  VideoCameraRecord,
  VideoModelDeploymentRecord,
  VideoModelInferenceTaskRecord,
  VideoModelRecord,
  VideoModelTrainMeta,
  VideoModelTrainTaskRecord,
  VideoOption,
} from '@/services/budiot/video/typing';
import { uploadFile } from '@/utils/file';

type ModelFormValues = Partial<VideoModelRecord>;
type DeploymentFormValues = Partial<VideoModelDeploymentRecord>;
type InferenceFormValues = Partial<VideoModelInferenceTaskRecord>;
type ModelViewMode = 'table' | 'card';
type DeploymentViewMode = 'table' | 'card';
type InferenceSourceMode = 'IMAGE_UPLOAD' | 'VIDEO_UPLOAD' | 'CAMERA';
type UploadedInferenceSource = {
  fileId?: string;
  fileName?: string;
  sourceUri?: string;
  previewUrl?: string;
  mediaType?: 'image' | 'video';
};
type DeploymentGroupRecord = {
  modelId: string;
  modelName: string;
  records: VideoModelDeploymentRecord[];
  runningCount: number;
  stoppedCount: number;
};
type DeployableModelOption = {
  label: string;
  value: string;
};

const emptyTrainMeta: VideoModelTrainMeta = {
  models: [],
  datasets: [],
  trainStatuses: [],
  deploymentStatuses: [],
  inferenceTypes: [],
  inferenceStatuses: [],
};

const ModelTrainPage: React.FC = () => {
  const { modal, message } = App.useApp();
  const modelActionRef = React.useRef<ActionType>(null);
  const deploymentActionRef = React.useRef<ActionType>(null);
  const inferenceActionRef = React.useRef<ActionType>(null);
  const [activeTab, setActiveTab] = React.useState('model');
  const [modelForm] = Form.useForm<ModelFormValues>();
  const [deploymentForm] = Form.useForm<DeploymentFormValues>();
  const [inferenceForm] = Form.useForm<InferenceFormValues>();
  const [modelStatuses, setModelStatuses] = React.useState<VideoOption[]>([]);
  const [trainMeta, setTrainMeta] = React.useState<VideoModelTrainMeta>(emptyTrainMeta);
  const [modelViewMode, setModelViewMode] = React.useState<ModelViewMode>('card');
  const [deploymentViewMode, setDeploymentViewMode] = React.useState<DeploymentViewMode>('card');
  const [modelTableData, setModelTableData] = React.useState<VideoModelRecord[]>([]);
  const [modelPageInfo, setModelPageInfo] = React.useState({ current: 1, pageSize: 12, total: 0 });
  const [deploymentTableData, setDeploymentTableData] = React.useState<VideoModelDeploymentRecord[]>([]);
  const [deploymentGroupData, setDeploymentGroupData] = React.useState<DeploymentGroupRecord[]>([]);
  const [deploymentPageInfo, setDeploymentPageInfo] = React.useState({ current: 1, pageSize: 12, total: 0 });
  const [deploymentDrawerOpen, setDeploymentDrawerOpen] = React.useState(false);
  const [selectedDeploymentGroup, setSelectedDeploymentGroup] = React.useState<DeploymentGroupRecord>();
  const [deploymentInstanceData, setDeploymentInstanceData] = React.useState<VideoModelDeploymentRecord[]>([]);
  const [deploymentInstanceLoading, setDeploymentInstanceLoading] = React.useState(false);
  const [deploymentInstancePageInfo, setDeploymentInstancePageInfo] = React.useState({ current: 1, pageSize: 10, total: 0 });
  const [deployableModelOptions, setDeployableModelOptions] = React.useState<DeployableModelOption[]>([]);
  const [deploymentLogOpen, setDeploymentLogOpen] = React.useState(false);
  const [deploymentLogAutoRefresh, setDeploymentLogAutoRefresh] = React.useState(true);
  const [deploymentLogInterval, setDeploymentLogInterval] = React.useState(3);
  const [deploymentLogRecord, setDeploymentLogRecord] = React.useState<VideoModelDeploymentRecord>();
  const [deploymentLogContent, setDeploymentLogContent] = React.useState('');
  const [editingModelId, setEditingModelId] = React.useState<string>();
  const [modelOpen, setModelOpen] = React.useState(false);
  const [deploymentOpen, setDeploymentOpen] = React.useState(false);
  const [inferenceOpen, setInferenceOpen] = React.useState(false);
  const [inferenceResultOpen, setInferenceResultOpen] = React.useState(false);
  const [inferenceResult, setInferenceResult] = React.useState('');
  const [inferenceSourceMode, setInferenceSourceMode] = React.useState<InferenceSourceMode>('IMAGE_UPLOAD');
  const [inferenceSelectedModelId, setInferenceSelectedModelId] = React.useState<string>();
  const [inferenceSelectedDeploymentId, setInferenceSelectedDeploymentId] = React.useState<string>();
  const [inferenceDeploymentData, setInferenceDeploymentData] = React.useState<VideoModelDeploymentRecord[]>([]);
  const [inferenceHistoryRecords, setInferenceHistoryRecords] = React.useState<VideoModelInferenceTaskRecord[]>([]);
  const [inferenceHistoryId, setInferenceHistoryId] = React.useState<string>();
  const [inferenceCameraOptions, setInferenceCameraOptions] = React.useState<VideoCameraRecord[]>([]);
  const [inferenceCameraId, setInferenceCameraId] = React.useState<string>();
  const [inferenceUploadedSource, setInferenceUploadedSource] = React.useState<UploadedInferenceSource>();
  const [inferenceSubmitting, setInferenceSubmitting] = React.useState(false);
  const [showInferenceOriginal, setShowInferenceOriginal] = React.useState(true);
  const [inferenceResultPayload, setInferenceResultPayload] = React.useState<Record<string, any>>();
  const [inferenceResultSummary, setInferenceResultSummary] = React.useState('');
  const inferenceDeploymentRequestRef = React.useRef(0);

  const loadMeta = React.useCallback(async () => {
    const [modelMetaRes, trainMetaRes] = await Promise.all([getVideoModelMeta(), getVideoTrainMeta()]);
    setModelStatuses(modelMetaRes.data.statuses || []);
    setTrainMeta(trainMetaRes.data || emptyTrainMeta);
  }, []);

  React.useEffect(() => {
    loadMeta().catch(() => undefined);
  }, [loadMeta]);

  const resolveModelDisplayLabel = React.useCallback(
    (modelId?: string, modelName?: string) => {
      const model = trainMeta.models.find((item) => item.id === modelId);
      const displayName = model?.name || modelName || modelId || '-';
      return model?.currentVersion ? `${displayName} - (版本号: ${model.currentVersion})` : displayName;
    },
    [trainMeta.models],
  );

  const resolveModelCardTitle = React.useCallback(
    (modelId?: string, modelName?: string) =>
      trainMeta.models.find((item) => item.id === modelId)?.name || modelName || modelId || '-',
    [trainMeta.models],
  );

  const resolveModelVersionText = React.useCallback(
    (modelId?: string) => {
      const currentVersion = trainMeta.models.find((item) => item.id === modelId)?.currentVersion;
      return currentVersion ? `版本号：${currentVersion}` : '';
    },
    [trainMeta.models],
  );

  const openModelForm = React.useCallback(
    async (record?: VideoModelRecord) => {
      if (record) {
        const res = await getVideoModelDetail(record.id);
        setEditingModelId(record.id);
        modelForm.setFieldsValue(res.data);
      } else {
        setEditingModelId(undefined);
        modelForm.resetFields();
        modelForm.setFieldsValue({ status: String(modelStatuses[0]?.value || 'TRAINING') });
      }
      setModelOpen(true);
    },
    [modelForm, modelStatuses],
  );

  const handleDeleteModel = React.useCallback(
    (record: VideoModelRecord) => {
      modal.confirm({
        title: `确认删除模型“${record.name}”吗？`,
        onOk: async () => {
          await deleteVideoModel(record.id);
          await Promise.all([loadMeta(), Promise.resolve(modelActionRef.current?.reload())]);
        },
      });
    },
    [loadMeta, modal],
  );

  const renderModelActions = React.useCallback(
    (record: VideoModelRecord) => (
      <Space>
        <Button type="link" icon={<EyeOutlined />} onClick={() => history.push(`/platform/video/model/train/${record.id}`)}>
          详情
        </Button>
        <Button type="link" icon={<EditOutlined />} onClick={() => openModelForm(record).catch(() => undefined)}>
          修改
        </Button>
        <Button danger type="link" icon={<DeleteOutlined />} onClick={() => handleDeleteModel(record)}>
          删除
        </Button>
      </Space>
    ),
    [handleDeleteModel, openModelForm],
  );

  const modelColumns = React.useMemo<ProColumns<VideoModelRecord>[]>(
    () => [
      { title: '模型名称', dataIndex: 'name' },
      {
        title: '状态',
        dataIndex: 'status',
        valueType: 'select',
        fieldProps: {
          options: modelStatuses.map((item) => ({ label: item.text, value: item.value })),
          allowClear: true,
        },
        render: (_, record) => (
          <Tag color={record.status === 'ONLINE' ? 'success' : 'blue'}>
            {modelStatuses.find((item) => item.value === record.status)?.text || record.status}
          </Tag>
        ),
      },
      {
        title: '封面',
        dataIndex: 'coverFileId',
        search: false,
        width: 90,
        render: (_, record) => (
          <div style={{ width: 56, height: 56, overflow: 'hidden', borderRadius: 8 }}>
            <ManagedFileImagePreview
              fileId={record.coverFileId}
              alt={record.name}
              style={{ width: 56, height: 56, objectFit: 'cover' }}
            />
          </div>
        ),
      },
      { title: '当前版本', dataIndex: 'currentVersion', search: false, render: (_, record) => record.currentVersion || '-' },
      { title: '描述', dataIndex: 'description', search: false, ellipsis: true },
      {
        title: '操作',
        valueType: 'option',
        width: 220,
        render: (_, record) => renderModelActions(record),
      },
    ],
    [modelStatuses, renderModelActions],
  );

  const requestModels = React.useCallback(async (params: Record<string, any>) => {
    const res = await getVideoModelPage({
      name: params.name,
      status: params.status,
      pageNo: params.current,
      pageSize: params.pageSize,
    });
    const list = res.data.list || [];
    setModelTableData(list);
    setModelPageInfo({
      current: params.current || 1,
      pageSize: params.pageSize || 12,
      total: res.data.totalCount || 0,
    });
    return {
      data: list,
      total: res.data.totalCount || 0,
      success: true,
    };
  }, []);

  const requestDeployments = React.useCallback(async (params: Record<string, any>) => {
    const res = await getVideoDeploymentPage({
      modelId: params.modelId,
      status: params.status,
      pageNo: 1,
      pageSize: 1000,
    });
    const list = res.data?.list || [];
    setDeploymentTableData(list);
    const grouped = Array.from(
      list.reduce<Map<string, DeploymentGroupRecord>>((acc, record) => {
        const key = record.modelId || 'unknown';
        if (!acc.has(key)) {
          acc.set(key, {
            modelId: key,
            modelName: record.modelName || record.modelId,
            records: [],
            runningCount: 0,
            stoppedCount: 0,
          });
        }
        const current = acc.get(key)!;
        current.records.push(record);
        if (record.status === 'RUNNING') {
          current.runningCount += 1;
        } else {
          current.stoppedCount += 1;
        }
        return acc;
      }, new Map()),
    ).map(([, value]) => value);
    const current = params.current || 1;
    const pageSize = params.pageSize || 12;
    const startIndex = (current - 1) * pageSize;
    const pageData = grouped.slice(startIndex, startIndex + pageSize);
    setDeploymentGroupData(pageData);
    setDeploymentPageInfo({
      current,
      pageSize,
      total: grouped.length,
    });
    return { data: pageData, total: grouped.length, success: true };
  }, []);

  const loadDeployableModelOptions = React.useCallback(async () => {
    const res = await getVideoTrainTaskPage({
      status: 'COMPLETED',
      pageNo: 1,
      pageSize: 1000,
    });
    const latestByModel = new Map<string, VideoModelTrainTaskRecord>();
    (res.data.list || []).forEach((item) => {
      const current = latestByModel.get(item.modelId);
      if (!current || (item.createdAt || 0) > (current.createdAt || 0)) {
        latestByModel.set(item.modelId, item);
      }
    });
    const options = Array.from(latestByModel.values())
      .map((task) => {
        const model = trainMeta.models.find((item) => item.id === task.modelId);
        if (!model?.id) {
          return null;
        }
        return {
          label: resolveModelDisplayLabel(model.id, task.modelName),
          value: model.id,
        };
      })
      .filter((item): item is DeployableModelOption => Boolean(item));
    setDeployableModelOptions(options);
    return options;
  }, [resolveModelDisplayLabel, trainMeta.models]);

  const runningInferenceDeployments = React.useMemo(
    () =>
      inferenceDeploymentData.filter(
        (item) =>
          item.status === 'RUNNING' && (!inferenceSelectedModelId || item.modelId === inferenceSelectedModelId),
      ),
    [inferenceDeploymentData, inferenceSelectedModelId],
  );

  const loadInferenceDeployments = React.useCallback(async (modelId?: string) => {
    const requestId = ++inferenceDeploymentRequestRef.current;
    const deploymentRes = await getVideoDeploymentPage({
      modelId,
      pageNo: 1,
      pageSize: 1000,
    });
    if (requestId !== inferenceDeploymentRequestRef.current) {
      return;
    }
    setInferenceDeploymentData(deploymentRes.data?.list || []);
  }, []);

  const loadInferenceHistory = React.useCallback(async () => {
    const inferenceRes = await getVideoInferencePage({
      pageNo: 1,
      pageSize: 100,
      pageOrderName: 'createdAt',
      pageOrderBy: 'desc',
    });
    setInferenceHistoryRecords(inferenceRes.data?.list || []);
  }, []);

  const loadInferenceCameras = React.useCallback(async () => {
    const cameraRes = await getVideoCameraPage({ pageNo: 1, pageSize: 200 });
    setInferenceCameraOptions(cameraRes.data?.list || []);
  }, []);

  const refreshInferenceModelAndServices = React.useCallback(async (modelId?: string) => {
    const targetModelId = modelId ?? inferenceSelectedModelId;
    await loadMeta();
    if (targetModelId) {
      await loadInferenceDeployments(targetModelId);
    } else {
      setInferenceDeploymentData([]);
      setInferenceSelectedDeploymentId(undefined);
    }
  }, [inferenceSelectedModelId, loadInferenceDeployments, loadMeta]);

  const refreshInferenceServices = React.useCallback(async (modelId?: string) => {
    const targetModelId = modelId ?? inferenceSelectedModelId;
    if (targetModelId) {
      await loadInferenceDeployments(targetModelId);
    }
  }, [inferenceSelectedModelId, loadInferenceDeployments]);

  const handleInferenceModelChange = React.useCallback(
    async (value?: string) => {
      setInferenceSelectedModelId(value);
      setInferenceSelectedDeploymentId(undefined);
      setInferenceHistoryId(undefined);
      setInferenceResultPayload(undefined);
      setInferenceResultSummary('');
      if (!value) {
        inferenceDeploymentRequestRef.current += 1;
        setInferenceDeploymentData([]);
        return;
      }
      await refreshInferenceServices(value);
    },
    [refreshInferenceServices],
  );

  React.useEffect(() => {
    if (activeTab !== 'inference') {
      return;
    }
    loadInferenceHistory().catch(() => undefined);
  }, [activeTab, loadInferenceHistory]);

  React.useEffect(() => {
    if (activeTab !== 'inference' || inferenceSelectedModelId) {
      return;
    }
    inferenceDeploymentRequestRef.current += 1;
    setInferenceDeploymentData([]);
    setInferenceSelectedDeploymentId(undefined);
  }, [activeTab, inferenceSelectedModelId]);

  React.useEffect(() => {
    if (activeTab !== 'inference' || inferenceSourceMode !== 'CAMERA') {
      return;
    }
    loadInferenceCameras().catch(() => undefined);
  }, [activeTab, inferenceSourceMode, loadInferenceCameras]);

  React.useEffect(() => {
    if (inferenceSourceMode !== 'IMAGE_UPLOAD') {
      setInferenceSelectedDeploymentId(undefined);
      return;
    }
    if (!runningInferenceDeployments.length) {
      setInferenceSelectedDeploymentId(undefined);
      return;
    }
    const matched = runningInferenceDeployments.find((item) => item.id === inferenceSelectedDeploymentId);
    if (!matched) {
      setInferenceSelectedDeploymentId(runningInferenceDeployments[0].id);
    }
  }, [inferenceSelectedDeploymentId, inferenceSourceMode, runningInferenceDeployments]);

  const handleInferenceSourceUpload = React.useCallback(
    async (file: File, mediaType: 'image' | 'video') => {
      const previewUrl = URL.createObjectURL(file);
      const formData = new FormData();
      formData.append('Filedata', file);
      const response = await uploadFile(formData, { type: mediaType });
      const payload = (response.data || {}) as { id?: string; url?: string };
      if (!payload.url) {
        throw new Error('上传文件未返回访问地址');
      }
      setInferenceUploadedSource({
        fileId: payload.id,
        fileName: file.name,
        sourceUri: payload.url,
        previewUrl,
        mediaType,
      });
      setInferenceHistoryId(undefined);
      setShowInferenceOriginal(true);
      setInferenceResultPayload(undefined);
      setInferenceResultSummary('');
    },
    [],
  );

  const applyInferenceRecord = React.useCallback(
    async (record: VideoModelInferenceTaskRecord) => {
      const resultRes = await getVideoInferenceResult(record.id);
      let parsed: Record<string, any> = {};
      try {
        parsed = resultRes.data?.result ? JSON.parse(resultRes.data.result) : {};
      } catch (error) {
        parsed = {};
      }
      setInferenceSelectedModelId(record.modelId);
      setInferenceSourceMode(record.taskType === 'VIDEO' ? 'VIDEO_UPLOAD' : 'IMAGE_UPLOAD');
      setInferenceResultPayload(parsed);
      setInferenceResultSummary(record.resultSummary || '');
      await loadInferenceDeployments(record.modelId);
      setInferenceUploadedSource({
        sourceUri: record.sourceUri,
        mediaType: record.taskType === 'VIDEO' ? 'video' : 'image',
      });
    },
    [loadInferenceDeployments],
  );

  const refreshDeploymentLog = React.useCallback(async (record: VideoModelDeploymentRecord) => {
    const res = await getVideoDeploymentDetail(record.id);
    setDeploymentLogRecord(res.data);
    setDeploymentLogContent(res.data.runtimeLog || '');
    return res.data;
  }, []);

  const loadDeploymentInstances = React.useCallback(
    async (modelId: string, current = 1, pageSize = 10) => {
      setDeploymentInstanceLoading(true);
      try {
        const res = await getVideoDeploymentPage({
          modelId,
          pageNo: 1,
          pageSize: 1000,
        });
        const list = res.data.list || [];
        setDeploymentInstanceData(list);
        setDeploymentInstancePageInfo({
          current,
          pageSize,
          total: list.length,
        });
      } finally {
        setDeploymentInstanceLoading(false);
      }
    },
    [],
  );

  const openDeploymentDrawer = React.useCallback(
    (group: DeploymentGroupRecord) => {
      setSelectedDeploymentGroup(group);
      setDeploymentDrawerOpen(true);
    },
    [],
  );

  const refreshDeploymentViews = React.useCallback(
    async (modelId?: string, nextCurrent?: number, nextPageSize?: number) => {
      await Promise.all([Promise.resolve(deploymentActionRef.current?.reload()), loadMeta()]);
      if (modelId && selectedDeploymentGroup?.modelId === modelId) {
        await loadDeploymentInstances(
          modelId,
          nextCurrent ?? deploymentInstancePageInfo.current,
          nextPageSize ?? deploymentInstancePageInfo.pageSize,
        );
      }
    },
    [
      deploymentInstancePageInfo.current,
      deploymentInstancePageInfo.pageSize,
      loadDeploymentInstances,
      loadMeta,
      selectedDeploymentGroup?.modelId,
    ],
  );

  const handleOperateDeployment = React.useCallback(
    async (record: VideoModelDeploymentRecord, action: 'START' | 'STOP' | 'RESTART') => {
      await operateVideoDeployment({ id: record.id, action });
      await refreshDeploymentViews(record.modelId);
      if (deploymentLogRecord?.id === record.id) {
        await refreshDeploymentLog(record);
      }
    },
    [
      deploymentLogRecord?.id,
      refreshDeploymentLog,
      refreshDeploymentViews,
    ],
  );

  const handleDeleteDeployment = React.useCallback(
    (record: VideoModelDeploymentRecord) => {
      modal.confirm({
        title: '确认删除部署实例吗？',
        onOk: async () => {
          await deleteVideoDeployment(record.id);
          const nextCurrent =
            selectedDeploymentGroup?.modelId === record.modelId &&
            deploymentInstanceData.length === 1 &&
            deploymentInstancePageInfo.current > 1
              ? deploymentInstancePageInfo.current - 1
              : deploymentInstancePageInfo.current;
          await refreshDeploymentViews(record.modelId, nextCurrent);
          if (selectedDeploymentGroup?.modelId === record.modelId) {
            setDeploymentInstancePageInfo((prev) => ({
              ...prev,
              current: nextCurrent,
            }));
          }
        },
      });
    },
    [
      deploymentInstanceData.length,
      deploymentInstancePageInfo.current,
      deploymentInstancePageInfo.pageSize,
      modal,
      refreshDeploymentViews,
      selectedDeploymentGroup?.modelId,
    ],
  );

  const handleBatchOperateDeployment = React.useCallback(
    async (group: DeploymentGroupRecord, action: 'START' | 'STOP' | 'RESTART') => {
      const targets = group.records.filter((record) =>
        action === 'START' ? record.status !== 'RUNNING' : record.status === 'RUNNING',
      );
      if (!targets.length) {
        return;
      }
      await Promise.all(targets.map((record) => operateVideoDeployment({ id: record.id, action })));
      await refreshDeploymentViews(group.modelId);
    },
    [refreshDeploymentViews],
  );

  const handleBatchDeleteDeployment = React.useCallback(
    (group: DeploymentGroupRecord) => {
      modal.confirm({
        title: `确认删除“${resolveModelDisplayLabel(group.modelId, group.modelName)}”下的全部部署实例吗？`,
        onOk: async () => {
          await Promise.all(group.records.map((record) => deleteVideoDeployment(record.id)));
          await refreshDeploymentViews(group.modelId, 1);
          if (selectedDeploymentGroup?.modelId === group.modelId) {
            setDeploymentInstancePageInfo((prev) => ({
              ...prev,
              current: 1,
            }));
          }
        },
      });
    },
    [modal, refreshDeploymentViews, resolveModelDisplayLabel, selectedDeploymentGroup?.modelId],
  );

  const openDeploymentLog = React.useCallback(
    async (record: VideoModelDeploymentRecord) => {
      setDeploymentLogRecord(record);
      setDeploymentLogOpen(true);
      await refreshDeploymentLog(record);
    },
    [refreshDeploymentLog],
  );

  const deploymentColumns = React.useMemo<ProColumns<DeploymentGroupRecord>[]>(
    () => [
      {
        title: '模型名称',
        dataIndex: 'modelId',
        valueType: 'select',
        fieldProps: {
          options: trainMeta.models.map((item) => ({ label: item.name, value: item.id })),
          allowClear: true,
        },
        render: (_, record) => resolveModelDisplayLabel(record.modelId, record.modelName),
      },
      {
        title: '副本数',
        dataIndex: 'records',
        search: false,
        render: (_, record) => (
            <Button
              type="link"
              style={{ paddingInline: 0 }}
              onClick={() => {
                openDeploymentDrawer(record);
              }}
            >
            {record.records.length}
          </Button>
        ),
      },
      {
        title: '状态',
        dataIndex: 'runningCount',
        search: false,
        render: (_, record) => (
          <Space wrap>
            <Tag color="success">运行中：{record.runningCount}</Tag>
            <Tag>已停止：{record.stoppedCount}</Tag>
          </Space>
        ),
      },
      {
        title: '操作',
        valueType: 'option',
        width: 220,
        render: (_, record) => (
          <Space size={4} wrap>
            <Tooltip title="查看实例">
              <Button
                type="link"
                icon={<EyeOutlined />}
                onClick={() => {
                  openDeploymentDrawer(record);
                }}
              />
            </Tooltip>
            <Tooltip title="批量启动">
              <Button
                type="link"
                icon={<PlayCircleOutlined />}
                disabled={record.stoppedCount === 0}
                onClick={() => handleBatchOperateDeployment(record, 'START')}
              />
            </Tooltip>
            <Tooltip title="批量重启">
              <Button
                type="link"
                icon={<ReloadOutlined />}
                disabled={record.runningCount === 0}
                onClick={() => handleBatchOperateDeployment(record, 'RESTART')}
              />
            </Tooltip>
            <Tooltip title="批量停止">
              <Button
                type="link"
                icon={<StopOutlined />}
                disabled={record.runningCount === 0}
                onClick={() => handleBatchOperateDeployment(record, 'STOP')}
              />
            </Tooltip>
            <Tooltip title="批量删除">
              <Button
                danger
                type="link"
                icon={<DeleteOutlined />}
                disabled={record.records.length === 0}
                onClick={() => handleBatchDeleteDeployment(record)}
              />
            </Tooltip>
          </Space>
        ),
      },
    ],
    [
      handleBatchDeleteDeployment,
      handleBatchOperateDeployment,
      openDeploymentDrawer,
      resolveModelDisplayLabel,
      trainMeta.deploymentStatuses,
      trainMeta.models,
    ],
  );

  const resolveDeploymentHost = React.useCallback((serviceUrl?: string) => {
    try {
      return serviceUrl ? new URL(serviceUrl).hostname || '-' : '-';
    } catch {
      return serviceUrl || '-';
    }
  }, []);

  const deploymentInstanceColumns = React.useMemo<TableColumnsType<VideoModelDeploymentRecord>>(
    () => [
      {
        title: '实例名称',
        dataIndex: 'instanceName',
        width: 180,
        ellipsis: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        render: (_, record) => (
          <Tag color={record.status === 'RUNNING' ? 'success' : 'default'}>
            {trainMeta.deploymentStatuses.find((item) => item.value === record.status)?.text || record.status}
          </Tag>
        ),
      },
      {
        title: 'IP',
        dataIndex: 'serviceUrl',
        width: 140,
        render: (_, record) => resolveDeploymentHost(record.serviceUrl),
      },
      {
        title: '端口',
        dataIndex: 'servicePort',
        width: 90,
      },
      {
        title: '接口地址',
        dataIndex: 'serviceUrl',
        ellipsis: true,
      },
      {
        title: '部署时间',
        dataIndex: 'createdAt',
        width: 180,
        render: (_, record) => (record.createdAt ? new Date(record.createdAt).toLocaleString() : '-'),
      },
      {
        title: '标记',
        dataIndex: 'grayRelease',
        width: 90,
        render: (_, record) => (record.grayRelease ? <Tag color="gold">灰度</Tag> : <Tag>正式</Tag>),
      },
      {
        title: '操作',
        key: 'actions',
        fixed: 'right',
        width: 170,
        render: (_, record) => (
          <Space wrap size={4}>
            {record.status === 'RUNNING' ? (
              <>
                <Tooltip title="停止">
                  <Button
                    size="small"
                    shape="circle"
                    icon={<StopOutlined />}
                    onClick={() => handleOperateDeployment(record, 'STOP')}
                  />
                </Tooltip>
                <Tooltip title="重启">
                  <Button
                    size="small"
                    shape="circle"
                    icon={<ReloadOutlined />}
                    onClick={() => handleOperateDeployment(record, 'RESTART')}
                  />
                </Tooltip>
              </>
            ) : (
              <Tooltip title="启动">
                <Button
                  size="small"
                  shape="circle"
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleOperateDeployment(record, 'START')}
                />
              </Tooltip>
            )}
            <Tooltip title="查看日志">
              <Button
                size="small"
                shape="circle"
                icon={<SnippetsOutlined />}
                onClick={() => openDeploymentLog(record)}
              />
            </Tooltip>
            <Tooltip title="删除">
              <Button
                danger
                size="small"
                shape="circle"
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteDeployment(record)}
              />
            </Tooltip>
          </Space>
        ),
      },
    ],
    [
      handleDeleteDeployment,
      handleOperateDeployment,
      openDeploymentLog,
      resolveDeploymentHost,
      trainMeta.deploymentStatuses,
    ],
  );

  React.useEffect(() => {
    if (!deploymentDrawerOpen || !selectedDeploymentGroup?.modelId) {
      return;
    }
    loadDeploymentInstances(
      selectedDeploymentGroup.modelId,
      deploymentInstancePageInfo.current,
      deploymentInstancePageInfo.pageSize,
    ).catch(() => undefined);
  }, [
    deploymentDrawerOpen,
    deploymentInstancePageInfo.current,
    deploymentInstancePageInfo.pageSize,
    loadDeploymentInstances,
    selectedDeploymentGroup?.modelId,
  ]);

  React.useEffect(() => {
    if (!deploymentLogOpen || !deploymentLogRecord || !deploymentLogAutoRefresh) {
      return;
    }
    const delay = Math.max(1, deploymentLogInterval) * 1000;
    const timer = window.setInterval(() => {
      refreshDeploymentLog(deploymentLogRecord).catch(() => undefined);
      deploymentActionRef.current?.reload();
    }, delay);
    return () => window.clearInterval(timer);
  }, [deploymentLogAutoRefresh, deploymentLogInterval, deploymentLogOpen, deploymentLogRecord, refreshDeploymentLog]);

  const inferenceColumns = React.useMemo<ProColumns<VideoModelInferenceTaskRecord>[]>(
    () => [
      { title: '任务名称', dataIndex: 'taskName' },
      {
        title: '模型',
        dataIndex: 'modelId',
        valueType: 'select',
        fieldProps: {
          options: trainMeta.models.map((item) => ({ label: item.name, value: item.id })),
          allowClear: true,
        },
        render: (_, record) => record.modelName || record.modelId,
      },
      {
        title: '类型',
        dataIndex: 'taskType',
        valueType: 'select',
        fieldProps: {
          options: trainMeta.inferenceTypes.map((item) => ({ label: item.text, value: item.value })),
          allowClear: true,
        },
        render: (_, record) => trainMeta.inferenceTypes.find((item) => item.value === record.taskType)?.text || record.taskType,
      },
      { title: '版本', dataIndex: 'version', search: false },
      { title: '输入资源', dataIndex: 'sourceUri', search: false, ellipsis: true },
      {
        title: '状态',
        dataIndex: 'status',
        valueType: 'select',
        fieldProps: {
          options: trainMeta.inferenceStatuses.map((item) => ({ label: item.text, value: item.value })),
          allowClear: true,
        },
        render: (_, record) => (
          <Tag color={record.status === 'COMPLETED' ? 'success' : 'processing'}>
            {trainMeta.inferenceStatuses.find((item) => item.value === record.status)?.text || record.status}
          </Tag>
        ),
      },
      { title: '摘要', dataIndex: 'resultSummary', search: false },
      {
        title: '操作',
        valueType: 'option',
        width: 260,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'result',
                label: '结果',
                icon: <EyeOutlined />,
                onClick: async () => {
                  const res = await getVideoInferenceResult(record.id);
                  setInferenceResult(res.data.result || '');
                  setInferenceResultOpen(true);
                },
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                onClick: () => {
                  modal.confirm({
                    title: '确认删除推理任务吗？',
                    onOk: async () => {
                      await deleteVideoInferenceTask(record.id);
                      inferenceActionRef.current?.reload();
                    },
                  });
                },
              },
            ]}
          />
        ),
      },
    ],
    [modal, trainMeta.inferenceStatuses, trainMeta.inferenceTypes, trainMeta.models],
  );

  const selectedInferenceDeployment = React.useMemo(
    () => inferenceDeploymentData.find((item) => item.id === inferenceSelectedDeploymentId),
    [inferenceDeploymentData, inferenceSelectedDeploymentId],
  );

  const selectedInferenceCamera = React.useMemo(
    () => inferenceCameraOptions.find((item) => item.id === inferenceCameraId),
    [inferenceCameraId, inferenceCameraOptions],
  );

  const handleStartInference = React.useCallback(async () => {
    const modelId = inferenceSelectedDeploymentId ? selectedInferenceDeployment?.modelId : inferenceSelectedModelId;
    const version = inferenceSourceMode === 'IMAGE_UPLOAD' ? selectedInferenceDeployment?.version : undefined;
    if (!modelId) {
      message.warning('请选择模型');
      return;
    }
    if (inferenceSourceMode === 'IMAGE_UPLOAD' && !inferenceSelectedDeploymentId) {
      message.warning('请选择模型服务');
      return;
    }
    let sourceUri = inferenceUploadedSource?.sourceUri || '';
    let taskType = inferenceSourceMode === 'VIDEO_UPLOAD' ? 'VIDEO' : 'IMAGE';
    if (inferenceSourceMode === 'CAMERA') {
      if (!selectedInferenceCamera) {
        message.warning('请选择摄像头');
        return;
      }
      sourceUri = selectedInferenceCamera.snapshotUrl || selectedInferenceCamera.playUrl || '';
      taskType = selectedInferenceCamera.snapshotUrl ? 'IMAGE' : 'VIDEO';
    }
    if (!sourceUri) {
      message.warning('请先选择输入源');
      return;
    }
    setInferenceSubmitting(true);
    try {
      const res = await createVideoInferenceTask({
        taskName: `推理-${Date.now()}`,
        modelId,
        version,
        taskType,
        sourceUri,
      });
      let parsed: Record<string, any> = {};
      try {
        parsed = res.data.resultJson ? JSON.parse(res.data.resultJson) : {};
      } catch (error) {
        parsed = {};
      }
      setInferenceResultPayload(parsed);
      setInferenceResultSummary(res.data.resultSummary || '');
      setInferenceHistoryId(res.data.id);
      await loadInferenceHistory();
    } finally {
      setInferenceSubmitting(false);
    }
  }, [
    createVideoInferenceTask,
    inferenceSelectedDeploymentId,
    inferenceSelectedModelId,
    inferenceSourceMode,
    inferenceUploadedSource?.sourceUri,
    loadInferenceHistory,
    message,
    selectedInferenceCamera,
    selectedInferenceDeployment?.modelId,
    selectedInferenceDeployment?.version,
  ]);

  const tabItems = React.useMemo<TabsProps['items']>(
    () => [
      {
        key: 'model',
        label: '模型管理',
        children: (
          <PlatformProTable<VideoModelRecord, Record<string, any>>
            actionRef={modelActionRef}
            persistenceKey="video-model-table"
            rowKey="id"
            headerTitle={<Typography.Text strong>模型管理</Typography.Text>}
            columns={modelColumns}
            request={requestModels}
            toolBarRender={() => [
              <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => openModelForm().catch(() => undefined)}>
                新增模型
              </Button>,
              <Radio.Group
                key="view-mode"
                value={modelViewMode}
                onChange={(event) => setModelViewMode(event.target.value)}
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
              modelViewMode === 'table' ? (
                defaultDom
              ) : (
                <>
                    {domList.toolbar}
                    {domList.alert}
                  <Card>
                    {modelTableData.length ? (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                          gap: 16,
                        }}
                      >
                        {modelTableData.map((record) => (
                          <Card
                            key={record.id}
                            hoverable
                            onClick={() => history.push(`/platform/video/model/train/${record.id}`)}
                            styles={{ body: { padding: 18 } }}
                          >
                            <Space direction="vertical" size={14} style={{ width: '100%' }}>
                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                                <div>
                                  <Typography.Title level={5} style={{ margin: 0 }}>
                                    {record.name}
                                  </Typography.Title>
                                  <Typography.Text type="secondary">当前版本：{record.currentVersion || '-'}</Typography.Text>
                                </div>
                                <div style={{ width: 80, height: 56, overflow: 'hidden', borderRadius: 8 }}>
                                  <ManagedFileImagePreview
                                    fileId={record.coverFileId}
                                    alt={record.name}
                                    style={{ width: 80, height: 56, objectFit: 'cover' }}
                                  />
                                </div>
                              </div>
                              <Tag color={record.status === 'ONLINE' ? 'success' : 'blue'} style={{ width: 'fit-content' }}>
                                {modelStatuses.find((item) => item.value === record.status)?.text || record.status}
                              </Tag>
                              <Typography.Text type="secondary">{record.description || '暂无描述'}</Typography.Text>
                              <Space wrap>
                                <Button
                                  type="link"
                                  size="small"
                                  icon={<EyeOutlined />}
                                  style={{ paddingInline: 0 }}
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    history.push(`/platform/video/model/train/${record.id}`);
                                  }}
                                >
                                  详情
                                </Button>
                                <Button
                                  type="link"
                                  size="small"
                                  icon={<EditOutlined />}
                                  style={{ paddingInline: 0 }}
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    openModelForm(record).catch(() => undefined);
                                  }}
                                >
                                  修改
                                </Button>
                                <Button
                                  type="link"
                                  size="small"
                                  danger
                                  icon={<DeleteOutlined />}
                                  style={{ paddingInline: 0 }}
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleDeleteModel(record);
                                  }}
                                >
                                  删除
                                </Button>
                              </Space>
                            </Space>
                          </Card>
                        ))}
                      </div>
                    ) : (
                      <Empty description="暂无数据" />
                    )}
                    <Pagination
                      style={{ marginTop: 16, textAlign: 'right' }}
                      current={modelPageInfo.current}
                      pageSize={modelPageInfo.pageSize}
                      total={modelPageInfo.total}
                      showSizeChanger
                      onChange={(current, pageSize) => {
                        modelActionRef.current?.setPageInfo?.({ current, pageSize });
                      }}
                    />
                  </Card>
                </>
              )
            }
          />
        ),
      },
      {
        key: 'inference',
        label: '模型推理',
        children: (
          <InferenceWorkbench />
        ),
      },
      {
        key: 'deployment',
        label: '模型部署',
        children: (
          <PlatformProTable<DeploymentGroupRecord, Record<string, any>>
            actionRef={deploymentActionRef}
            persistenceKey="video-deployment-table"
            rowKey="modelId"
            headerTitle={<Typography.Text strong>模型部署</Typography.Text>}
            columns={deploymentColumns}
            request={requestDeployments}
            toolBarRender={() => [
              <Button
                key="create"
                type="primary"
                icon={<PlusOutlined />}
                onClick={async () => {
                  const options = await loadDeployableModelOptions();
                  if (!options.length) {
                    modal.warning({
                      title: '无版本可用',
                      content: '当前没有模型存在已完成训练的版本，无法新增部署。',
                    });
                    return;
                  }
                  deploymentForm.resetFields();
                  deploymentForm.setFieldsValue({
                    modelId: options[0].value,
                    servicePort: 19100,
                    grayRelease: false,
                  });
                  setDeploymentOpen(true);
                }}
              >
                新增部署
              </Button>,
              <Radio.Group
                key="deployment-view-mode"
                value={deploymentViewMode}
                onChange={(event) => setDeploymentViewMode(event.target.value)}
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
              deploymentViewMode === 'table' ? (
                defaultDom
              ) : (
                <>
                  {domList.toolbar}
                  {domList.alert}
                  <Card>
                    {deploymentGroupData.length ? (
                      <>
                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))',
                            gap: 16,
                          }}
                        >
                          {deploymentGroupData.map((group) => (
                            <Card key={group.modelId} styles={{ body: { padding: 18 } }}>
                              <Space direction="vertical" size={14} style={{ width: '100%' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                                  <div>
                                    <Typography.Title level={5} style={{ margin: 0 }}>
                                      {resolveModelCardTitle(group.modelId, group.modelName)}
                                    </Typography.Title>
                                    <Typography.Text
                                      type="secondary"
                                      style={{ display: 'block', marginTop: 6, minHeight: 22 }}
                                    >
                                      {resolveModelVersionText(group.modelId) || ' '}
                                    </Typography.Text>
                                     <Button
                                       type="link"
                                       style={{ paddingInline: 0, marginTop: 4 }}
                                       onClick={() => {
                                         openDeploymentDrawer(group);
                                       }}
                                     >
                                      副本数：{group.records.length}
                                    </Button>
                                  </div>
                                  <Space direction="vertical" size={8} align="end">
                                    <Tag color="success">运行中：{group.runningCount}</Tag>
                                    <Tag>已停止：{group.stoppedCount}</Tag>
                                  </Space>
                                </div>
                                <Space wrap size={4}>
                                  <Tooltip title="查看实例">
                                    <Button
                                      shape="circle"
                                      icon={<EyeOutlined />}
                                      onClick={() => {
                                        openDeploymentDrawer(group);
                                      }}
                                    />
                                  </Tooltip>
                                  <Tooltip title="批量启动">
                                    <Button
                                      shape="circle"
                                      icon={<PlayCircleOutlined />}
                                      disabled={group.stoppedCount === 0}
                                      onClick={() => handleBatchOperateDeployment(group, 'START')}
                                    />
                                  </Tooltip>
                                  <Tooltip title="批量重启">
                                    <Button
                                      shape="circle"
                                      icon={<ReloadOutlined />}
                                      disabled={group.runningCount === 0}
                                      onClick={() => handleBatchOperateDeployment(group, 'RESTART')}
                                    />
                                  </Tooltip>
                                  <Tooltip title="批量停止">
                                    <Button
                                      shape="circle"
                                      icon={<StopOutlined />}
                                      disabled={group.runningCount === 0}
                                      onClick={() => handleBatchOperateDeployment(group, 'STOP')}
                                    />
                                  </Tooltip>
                                  <Tooltip title="批量删除">
                                    <Button
                                      danger
                                      shape="circle"
                                      icon={<DeleteOutlined />}
                                      disabled={group.records.length === 0}
                                      onClick={() => handleBatchDeleteDeployment(group)}
                                    />
                                  </Tooltip>
                                </Space>
                              </Space>
                            </Card>
                          ))}
                        </div>
                        <Pagination
                          style={{ marginTop: 16, textAlign: 'right' }}
                          current={deploymentPageInfo.current}
                          pageSize={deploymentPageInfo.pageSize}
                          total={deploymentPageInfo.total}
                          showSizeChanger
                          onChange={(current, pageSize) => {
                            deploymentActionRef.current?.setPageInfo?.({ current, pageSize });
                          }}
                        />
                      </>
                    ) : (
                      <Empty description="暂无数据" />
                    )}
                  </Card>
                </>
              )
            }
          />
        ),
      },
    ],
    [
      deploymentColumns,
      deploymentGroupData,
      deploymentLogRecord,
      deploymentPageInfo.current,
      deploymentPageInfo.pageSize,
      deploymentPageInfo.total,
      deploymentForm,
      deployableModelOptions,
      deploymentViewMode,
      handleBatchDeleteDeployment,
      handleBatchOperateDeployment,
      handleDeleteDeployment,
      handleOperateDeployment,
      inferenceColumns,
      inferenceForm,
      handleDeleteModel,
      modelColumns,
      modelPageInfo.current,
      modelPageInfo.pageSize,
      modelPageInfo.total,
      modelStatuses,
      modelTableData,
      modelViewMode,
      openModelForm,
      openDeploymentDrawer,
      openDeploymentLog,
      requestModels,
      requestDeployments,
      resolveDeploymentHost,
      resolveModelCardTitle,
      resolveModelDisplayLabel,
      resolveModelVersionText,
      loadDeployableModelOptions,
      trainMeta.inferenceTypes,
      trainMeta.deploymentStatuses,
    ],
  );

  return (
    <PageContainer title={false}>
      <Tabs activeKey={activeTab} items={tabItems} onChange={setActiveTab} />

      <Modal
        title={editingModelId ? '修改模型' : '新增模型'}
        open={modelOpen}
        onCancel={() => setModelOpen(false)}
        onOk={async () => {
          const values = await modelForm.validateFields();
          if (editingModelId) {
            await updateVideoModel({ ...values, id: editingModelId });
          } else {
            const importPackage = (values as Record<string, any>).importPackage as
              | { fileId?: string; fileName?: string }
              | undefined;
            const payload = { ...values } as Record<string, any>;
            delete payload.importPackage;
            if (importPackage?.fileId && !payload.currentVersion) {
              payload.currentVersion = 'v1';
            }
            if (importPackage?.fileId) {
              payload.modelFileId = importPackage.fileId;
            }
            const created = await createVideoModel(payload);
            if (importPackage?.fileId && created.data?.id) {
              await importVideoModelPackage(created.data.id, {
                fileId: importPackage.fileId,
                fileName: importPackage.fileName,
              });
            }
          }
          setModelOpen(false);
          await Promise.all([Promise.resolve(modelActionRef.current?.reload()), loadMeta()]);
        }}
      >
        <Form form={modelForm} layout="vertical">
          <Form.Item name="name" label="模型名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="modelFileId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="currentVersion" label="当前版本">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="模型状态" rules={[{ required: true }]}>
            <Select options={modelStatuses.map((item) => ({ label: item.text, value: item.value }))} />
          </Form.Item>
          <Form.Item name="coverFileId" label="封面文件">
            <VideoCoverUploadField />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          {!editingModelId ? (
            <Form.Item name="importPackage" label="导入训练文件">
              <VideoPackageUploadField />
            </Form.Item>
          ) : null}
        </Form>
      </Modal>

      <Modal
        title="创建部署实例"
        open={deploymentOpen}
        onCancel={() => setDeploymentOpen(false)}
        onOk={async () => {
          const values = await deploymentForm.validateFields();
          await createVideoDeployment(values);
          setDeploymentOpen(false);
          deploymentActionRef.current?.reload();
          await loadMeta();
        }}
      >
        <Form form={deploymentForm} layout="vertical">
          <Form.Item
            name="modelId"
            label="模型"
            rules={[{ required: true, message: '请选择可部署模型' }]}
          >
            <Select options={deployableModelOptions} placeholder="请选择模型(模型版本)" />
          </Form.Item>
          <Form.Item name="servicePort" label="服务端口" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="grayRelease" label="灰度发布" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新增推理任务"
        open={inferenceOpen}
        onCancel={() => setInferenceOpen(false)}
        onOk={async () => {
          const values = await inferenceForm.validateFields();
          await createVideoInferenceTask(values);
          setInferenceOpen(false);
          inferenceActionRef.current?.reload();
        }}
      >
        <Form form={inferenceForm} layout="vertical">
          <Form.Item name="taskName" label="任务名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="modelId" label="模型" rules={[{ required: true }]}>
            <Select options={trainMeta.models.map((item) => ({ label: item.name, value: item.id }))} />
          </Form.Item>
          <Form.Item name="taskType" label="推理类型" rules={[{ required: true }]}>
            <Select options={trainMeta.inferenceTypes.map((item) => ({ label: item.text, value: item.value }))} />
          </Form.Item>
          <Form.Item name="sourceUri" label="输入资源地址" rules={[{ required: true }]}>
            <Input placeholder="图片/视频/音频 URL 或文件路径" />
          </Form.Item>
          <Form.Item name="language" label="语言">
            <Input placeholder="如 zh-CN / en-US" />
          </Form.Item>
          <Form.Item name="promptText" label="提示词">
            <Input.TextArea rows={3} placeholder="多模态理解、OCR补充提示、ASR上下文等" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={deploymentLogRecord ? `部署日志 - ${deploymentLogRecord.instanceName}` : '部署日志'}
        open={deploymentLogOpen}
        width={960}
        footer={null}
        onCancel={() => {
          setDeploymentLogOpen(false);
          setDeploymentLogRecord(undefined);
          setDeploymentLogContent('');
        }}
      >
        <Space direction="vertical" size={12} style={{ width: '100%', marginBottom: 16 }}>
          <Space wrap>
            <Checkbox checked={deploymentLogAutoRefresh} onChange={(event) => setDeploymentLogAutoRefresh(event.target.checked)}>
              自动刷新
            </Checkbox>
            <Space size={6}>
              <Typography.Text type="secondary">刷新间隔(秒)</Typography.Text>
              <AntInputNumber
                min={1}
                max={60}
                value={deploymentLogInterval}
                onChange={(value) => setDeploymentLogInterval(Number(value) || 3)}
                disabled={!deploymentLogAutoRefresh}
              />
            </Space>
            <Button
              icon={<SnippetsOutlined />}
              onClick={() => {
                if (deploymentLogRecord) {
                  refreshDeploymentLog(deploymentLogRecord).catch(() => undefined);
                }
              }}
            >
              立即刷新
            </Button>
          </Space>
          {deploymentLogRecord ? (
            <Descriptions size="small" column={2}>
              <Descriptions.Item label="IP">
                {deploymentLogRecord.serviceUrl ? (() => {
                  try {
                    return new URL(deploymentLogRecord.serviceUrl).hostname || '-';
                  } catch {
                    return deploymentLogRecord.serviceUrl || '-';
                  }
                })() : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="端口">{deploymentLogRecord.servicePort || '-'}</Descriptions.Item>
              <Descriptions.Item label="接口地址" span={2}>
                {deploymentLogRecord.serviceUrl || '-'}
              </Descriptions.Item>
            </Descriptions>
          ) : null}
        </Space>
        <pre
          style={{
            minHeight: 520,
            maxHeight: '70vh',
            overflow: 'auto',
            whiteSpace: 'pre-wrap',
            margin: 0,
            padding: 16,
            borderRadius: 10,
            background: '#0f172a',
            color: '#e2e8f0',
            fontFamily: 'SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace',
            fontSize: 13,
            lineHeight: 1.7,
            boxShadow: 'inset 0 0 0 1px rgba(148, 163, 184, 0.16)',
          }}
        >
          {deploymentLogContent || '暂无日志'}
        </pre>
      </Modal>

      <Drawer
        title={
          selectedDeploymentGroup
            ? `${resolveModelDisplayLabel(selectedDeploymentGroup.modelId, selectedDeploymentGroup.modelName)} - 实例列表`
            : '实例列表'
        }
        open={deploymentDrawerOpen}
        width={1220}
        onClose={() => {
          setDeploymentDrawerOpen(false);
          setSelectedDeploymentGroup(undefined);
          setDeploymentInstanceData([]);
          setDeploymentInstancePageInfo((prev) => ({
            ...prev,
            current: 1,
          }));
        }}
      >
        <Table<VideoModelDeploymentRecord>
          rowKey="id"
          size="small"
          columns={deploymentInstanceColumns}
          dataSource={deploymentInstanceData.slice(
            (deploymentInstancePageInfo.current - 1) * deploymentInstancePageInfo.pageSize,
            deploymentInstancePageInfo.current * deploymentInstancePageInfo.pageSize,
          )}
          loading={deploymentInstanceLoading}
          scroll={{ x: 1120 }}
          pagination={{
            current: deploymentInstancePageInfo.current,
            pageSize: deploymentInstancePageInfo.pageSize,
            total: deploymentInstancePageInfo.total,
            showSizeChanger: true,
            onChange: (current, pageSize) => {
              if (selectedDeploymentGroup?.modelId) {
                setDeploymentInstancePageInfo((prev) => ({
                  ...prev,
                  current,
                  pageSize,
                }));
              }
            },
          }}
        />
      </Drawer>

      <Modal
        title="推理结果"
        open={inferenceResultOpen}
        footer={null}
        onCancel={() => setInferenceResultOpen(false)}
      >
        <pre style={{ maxHeight: 420, overflow: 'auto', whiteSpace: 'pre-wrap' }}>
          {inferenceResult || '暂无结果'}
        </pre>
      </Modal>
    </PageContainer>
  );
};

export default ModelTrainPage;
