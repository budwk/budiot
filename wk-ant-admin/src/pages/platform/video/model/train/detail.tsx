import {
  DownloadOutlined,
  EyeOutlined,
  FileImageOutlined,
  RocketOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import PlatformProTable from '@/components/PlatformProTable';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import { App, Button, Card, Checkbox, Descriptions, Empty, Form, Image, Input, InputNumber, Modal, Progress, Select, Space, Switch, Tag, Typography } from 'antd';
import * as React from 'react';
import { APP_ID, DEFAULT_LANG } from '@/constants/app';
import { ManagedFileImagePreview } from '../../shared';
import { getVideoModelDetail, getVideoModelMeta } from '@/services/budiot/video/model';
import {
  createVideoTrainTask,
  createVideoDeployment,
  deleteVideoTrainTask,
  exportVideoTrainTask,
  getVideoTrainMeta,
  getVideoTrainTaskLog,
  getVideoTrainTaskPage,
} from '@/services/budiot/video/train';
import type {
  VideoDatasetRecord,
  VideoModelRecord,
  VideoModelTrainMeta,
  VideoModelTrainTaskRecord,
  VideoOption,
} from '@/services/budiot/video/typing';
import { getStoredToken } from '@/utils/session';

const emptyTrainMeta: VideoModelTrainMeta = {
  models: [],
  datasets: [],
  trainStatuses: [],
  deploymentStatuses: [],
  inferenceTypes: [],
  inferenceStatuses: [],
};

const activeTrainStatuses = ['PREPARING', 'RUNNING'];
const terminalTrainStatuses = ['COMPLETED', 'FAILED', 'CANCELED'];

type TrainResultPayload = {
  metrics?: Record<string, unknown>;
  charts?: Array<{ name?: string; url?: string }>;
  artifacts?: Record<string, string>;
  uploaded_artifacts?: Record<string, { url?: string; filename?: string }>;
  export_downloads?: Record<string, { url?: string; filename?: string }>;
  export_errors?: Record<string, string>;
  metadata_url?: string;
};

type DeploymentFormValue = {
  modelId: string;
  version: string;
  instanceName: string;
  servicePort: number;
  grayRelease: boolean;
};

const formatMetricValue = (value: unknown) => {
  if (typeof value === 'number') {
    return Number.isInteger(value) ? `${value}` : value.toFixed(4);
  }
  return typeof value === 'string' ? value : JSON.stringify(value);
};

const withApiPrefix = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  if (url.startsWith('/')) {
    return `/api${url}`;
  }
  return `/api/${url}`;
};

const downloadArtifact = async (url?: string, fileName?: string) => {
  const finalUrl = withApiPrefix(url);
  if (!finalUrl) {
    return;
  }
  const response = await fetch(finalUrl, {
    method: 'GET',
    headers: {
      Authorization: getStoredToken() || '',
      lang: DEFAULT_LANG,
      appId: APP_ID,
    },
  });
  if (!response.ok) {
    throw new Error(`下载失败: ${response.status}`);
  }
  const blob = await response.blob();
  const blobUrl = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = fileName || finalUrl.split('/').pop() || 'download';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
};

const ModelTrainDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { modal, message } = App.useApp();
  const [taskForm] = Form.useForm<Partial<VideoModelTrainTaskRecord>>();
  const [deploymentForm] = Form.useForm<DeploymentFormValue>();
  const [exportForm] = Form.useForm<{ exportFormats: string[] }>();
  const actionRef = React.useRef<ActionType | undefined>(undefined);
  const [model, setModel] = React.useState<VideoModelRecord>();
  const [modelStatuses, setModelStatuses] = React.useState<VideoOption[]>([]);
  const [trainMeta, setTrainMeta] = React.useState<VideoModelTrainMeta>(emptyTrainMeta);
  const [taskTotal, setTaskTotal] = React.useState(0);
  const [hasActiveTask, setHasActiveTask] = React.useState(false);
  const [taskOpen, setTaskOpen] = React.useState(false);
  const [taskLogOpen, setTaskLogOpen] = React.useState(false);
  const [taskLogTask, setTaskLogTask] = React.useState<VideoModelTrainTaskRecord>();
  const [taskLog, setTaskLog] = React.useState('');
  const [taskLogStatus, setTaskLogStatus] = React.useState<string>();
  const [taskLogProgress, setTaskLogProgress] = React.useState<number>(0);
  const [taskResultOpen, setTaskResultOpen] = React.useState(false);
  const [taskResultTask, setTaskResultTask] = React.useState<VideoModelTrainTaskRecord>();
  const [taskResultLoading, setTaskResultLoading] = React.useState(false);
  const [taskResult, setTaskResult] = React.useState<TrainResultPayload>();
  const [deployOpen, setDeployOpen] = React.useState(false);
  const [deployTask, setDeployTask] = React.useState<VideoModelTrainTaskRecord>();
  const [exportOpen, setExportOpen] = React.useState(false);
  const [exportTask, setExportTask] = React.useState<VideoModelTrainTaskRecord>();
  const [exportSubmitting, setExportSubmitting] = React.useState(false);

  const trainBootstrapModelOptions = React.useMemo(
    () => [
      { label: '默认(yolov11n.pt)', value: 'yolov11n.pt' },
      { label: 'YOLOv8 Nano (yolov8n.pt)', value: 'yolov8n.pt' },
      ...trainMeta.models.map((item) => ({
        label: item.currentVersion ? `${item.name} (${item.currentVersion})` : item.name,
        value: item.currentVersion ? `${item.name}:${item.currentVersion}` : item.name,
      })),
    ],
    [trainMeta.models],
  );

  const loadSummary = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const [modelRes, modelMetaRes, metaRes] = await Promise.all([
      getVideoModelDetail(id),
      getVideoModelMeta(),
      getVideoTrainMeta(),
    ]);
    setModel(modelRes.data);
    setModelStatuses(modelMetaRes.data.statuses || []);
    setTrainMeta(metaRes.data || emptyTrainMeta);
  }, [id]);

  React.useEffect(() => {
    loadSummary().catch(() => undefined);
  }, [loadSummary]);

  const refreshTaskLog = React.useCallback(async (taskId: string) => {
    const res = await getVideoTrainTaskLog(taskId);
    setTaskLog(res.data.log || '');
    setTaskLogStatus(res.data.status);
    setTaskLogProgress(res.data.progress || 0);
    setTaskLogTask((current) =>
      current
        ? {
            ...current,
            status: res.data.status || current.status,
            progress: res.data.progress ?? current.progress,
          }
        : current,
    );
    return res.data;
  }, []);

  const openTaskResult = React.useCallback(
    async (record: VideoModelTrainTaskRecord) => {
      setTaskResultTask(record);
      setTaskResultOpen(true);
      setTaskResultLoading(true);
      try {
        const data = await refreshTaskLog(record.id);
        setTaskResult(data.result as TrainResultPayload);
      } finally {
        setTaskResultLoading(false);
      }
    },
    [refreshTaskLog],
  );

  React.useEffect(() => {
    if (!taskLogOpen || !taskLogTask?.id) {
      return;
    }
    refreshTaskLog(taskLogTask.id).catch(() => undefined);
    if (taskLogStatus && terminalTrainStatuses.includes(taskLogStatus)) {
      return;
    }
    const timer = window.setInterval(() => {
      refreshTaskLog(taskLogTask.id).catch(() => undefined);
      actionRef.current?.reload();
    }, 3000);
    return () => window.clearInterval(timer);
  }, [actionRef, refreshTaskLog, taskLogOpen, taskLogStatus, taskLogTask?.id]);

  const statusColorMap = React.useMemo<Record<string, string>>(
    () => ({
      PREPARING: 'processing',
      RUNNING: 'processing',
      COMPLETED: 'success',
      FAILED: 'error',
      CANCELED: 'default',
    }),
    [],
  );

  const openDeployModal = React.useCallback(
    (record: VideoModelTrainTaskRecord) => {
      setDeployTask(record);
      deploymentForm.setFieldsValue({
        modelId: record.modelId,
        version: record.modelVersion,
        servicePort: 19100,
        grayRelease: false,
      });
      setDeployOpen(true);
    },
    [deploymentForm],
  );

  const openExportModal = React.useCallback((record: VideoModelTrainTaskRecord) => {
    exportForm.setFieldsValue({
      exportFormats: ['onnx'],
    });
    setExportTask(record);
    setExportOpen(true);
  }, [exportForm]);

  const taskColumns = React.useMemo<ProColumns<VideoModelTrainTaskRecord>[]>(
    () => [
      { title: '任务名称', dataIndex: 'taskName' },
      { title: '数据集', dataIndex: 'datasetName', render: (_, record) => record.datasetName || record.datasetId },
      { title: '版本', dataIndex: 'modelVersion' },
      { title: 'epochs', dataIndex: 'epochs' },
      {
        title: '进度',
        dataIndex: 'progress',
        width: 220,
        render: (_, record) => (
          <Progress
            percent={Math.max(0, Math.min(100, record.progress || 0))}
            size="small"
            status={record.status === 'FAILED' ? 'exception' : undefined}
          />
        ),
      },
      {
        title: '状态',
        dataIndex: 'status',
        render: (_, record) => (
          <Tag color={statusColorMap[record.status] || 'default'}>
            {trainMeta.trainStatuses.find((item) => item.value === record.status)?.text || record.status}
          </Tag>
        ),
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        valueType: 'dateTime',
        width: 260,
      },
      {
        title: '操作',
        width: 180,
        render: (_, record) => (
          <Space>
            {record.status === 'COMPLETED' ? (
              <Button
                type="link"
                icon={<FileImageOutlined />}
                onClick={() => {
                  openTaskResult(record).catch(() => undefined);
                }}
              >
                查看结果
              </Button>
            ) : null}
            {record.status === 'COMPLETED' ? (
              <Button
                type="link"
                icon={<DownloadOutlined />}
                onClick={() => {
                  openExportModal(record);
                }}
              >
                导出
              </Button>
            ) : null}
            {record.status === 'COMPLETED' ? (
              <Button
                type="link"
                icon={<RocketOutlined />}
                title="发布为正式模型"
                onClick={() => {
                  openDeployModal(record);
                }}
              >
                部署
              </Button>
            ) : null}
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={async () => {
                setTaskLogTask(record);
                setTaskLogOpen(true);
                await refreshTaskLog(record.id);
              }}
            >
              查看日志
            </Button>
            <Button
              danger
              type="link"
              icon={<DeleteOutlined />}
              onClick={() =>
                modal.confirm({
                   title: `确认删除训练任务“${record.taskName}”吗？`,
                   onOk: async () => {
                     await deleteVideoTrainTask(record.id);
                     actionRef.current?.reload();
                   },
                 })
               }
            >
              删除
            </Button>
          </Space>
        ),
      },
    ],
    [modal, openDeployModal, openExportModal, openTaskResult, refreshTaskLog, statusColorMap, trainMeta.trainStatuses],
  );

  const resultLinks = React.useMemo(() => {
    const uploaded = Object.entries(taskResult?.uploaded_artifacts || {}).map(([key, value]) => ({
      key,
      label: value?.filename || key,
      url: withApiPrefix(value?.url || ''),
    }));
    const dedup = new Map<string, { label: string; url: string }>();
    uploaded.forEach((item) => {
      if (item.url && !dedup.has(item.url)) {
        dedup.set(item.url, { label: item.label, url: item.url });
      }
    });
    return Array.from(dedup.values());
  }, [taskResult]);

  return (
    <PageContainer
      title={model?.name || '模型详情'}
      onBack={() => history.push('/platform/video/model/train')}
    >
      <Card style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="模型封面" span={2}>
            <div style={{ width: 240, height: 160, overflow: 'hidden', borderRadius: 8 }}>
              <ManagedFileImagePreview
                fileId={model?.coverFileId}
                alt={model?.name}
                style={{ width: 240, height: 160, objectFit: 'cover' }}
              />
            </div>
          </Descriptions.Item>
          <Descriptions.Item label="模型名称">{model?.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="当前版本">{model?.currentVersion || '-'}</Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>
            {model?.description || '暂无描述'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <PlatformProTable<VideoModelTrainTaskRecord, Record<string, unknown>>
        rowKey="id"
        persistenceKey={`video-model-train-task-${id || 'detail'}`}
        actionRef={actionRef}
        headerTitle={`训练任务 (${taskTotal})`}
        search={false}
        polling={hasActiveTask ? 3000 : undefined}
        columns={taskColumns}
        request={async (params) => {
          if (!id) {
            return { data: [], success: true, total: 0 };
          }
          const res = await getVideoTrainTaskPage({
            modelId: id,
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
          });
          const list = res.data.list || [];
          setTaskTotal(res.data.totalCount || 0);
          setHasActiveTask(list.some((item) => activeTrainStatuses.includes(item.status)));
          return {
            data: list,
            success: true,
            total: res.data.totalCount || 0,
          };
        }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              taskForm.resetFields();
              taskForm.setFieldsValue({
                epochs: 100,
                batchSize: 16,
                imgsz: 640,
                pretrainedModel: 'yolov11n.pt',
              });
              setTaskOpen(true);
            }}
          >
            启动训练
          </Button>,
        ]}
      />

      <Modal
        title="启动训练任务"
        open={taskOpen}
        onCancel={() => setTaskOpen(false)}
        onOk={async () => {
          const values = await taskForm.validateFields();
          if (!id) {
            return;
          }
          await createVideoTrainTask({
            ...values,
            modelId: id,
            taskName: `${model?.name || '训练任务'}-${Date.now()}`,
            modelVersion: `v${Date.now()}`,
          });
          setTaskOpen(false);
          await Promise.all([loadSummary(), actionRef.current?.reload?.()]);
        }}
      >
        <Form form={taskForm} layout="vertical">
          <Form.Item name="datasetId" label="数据集" rules={[{ required: true }]}>
            <Select
              options={trainMeta.datasets.map((item: VideoDatasetRecord) => ({
                label: item.name,
                value: item.id,
              }))}
            />
          </Form.Item>
          <Form.Item name="pretrainedModel" label="模型" initialValue="yolov11n.pt">
            <Select options={trainBootstrapModelOptions} />
          </Form.Item>
          <Form.Item name="epochs" label="迭代次数" extra="推荐值: 100-300" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="batchSize" label="批量大小" extra="默认 16，训练服务会按可用内存自动下调" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="imgsz" label="图像尺寸" extra="默认 640px，内存紧张时会自动降档" rules={[{ required: true }]}>
            <InputNumber min={32} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={taskLogTask ? `训练日志 - ${taskLogTask.taskName}` : '训练日志'}
        open={taskLogOpen}
        width={960}
        footer={null}
        styles={{ body: { paddingTop: 12 } }}
        onCancel={() => {
          setTaskLogOpen(false);
          setTaskLogTask(undefined);
          setTaskLogStatus(undefined);
          setTaskLogProgress(0);
        }}
      >
        <div style={{ marginBottom: 16 }}>
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            <Tag color={statusColorMap[taskLogStatus || taskLogTask?.status || ''] || 'default'}>
              {trainMeta.trainStatuses.find((item) => item.value === (taskLogStatus || taskLogTask?.status))?.text ||
                taskLogStatus ||
                taskLogTask?.status ||
                '未知状态'}
            </Tag>
            <Progress
              percent={Math.max(0, Math.min(100, taskLogProgress || taskLogTask?.progress || 0))}
              size="small"
              status={(taskLogStatus || taskLogTask?.status) === 'FAILED' ? 'exception' : undefined}
            />
          </Space>
        </div>
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
            fontFamily:
              'SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace',
            fontSize: 13,
            lineHeight: 1.7,
            boxShadow: 'inset 0 0 0 1px rgba(148, 163, 184, 0.16)',
          }}
        >
          {taskLog || '暂无日志'}
        </pre>
      </Modal>

      <Modal
        title={exportTask ? `导出模型 - ${exportTask.taskName}` : '导出模型'}
        open={exportOpen}
        confirmLoading={exportSubmitting}
        onCancel={() => {
          setExportOpen(false);
          setExportTask(undefined);
        }}
        onOk={async () => {
          if (!exportTask) {
            return;
          }
          const values = await exportForm.validateFields();
          setExportSubmitting(true);
          try {
            const res = await exportVideoTrainTask(exportTask.id, values);
            const downloads = res.data.downloads || [];
            await Promise.all(downloads.map((item) => downloadArtifact(item.url, item.filename)));
            if (res.data.result) {
              setTaskResult(res.data.result as TrainResultPayload);
            }
            if (Object.keys(res.data.export_errors || {}).length) {
              modal.warning({
                title: '部分格式导出失败',
                content: (
                  <div>
                    {Object.entries(res.data.export_errors || {}).map(([key, value]) => (
                      <div key={key}>
                        {key}: {value}
                      </div>
                    ))}
                  </div>
                ),
              });
            } else {
              message.success('模型导出完成');
            }
            setExportOpen(false);
            setExportTask(undefined);
          } catch (error) {
            message.error(error instanceof Error ? error.message : '模型导出失败');
          } finally {
            setExportSubmitting(false);
          }
        }}
      >
        <Form form={exportForm} layout="vertical">
          <Form.Item
            name="exportFormats"
            label="导出格式"
            rules={[{ required: true, message: '请选择至少一种导出格式' }]}
          >
            <Checkbox.Group
              options={[
                { label: 'ONNX', value: 'onnx' },
                { label: 'OpenVINO', value: 'openvino' },
              ]}
            />
          </Form.Item>
          <Typography.Text type="secondary">
            导出后将生成可下载产物；多文件格式会自动打包为 zip。
          </Typography.Text>
        </Form>
      </Modal>

      <Modal
        title={deployTask ? `部署训练结果 - ${deployTask.taskName}` : '部署训练结果'}
        open={deployOpen}
        onCancel={() => {
          setDeployOpen(false);
          setDeployTask(undefined);
        }}
        onOk={async () => {
          const values = await deploymentForm.validateFields();
          await createVideoDeployment(values);
          setDeployOpen(false);
          setDeployTask(undefined);
          await loadSummary();
        }}
      >
        <Form form={deploymentForm} layout="vertical">
          <Form.Item name="modelId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="version" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="提示">
            <Typography.Text type="secondary">发布为正式模型</Typography.Text>
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
        title={taskResultTask ? `训练结果 - ${taskResultTask.taskName}` : '训练结果'}
        open={taskResultOpen}
        width={1120}
        footer={null}
        confirmLoading={taskResultLoading}
        onCancel={() => {
          setTaskResultOpen(false);
          setTaskResultTask(undefined);
          setTaskResult(undefined);
        }}
      >
        {taskResultLoading ? null : (
          <Space direction="vertical" size={20} style={{ width: '100%' }}>
            <Card size="small" title="训练指标">
              {taskResult?.metrics && Object.keys(taskResult.metrics).length ? (
                <Descriptions size="small" column={2}>
                  {Object.entries(taskResult.metrics).map(([key, value]) => (
                    <Descriptions.Item key={key} label={key}>
                      {formatMetricValue(value)}
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              ) : (
                <Empty description="暂无训练指标" />
              )}
            </Card>

            <Card size="small" title="训练图表">
              {taskResult?.charts?.length ? (
                <Space wrap size={16}>
                  {taskResult.charts.map((chart) => (
                    <div key={`${chart.name}-${chart.url}`} style={{ width: 320 }}>
                      <div style={{ marginBottom: 8, fontWeight: 500 }}>{chart.name || '训练图表'}</div>
                      <Image
                        src={withApiPrefix(chart.url)}
                        alt={chart.name || '训练图表'}
                        style={{ width: '100%', borderRadius: 8, border: '1px solid #f0f0f0' }}
                      />
                    </div>
                  ))}
                </Space>
              ) : (
                <Empty description="暂无训练图表" />
              )}
            </Card>

            <Card size="small" title="训练产物">
              <Space wrap size={[12, 12]}>
                {resultLinks.length ? (
                  resultLinks.map((item) => (
                    <Typography.Link
                      key={`${item.label}-${item.url}`}
                      onClick={() => {
                        downloadArtifact(item.url, item.label).catch(() => undefined);
                      }}
                    >
                      {item.label}
                    </Typography.Link>
                  ))
                ) : (
                  <Empty description="暂无训练产物" />
                )}
              </Space>
              {taskResult?.export_errors && Object.keys(taskResult.export_errors).length ? (
                <div style={{ marginTop: 16 }}>
                  <Tag color="warning">部分导出已跳过</Tag>
                  <div style={{ marginTop: 8, color: '#595959' }}>
                    {Object.entries(taskResult.export_errors).map(([key, value]) => (
                      <div key={key}>
                        {key}: {value}
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </Card>
          </Space>
        )}
      </Modal>
    </PageContainer>
  );
};

export default ModelTrainDetailPage;
