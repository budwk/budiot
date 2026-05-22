import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { UploadFile } from 'antd';
import {
  App,
  Button,
  Form,
  Image,
  Input,
  InputNumber,
  Modal,
  Radio,
  Switch,
  Upload,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import {
  createApp,
  deleteApp,
  getAppDetail,
  getAppPage,
  updateApp,
  updateAppDisabled,
  updateAppLocation,
} from '@/services/budiot/sys/app';
import type { SysAppRecord } from '@/services/budiot/typing';

type AppFormValues = {
  id: string;
  name: string;
  path?: string;
  icon?: string;
  hidden: boolean;
  disabled: boolean;
  location: number;
};

const defaultFormValues: AppFormValues = {
  id: '',
  name: '',
  path: '',
  icon: '',
  hidden: false,
  disabled: false,
  location: 0,
};

const fileToDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });

const AppPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<AppFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [iconFileList, setIconFileList] = React.useState<UploadFile[]>([]);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);

  const openCreate = () => {
    setEditingId(undefined);
    setIconFileList([]);
    form.resetFields();
    form.setFieldsValue(defaultFormValues);
    setModalOpen(true);
  };

  const openEdit = async (record: SysAppRecord) => {
    const response = await getAppDetail(record.id);
    setEditingId(record.id);
    const data = response.data;
    form.setFieldsValue({
      ...defaultFormValues,
      ...data,
    });
    setIconFileList(
      data.icon
        ? [
            {
              uid: data.id,
              name: 'icon',
              status: 'done',
              url: data.icon,
            },
          ]
        : [],
    );
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateApp({ ...values, id: editingId });
      } else {
        await createApp(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysAppRecord>[]>(
    () => [
      {
        title: '应用名称',
        dataIndex: 'name',
      },
      {
        title: '关键字',
        dataIndex: 'keyword',
        hideInTable: true,
      },
      {
        title: '应用图标',
        dataIndex: 'icon',
        search: false,
        width: 100,
        render: (_, record) =>
          record.icon ? <Image width={24} height={24} src={record.icon} preview={false} /> : '-',
      },
      {
        title: '应用ID',
        dataIndex: 'id',
        search: false,
      },
      {
        title: '默认路径',
        dataIndex: 'path',
        search: false,
      },
      {
        title: '是否隐藏',
        dataIndex: 'hidden',
        search: false,
        width: 100,
        render: (_, record) =>
          record.hidden ? <span style={{ color: '#ff4d4f' }}>隐藏</span> : <span style={{ color: '#52c41a' }}>显示</span>,
      },
      {
        title: '排序',
        dataIndex: 'location',
        search: false,
        width: 140,
        render: (_, record) => (
          <InputNumber
            min={1}
            max={100}
            value={record.location}
            disabled={!access.hasPermission('sys.manage.app.update')}
            onChange={async (nextValue) => {
              if (typeof nextValue !== 'number') return;
              await updateAppLocation({ id: record.id, location: nextValue });
              actionRef.current?.reload();
            }}
          />
        ),
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        search: false,
        width: 120,
        render: (_, record) => (
          <Switch
            checked={!record.disabled}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            loading={switchingIds.includes(record.id)}
            disabled={!access.hasPermission('sys.manage.app.update')}
            onChange={async (checked) => {
              setSwitchingIds((prev) => [...prev, record.id]);
              try {
                await updateAppDisabled({ id: record.id, disabled: !checked });
                actionRef.current?.reload();
              } finally {
                setSwitchingIds((prev) => prev.filter((item) => item !== record.id));
              }
            }}
          />
        ),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 160,
        render: (_, record) => [
          <Button
            key="edit"
            type="link"
            icon={<EditOutlined />}
            disabled={!access.hasPermission('sys.manage.app.update')}
            onClick={() => openEdit(record)}
          >
            修改
          </Button>,
          <Button
            key="delete"
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={!access.hasPermission('sys.manage.app.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认删除应用',
                content: `确定删除 ${record.name} 吗？`,
                onOk: async () => {
                  await deleteApp(record.id);
                  actionRef.current?.reload();
                },
              });
            }}
          >
            删除
          </Button>,
        ],
      },
    ],
    [access, modal, switchingIds],
  );

  return (
    <PageContainer title="应用管理">
      <PlatformProTable<
        SysAppRecord,
        { current?: number; pageSize?: number; keyword?: string; name?: string }
      >
        persistenceKey="platform-sys-app-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="应用列表"
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.app.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getAppPage({
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
            pageOrderName: '',
            pageOrderBy: '',
            keyword: params.keyword || params.name || '',
          });
          return {
            data: response.data.list || [],
            total: response.data.totalCount || 0,
            success: true,
          };
        }}
        columns={columns}
      />

      <Modal
        title={editingId ? '修改应用' : '新增应用'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="应用名称"
            name="name"
            rules={[{ required: true, message: '请输入应用名称' }]}
          >
            <Input placeholder="请输入应用名称" />
          </Form.Item>
          <Form.Item
            label="应用ID"
            name="id"
            rules={[{ required: true, message: '请输入应用ID' }]}
          >
            <Input placeholder="请输入应用ID" disabled={Boolean(editingId)} />
          </Form.Item>
          <Form.Item label="默认路径" name="path">
            <Input placeholder="请输入默认路径" />
          </Form.Item>
          <Form.Item label="应用图标" name="icon">
            <Upload
              accept=".png,.jpg,.svg"
              listType="picture-card"
              maxCount={1}
              fileList={iconFileList}
              onRemove={() => {
                setIconFileList([]);
                form.setFieldValue('icon', '');
              }}
              beforeUpload={async (file) => {
                const base64 = await fileToDataUrl(file);
                form.setFieldValue('icon', base64);
                setIconFileList([
                  {
                    uid: file.uid,
                    name: file.name,
                    status: 'done',
                    url: base64,
                  },
                ]);
                return false;
              }}
            >
              {iconFileList.length ? null : '上传'}
            </Upload>
          </Form.Item>
          <Form.Item label="是否隐藏" name="hidden">
            <Radio.Group
              options={[
                { label: '显示', value: false },
                { label: '隐藏', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item label="应用状态" name="disabled" valuePropName="checked">
            <Switch checkedChildren="禁用" unCheckedChildren="启用" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default AppPage;
