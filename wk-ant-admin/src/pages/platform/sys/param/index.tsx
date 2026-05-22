import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess, useModel } from '@umijs/max';
import type { UploadFile } from 'antd';
import {
  App,
  Button,
  Form,
  Image,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Tag,
  Upload,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createParam,
  deleteParam,
  getParamDetail,
  getParamMeta,
  getParamPage,
  updateParam,
} from '@/services/budiot/sys/param';
import type {
  BudiotAppOption,
  SysParamRecord,
  SysParamTypeOption,
} from '@/services/budiot/typing';
import { uploadFile } from '@/utils/file';

type ParamFormValues = {
  id?: string;
  appId: string;
  configKey: string;
  configValue: string;
  type: string;
  note?: string;
  opened: boolean;
};

type ParamTableParams = {
  current?: number;
  pageSize?: number;
  configKey?: string;
  appId?: string;
};

const ParamPage = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const { initialState } = useModel('@@initialState');
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<ParamFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [metaLoading, setMetaLoading] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [apps, setApps] = React.useState<BudiotAppOption[]>([]);
  const [types, setTypes] = React.useState<SysParamTypeOption[]>([]);
  const [selectedAppId, setSelectedAppId] = React.useState<string>();
  const [fileList, setFileList] = React.useState<UploadFile[]>([]);

  const typeOptions = React.useMemo(
    () =>
      types
        .filter(
          (item): item is SysParamTypeOption & { value: string; text: string } =>
            typeof item?.value === 'string' &&
            item.value.length > 0 &&
            typeof item?.text === 'string' &&
            item.text.length > 0,
        )
        .filter(
          (item, index, list) => list.findIndex((current) => current.value === item.value) === index,
        )
        .map((item) => ({
          label: item.text,
          value: item.value,
        })),
    [types],
  );

  const currentType = Form.useWatch('type', form);
  const currentImageValue = Form.useWatch('configValue', form);
  const imageDomain = initialState?.platformInfo?.AppFileDomain || '';

  React.useEffect(() => {
    const initialize = async () => {
      setMetaLoading(true);
      try {
        const response = await getParamMeta();
        const nextApps = response.data.apps || [];
        setApps(nextApps);
        setTypes(response.data.types || []);
        if (nextApps[0]?.id) {
          setSelectedAppId(nextApps[0].id);
        }
      } finally {
        setMetaLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const openCreate = () => {
    if (!selectedAppId) {
      message.warning('请先选择应用');
      return;
    }
    setEditingId(undefined);
    setFileList([]);
    form.resetFields();
    form.setFieldsValue({
      appId: selectedAppId,
      configKey: '',
      configValue: '',
      type: 'TEXT',
      note: '',
      opened: true,
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysParamRecord) => {
    const response = await getParamDetail(record.id);
    const data = response.data;
    const currentRecordType =
      typeof data.type === 'string' ? data.type : data.type?.value || 'TEXT';
    setEditingId(record.id);
    setFileList([]);
    form.setFieldsValue({
      ...data,
      type: currentRecordType,
    });
    setModalOpen(true);
  };

  const handleUpload = async (file: File) => {
    const fd = new FormData();
    fd.append('Filedata', file);
    const response = await uploadFile(fd, { type: 'image' });
    const nextUrl = (response.data as { url?: string })?.url || '';
    form.setFieldValue('configValue', nextUrl);
    setFileList([
      {
        uid: `${Date.now()}`,
        name: file.name,
        status: 'done',
        url: nextUrl,
      },
    ]);
    return false;
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateParam({ ...values, id: editingId });
      } else {
        await createParam(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysParamRecord>[]>(
    () => [
      {
        title: '参数Key',
        dataIndex: 'configKey',
      },
      {
        title: '参数值',
        dataIndex: 'configValue',
        search: false,
        render: (_, record) =>
          (typeof record.type === 'string' ? record.type : record.type?.value) === 'IMAGE' ? (
            record.configValue ? (
              <Image width={56} height={56} src={`${imageDomain}${record.configValue}`} />
            ) : (
              '-'
            )
          ) : (
            record.configValue || '-'
          ),
      },
      {
        title: '参数类型',
        dataIndex: 'type',
        search: false,
        width: 120,
        render: (_, record) => (
          <Tag>{typeof record.type === 'string' ? record.type : record.type?.text || '-'}</Tag>
        ),
      },
      {
        title: '备注',
        dataIndex: 'note',
        search: false,
        ellipsis: true,
      },
      {
        title: '是否开放',
        dataIndex: 'opened',
        search: false,
        width: 100,
        render: (_, record) =>
          record.opened ? <Tag color="success">开放</Tag> : <Tag>内部</Tag>,
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 180,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('sys.config.param.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('sys.config.param.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除参数',
                    content: `确定删除 ${record.configKey} 吗？`,
                    onOk: async () => {
                      await deleteParam(record.id);
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
    [access, imageDomain, modal],
  );

  return (
    <PageContainer title="参数管理">
      <PlatformProTable<SysParamRecord, ParamTableParams>
        persistenceKey="platform-sys-param-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="参数列表"
        params={{ appId: selectedAppId }}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.config.param.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
          <Space key="app-switch" size={8}>
            <span>应用</span>
            <Select
              loading={metaLoading}
              value={selectedAppId}
              placeholder="切换应用"
              style={{ minWidth: 220 }}
              options={apps.map((item) => ({ label: item.name, value: item.id }))}
              onChange={(value) => setSelectedAppId(value)}
            />
          </Space>,
        ]}
        request={async (params) => {
          const appId = selectedAppId || params.appId;
          if (!appId) {
            return { data: [], total: 0, success: true };
          }
          const response = await getParamPage({
            appId,
            configKey: params.configKey || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
            pageOrderName: 'createdAt',
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
        title={editingId ? '修改参数' : '新增参数'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="type" label="参数类型">
            <Radio.Group options={typeOptions} />
          </Form.Item>
          <Form.Item name="appId" label="所属应用">
            <Select
              disabled
              options={apps.map((item) => ({ label: item.name, value: item.id }))}
            />
          </Form.Item>
          <Form.Item
            name="configKey"
            label="参数Key"
            rules={[{ required: true, message: '请输入参数Key' }]}
          >
            <Input placeholder="参数Key" />
          </Form.Item>
          {currentType === 'TEXT' ? (
            <Form.Item
              name="configValue"
              label="参数值"
              rules={[{ required: true, message: '请输入参数值' }]}
            >
              <Input placeholder="参数值" />
            </Form.Item>
          ) : null}
          {currentType === 'BOOL' ? (
            <Form.Item
              name="configValue"
              label="参数值"
              rules={[{ required: true, message: '请选择参数值' }]}
            >
              <Radio.Group
                options={[
                  { label: '是', value: 'true' },
                  { label: '否', value: 'false' },
                ]}
              />
            </Form.Item>
          ) : null}
          {currentType === 'IMAGE' ? (
            <Form.Item
              name="configValue"
              label="参数值"
              rules={[{ required: true, message: '请上传图片' }]}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                <Upload
                  listType="picture-card"
                  fileList={fileList}
                  maxCount={1}
                  beforeUpload={async (file) => handleUpload(file as File)}
                  onRemove={() => {
                    setFileList([]);
                    form.setFieldValue('configValue', '');
                  }}
                >
                  {fileList.length ? null : '上传图片'}
                </Upload>
                {!fileList.length && currentImageValue ? (
                  <Image width={96} src={`${imageDomain}${currentImageValue}`} />
                ) : null}
              </div>
            </Form.Item>
          ) : null}
          <Form.Item name="note" label="参数备注">
            <Input placeholder="备注说明" />
          </Form.Item>
          <Form.Item name="opened" label="是否开放">
            <Radio.Group
              options={[
                { label: '是', value: true },
                { label: '否', value: false },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default ParamPage;
