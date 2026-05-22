import {
  AppstoreOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history } from '@umijs/max';
import { App, Button, Card, Form, Input, Modal, Pagination, Progress, Radio, Space, Tag, Typography } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import { ManagedFileImagePreview, VideoCoverUploadField } from '../../shared';
import {
  createVideoDataset,
  deleteVideoDataset,
  getVideoDatasetAiConfig,
  getVideoDatasetDetail,
  getVideoDatasetMeta,
  getVideoDatasetPage,
  saveVideoDatasetAiConfig,
  updateVideoDataset,
} from '@/services/budiot/video/dataset';
import type {
  VideoDatasetAiConfigRecord,
  VideoDatasetRecord,
  VideoOption,
} from '@/services/budiot/video/typing';

type DatasetFormValues = Partial<VideoDatasetRecord>;
type DatasetViewMode = 'table' | 'card';
type DatasetAiConfigFormValues = VideoDatasetAiConfigRecord;

const DATASET_TYPE_LABELS: Record<string, string> = {
  IMAGE: '图片',
  TEXT: '文本',
};

const DatasetPage: React.FC = () => {
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<DatasetFormValues>();
  const [aiConfigForm] = Form.useForm<DatasetAiConfigFormValues>();
  const [types, setTypes] = React.useState<VideoOption[]>([]);
  const [viewMode, setViewMode] = React.useState<DatasetViewMode>('card');
  const [tableData, setTableData] = React.useState<VideoDatasetRecord[]>([]);
  const [pageInfo, setPageInfo] = React.useState({ current: 1, pageSize: 12, total: 0 });
  const [editingId, setEditingId] = React.useState<string>();
  const [open, setOpen] = React.useState(false);
  const [aiConfigOpen, setAiConfigOpen] = React.useState(false);

  const loadTypes = React.useCallback(async () => {
    const res = await getVideoDatasetMeta();
    setTypes(res.data.datasetTypes || []);
  }, []);

  React.useEffect(() => {
    loadTypes().catch(() => undefined);
  }, [loadTypes]);

  const openCreate = React.useCallback(() => {
    setEditingId(undefined);
    form.resetFields();
    setOpen(true);
  }, [form]);

  const openAiConfig = React.useCallback(async () => {
    const res = await getVideoDatasetAiConfig();
    aiConfigForm.setFieldsValue({
      provider: res.data.provider || 'QWEN',
      modelName: res.data.modelName || 'qwen-vl-max-latest',
      apiKey: res.data.apiKey || '',
      promptText: res.data.promptText || '',
    });
    setAiConfigOpen(true);
  }, [aiConfigForm]);

  const openEdit = React.useCallback(
    async (id: string) => {
      const res = await getVideoDatasetDetail(id);
      setEditingId(id);
      form.setFieldsValue(res.data);
      setOpen(true);
    },
    [form],
  );

  const handleDelete = React.useCallback(
    (record: VideoDatasetRecord) => {
      modal.confirm({
        title: `确认删除数据集“${record.name}”吗？`,
        onOk: async () => {
          await deleteVideoDataset(record.id);
          actionRef.current?.reload();
        },
      });
    },
    [modal],
  );

  const requestDatasets = React.useCallback(
    async (params: Record<string, any>) => {
      const res = await getVideoDatasetPage({
        name: params.name,
        datasetType: params.datasetType,
        pageNo: params.current,
        pageSize: params.pageSize,
        pageOrderName: 'updatedAt',
        pageOrderBy: 'descending',
      });
      const list = res.data.list || [];
      setTableData(list);
      setPageInfo({
        current: params.current || 1,
        pageSize: params.pageSize || 12,
        total: res.data.totalCount || 0,
      });
      return {
        data: list,
        total: res.data.totalCount || 0,
        success: true,
      };
    },
    [],
  );

  const getDatasetTypeText = React.useCallback(
    (datasetType?: string) => {
      if (!datasetType) {
        return '-';
      }
      return types.find((item) => item.value === datasetType)?.text || DATASET_TYPE_LABELS[datasetType] || datasetType;
    },
    [types],
  );

  const columns = React.useMemo<ProColumns<VideoDatasetRecord>[]>(
    () => [
      { title: '数据集名称', dataIndex: 'name' },
      {
        title: '类型',
        dataIndex: 'datasetType',
        valueType: 'select',
        fieldProps: {
          options: types.map((item) => ({ label: item.text, value: item.value })),
          allowClear: true,
        },
        render: (_, record) => <Tag color="blue">{getDatasetTypeText(record.datasetType)}</Tag>,
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
              style={{ width: 56, height: 56, objectFit: 'contain', background: '#f5f5f5' }}
            />
          </div>
        ),
      },
      {
        title: '标注进度',
        search: false,
        render: (_, record) => {
          const totalCount = record.totalCount || 0;
          const labeledCount = record.labeledCount || 0;
          const percent = totalCount > 0 ? Math.round((labeledCount / totalCount) * 100) : 0;
          return (
            <Progress
              percent={percent}
              size="small"
              style={{ width: 220 }}
              format={() => `${labeledCount}/${totalCount}`}
            />
          );
        },
      },
      { title: '描述', dataIndex: 'description', search: false, ellipsis: true },
      {
        title: '操作',
        valueType: 'option',
        width: 220,
        render: (_, record) => (
          <Space>
            <Button
              type="link"
              icon={<EyeOutlined />}
              onClick={() => history.push(`/platform/video/model/dataset/${record.id}`)}
            >
              详情
            </Button>
            <Button type="link" icon={<EditOutlined />} onClick={() => openEdit(record.id)}>
              修改
            </Button>
            <Button danger type="link" icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>
              删除
            </Button>
          </Space>
        ),
      },
    ],
    [getDatasetTypeText, handleDelete, openEdit, types],
  );

  return (
    <PageContainer title={false}>
      <PlatformProTable<VideoDatasetRecord, Record<string, any>>
        actionRef={actionRef}
        persistenceKey="video-dataset-table"
        rowKey="id"
        headerTitle={<Typography.Text strong>数据标注管理</Typography.Text>}
        columns={columns}
        request={requestDatasets}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建数据集
          </Button>,
          <Button key="ai-config" icon={<SettingOutlined />} onClick={() => openAiConfig().catch(() => undefined)}>
            设置AI标注模型
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
                    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                    gap: 16,
                  }}
                >
                  {tableData.map((record) => {
                    const totalCount = record.totalCount || 0;
                    const labeledCount = record.labeledCount || 0;
                    const percent = totalCount > 0 ? Math.round((labeledCount / totalCount) * 100) : 0;
                    return (
                      <Card
                        key={record.id}
                        hoverable
                        onClick={() => history.push(`/platform/video/model/dataset/${record.id}`)}
                        styles={{ body: { padding: 18 } }}
                      >
                        <Space direction="vertical" size={14} style={{ width: '100%' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                            <div>
                              <Typography.Title level={5} style={{ margin: 0 }}>
                                {record.name}
                              </Typography.Title>
                              <Typography.Text type="secondary">{getDatasetTypeText(record.datasetType)}</Typography.Text>
                            </div>
                            <div style={{ width: 80, height: 56, overflow: 'hidden', borderRadius: 8 }}>
                              <ManagedFileImagePreview
                                fileId={record.coverFileId}
                                alt={record.name}
                                style={{ width: 80, height: 56, objectFit: 'contain', background: '#f5f5f5' }}
                              />
                            </div>
                          </div>
                          <Typography.Text type="secondary">{record.description || '暂无描述'}</Typography.Text>
                          <Progress percent={percent} size="small" format={() => `${labeledCount}/${totalCount}`} />
                          <Space wrap>
                            <Button
                              type="link"
                              size="small"
                              icon={<EyeOutlined />}
                              style={{ paddingInline: 0 }}
                              onClick={(event) => {
                                event.stopPropagation();
                                history.push(`/platform/video/model/dataset/${record.id}`);
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
                                openEdit(record.id).catch(() => undefined);
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
                                handleDelete(record);
                              }}
                            >
                              删除
                            </Button>
                          </Space>
                        </Space>
                      </Card>
                    );
                  })}
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
        destroyOnClose
        open={aiConfigOpen}
        title="设置AI标注模型"
        onCancel={() => setAiConfigOpen(false)}
        onOk={() => {
          aiConfigForm
            .validateFields()
            .then(async (values) => {
              await saveVideoDatasetAiConfig(values);
              setAiConfigOpen(false);
            })
            .catch(() => undefined);
        }}
      >
        <Form form={aiConfigForm} layout="vertical" initialValues={{ provider: 'QWEN', modelName: 'qwen-vl-max-latest' }}>
          <Form.Item name="provider" label="AI服务商" rules={[{ required: true, message: '请选择AI服务商' }]}>
            <Radio.Group
              options={[{ label: '阿里千问', value: 'QWEN' }]}
              optionType="button"
              buttonStyle="solid"
            />
          </Form.Item>
          <Form.Item name="modelName" label="模型名称" rules={[{ required: true, message: '请输入模型名称' }]}>
            <Input placeholder="例如：qwen-vl-max-latest" />
          </Form.Item>
          <Form.Item name="apiKey" label="千问Key" rules={[{ required: true, message: '请输入千问Key' }]}>
            <Input.Password placeholder="请输入阿里千问 API Key" />
          </Form.Item>
          <Form.Item name="promptText" label="提示词">
            <Input.TextArea
              rows={6}
              placeholder="可选；为空时系统会根据当前数据集标签自动生成识别提示词"
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        destroyOnClose
        open={open}
        title={editingId ? '修改数据集' : '新建数据集'}
        onCancel={() => setOpen(false)}
        onOk={() => {
          form
            .validateFields()
            .then(async (values) => {
              if (editingId) {
                await updateVideoDataset({ ...values, id: editingId });
              } else {
                await createVideoDataset(values);
              }
              setOpen(false);
              actionRef.current?.reload();
            })
            .catch(() => undefined);
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="数据集名称" rules={[{ required: true, message: '请输入数据集名称' }]}>
            <Input maxLength={120} placeholder="请输入数据集名称" />
          </Form.Item>
          <Form.Item name="datasetType" label="数据集类型" rules={[{ required: true, message: '请选择数据集类型' }]}>
            <Radio.Group
              options={types.map((item) => ({ label: item.text, value: item.value }))}
              optionType="button"
              buttonStyle="solid"
            />
          </Form.Item>
          <Form.Item name="description" label="数据集描述">
            <Input.TextArea rows={4} maxLength={500} placeholder="请输入数据集描述" />
          </Form.Item>
          <Form.Item name="coverFileId" label="数据集封面">
            <VideoCoverUploadField />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default DatasetPage;
