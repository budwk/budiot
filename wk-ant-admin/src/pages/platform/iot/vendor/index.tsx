import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Radio,
  Tag,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createIotVendor,
  deleteIotVendor,
  getIotVendorDetail,
  getIotVendorPage,
  updateIotVendor,
} from '@/services/budiot/iot/vendor';
import type { IotVendorRecord } from '@/services/budiot/iot/typing';
import { formatDateTime } from '../shared';

type VendorFormValues = {
  id?: string;
  name: string;
  code: string;
  contactName?: string;
  contactMobile?: string;
  contactEmail?: string;
  description?: string;
  disabled: boolean;
};

const defaultValues: VendorFormValues = {
  name: '',
  code: '',
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  description: '',
  disabled: false,
};

const VendorPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<VendorFormValues>();
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [submitting, setSubmitting] = React.useState(false);

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue(defaultValues);
    setModalOpen(true);
  };

  const openEdit = async (record: IotVendorRecord) => {
    const response = await getIotVendorDetail(record.id);
    setEditingId(record.id);
    form.setFieldsValue({
      id: response.data.id,
      name: response.data.name,
      code: response.data.code,
      contactName: response.data.contactName || '',
      contactMobile: response.data.contactMobile || '',
      contactEmail: response.data.contactEmail || '',
      description: response.data.description || '',
      disabled: response.data.disabled,
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotVendor({ ...values, id: editingId });
      } else {
        await createIotVendor(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<IotVendorRecord>[]>(
    () => [
      {
        title: '厂商名称',
        dataIndex: 'name',
      },
      {
        title: '厂商编码',
        dataIndex: 'code',
        width: 140,
      },
      {
        title: '联系人',
        dataIndex: 'contactName',
        search: false,
        width: 120,
      },
      {
        title: '联系电话',
        dataIndex: 'contactMobile',
        search: false,
        width: 140,
      },
      {
        title: '联系邮箱',
        dataIndex: 'contactEmail',
        search: false,
        width: 180,
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
        render: (_, record) => formatDateTime(record.updatedAt),
        width: 180,
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 160,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('iot.manage.vendor.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                disabled: !access.hasPermission('iot.manage.vendor.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除厂商',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteIotVendor(record.id);
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
    [access, modal],
  );

  return (
    <PageContainer title="设备厂商">
      <PlatformProTable<IotVendorRecord, { name?: string; code?: string; disabled?: boolean; current?: number; pageSize?: number }>
        persistenceKey="platform-iot-vendor-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="厂商列表"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.vendor.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getIotVendorPage({
            name: params.name || '',
            code: params.code || '',
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
        title={editingId ? '修改厂商' : '新增厂商'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="厂商名称" name="name" rules={[{ required: true, message: '请输入厂商名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="厂商编码" name="code" rules={[{ required: true, message: '请输入厂商编码' }]}>
            <Input />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
            <Form.Item label="联系人" name="contactName">
              <Input />
            </Form.Item>
            <Form.Item label="联系电话" name="contactMobile">
              <Input />
            </Form.Item>
            <Form.Item label="联系邮箱" name="contactEmail">
              <Input />
            </Form.Item>
            <Form.Item label="状态" name="disabled">
              <Radio.Group
                options={[
                  { label: '启用', value: false },
                  { label: '禁用', value: true },
                ]}
              />
            </Form.Item>
          </div>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default VendorPage;
