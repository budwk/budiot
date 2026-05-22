import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { App, Button, Form, Input, InputNumber, Modal } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import {
  createPost,
  deletePost,
  getPostDetail,
  getPostPage,
  updatePost,
  updatePostLocation,
} from '@/services/budiot/sys/post';
import type { SysPostListRecord } from '@/services/budiot/typing';

type PostFormValues = {
  id?: string;
  name: string;
  code: string;
  location?: number;
};

const PostPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<PostFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({ name: '', code: '', location: 0 });
    setModalOpen(true);
  };

  const openEdit = async (record: SysPostListRecord) => {
    const response = await getPostDetail(record.id);
    setEditingId(record.id);
    form.setFieldsValue(response.data);
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updatePost({ ...values, id: editingId });
      } else {
        await createPost(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysPostListRecord>[]>(
    () => [
      {
        title: '职务名称',
        dataIndex: 'name',
      },
      {
        title: '职务编号',
        dataIndex: 'code',
      },
      {
        title: '排序',
        dataIndex: 'location',
        width: 180,
        search: false,
        render: (_, record) => (
          <InputNumber
            min={1}
            max={100}
            value={record.location}
            disabled={!access.hasPermission('sys.manage.post.update')}
            onChange={async (nextValue) => {
              if (typeof nextValue !== 'number') return;
              await updatePostLocation({ id: record.id, location: nextValue });
              actionRef.current?.reload();
            }}
          />
        ),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 140,
        render: (_, record) => [
          <Button
            key="edit"
            type="link"
            icon={<EditOutlined />}
            disabled={!access.hasPermission('sys.manage.post.update')}
            onClick={() => openEdit(record)}
          >
            修改
          </Button>,
          <Button
            key="delete"
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={!access.hasPermission('sys.manage.post.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认删除职务',
                content: `确定删除 ${record.name} 吗？`,
                onOk: async () => {
                  await deletePost(record.id);
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
    [access, modal],
  );

  return (
    <PageContainer title="岗位管理">
      <PlatformProTable<SysPostListRecord, { current?: number; pageSize?: number }>
        persistenceKey="platform-sys-post-table"
        actionRef={actionRef}
        rowKey="id"
        search={false}
        headerTitle="岗位列表"
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.post.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getPostPage({
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
            pageOrderName: '',
            pageOrderBy: '',
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
        title={editingId ? '修改职务' : '新增职务'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="职务名称"
            rules={[{ required: true, message: '请输入职务名称' }]}
          >
            <Input placeholder="请输入职务名称" />
          </Form.Item>
          <Form.Item
            name="code"
            label="职务编号"
            rules={[{ required: true, message: '请输入职务编号' }]}
          >
            <Input placeholder="请输入职务编号" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default PostPage;
