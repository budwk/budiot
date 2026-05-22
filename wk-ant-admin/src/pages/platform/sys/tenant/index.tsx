import {
  ClockCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  TagsOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess } from '@umijs/max';
import { App, Button, DatePicker, Form, Input, Modal, Radio, Switch } from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createTenant,
  deleteTenant,
  getTenantDetail,
  getTenantMeta,
  getTenantPage,
  updateTenant,
  updateTenantDisabled,
  updateTenantExpire,
} from '@/services/budiot/sys/tenant';
import type { SysTenantPackageRecord, SysTenantRecord } from '@/services/budiot/typing';

const PLATFORM_TENANT_ID = 'platform';

type TenantFormValues = {
  id?: string;
  name: string;
  packageId: string;
  adminLoginname: string;
  adminPassword?: string;
  hasExpire: boolean;
  expireAt?: dayjs.Dayjs | null;
  disabled: boolean;
};

type ExpireFormValues = {
  id: string;
  hasExpire: boolean;
  expireAt?: dayjs.Dayjs | null;
};

const normalizeExpireAt = (value?: dayjs.Dayjs | null, hasExpire?: boolean) =>
  hasExpire && value ? value.valueOf() : null;

const TenantPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [tenantForm] = Form.useForm<TenantFormValues>();
  const [expireForm] = Form.useForm<ExpireFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [packages, setPackages] = React.useState<SysTenantPackageRecord[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [expireModalOpen, setExpireModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();

  React.useEffect(() => {
    getTenantMeta()
      .then((response) => setPackages(response.data.packages || []))
      .catch(() => undefined);
  }, []);

  const openCreate = () => {
    setEditingId(undefined);
    tenantForm.resetFields();
    tenantForm.setFieldsValue({
      name: '',
      packageId: packages[0]?.id || '',
      adminLoginname: '',
      adminPassword: '',
      hasExpire: false,
      expireAt: null,
      disabled: false,
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysTenantRecord) => {
    const response = await getTenantDetail(record.id);
    const data = response.data;
    setEditingId(record.id);
    tenantForm.setFieldsValue({
      id: data.id,
      name: data.name,
      packageId: data.packageId || '',
      adminLoginname: data.adminLoginname || '',
      adminPassword: '',
      hasExpire: data.hasExpire,
      expireAt: data.expireAt ? dayjs(Number(data.expireAt)) : null,
      disabled: data.disabled,
    });
    setModalOpen(true);
  };

  const openExpire = (record: SysTenantRecord) => {
    expireForm.setFieldsValue({
      id: record.id,
      hasExpire: record.hasExpire,
      expireAt: record.expireAt ? dayjs(Number(record.expireAt)) : null,
    });
    setExpireModalOpen(true);
  };

  const submitTenant = async () => {
    const values = await tenantForm.validateFields();
    setSubmitting(true);
    try {
      const payload = {
        ...values,
        expireAt: normalizeExpireAt(values.expireAt, values.hasExpire),
      };
      if (editingId) {
        await updateTenant(payload);
      } else {
        await createTenant(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const submitExpire = async () => {
    const values = await expireForm.validateFields();
    setSubmitting(true);
    try {
      await updateTenantExpire({
        ...values,
        expireAt: normalizeExpireAt(values.expireAt, values.hasExpire),
      });
      setExpireModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysTenantRecord>[]>(
    () => [
      {
        title: '租户名称',
        dataIndex: 'name',
      },
      {
        title: '套餐',
        key: 'tenantPackage',
        search: false,
        render: (_, record) => record.tenantPackage?.name || '-',
      },
      {
        title: '管理员账号',
        dataIndex: 'adminLoginname',
      },
      {
        title: '到期时间',
        key: 'expireAt',
        search: false,
        render: (_, record) =>
          record.hasExpire ? dayjs(Number(record.expireAt)).format('YYYY-MM-DD HH:mm:ss') : '永久有效',
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
            disabled={
              record.id === PLATFORM_TENANT_ID ||
              !access.hasPermission('sys.manage.tenant.update')
            }
            onChange={async (checked) => {
              await updateTenantDisabled({
                id: record.id,
                disabled: !checked,
              });
              actionRef.current?.reload();
            }}
          />
        ),
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        search: false,
        valueType: 'dateTime',
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
                disabled:
                  record.id === PLATFORM_TENANT_ID ||
                  !access.hasPermission('sys.manage.tenant.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'expire',
                label: '到期时间',
                icon: <ClockCircleOutlined />,
                disabled:
                  record.id === PLATFORM_TENANT_ID ||
                  !access.hasPermission('sys.manage.tenant.update'),
                onClick: () => openExpire(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled:
                  record.id === PLATFORM_TENANT_ID ||
                  !access.hasPermission('sys.manage.tenant.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除租户',
                    content: `确定删除租户 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteTenant(record.id);
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
    <PageContainer title="租户管理">
      <PlatformProTable<SysTenantRecord, { current?: number; pageSize?: number; name?: string }>
        persistenceKey="platform-sys-tenant-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="租户列表"
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.tenant.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
          <Button
            key="package"
            icon={<TagsOutlined />}
            disabled={!access.hasPermission('sys.manage.tenant.package')}
            onClick={() => history.push('/platform/sys/tenant/package')}
          >
            套餐管理
          </Button>,
        ]}
        request={async (params) => {
          const response = await getTenantPage({
            name: params.name || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
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
        title={editingId ? '修改租户' : '新增租户'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submitTenant}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form<TenantFormValues> form={tenantForm} layout="vertical">
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="name" label="租户名称" rules={[{ required: true, message: '请输入租户名称' }]}>
            <Input placeholder="请输入租户名称" />
          </Form.Item>
          <Form.Item label="租户套餐" required>
            <Form.Item
              noStyle
              name="packageId"
              rules={[{ required: true, message: '请选择租户套餐' }]}
            >
              <Radio.Group
                options={packages
                  .filter((item) => item?.id)
                  .map((item) => ({ label: item.name, value: item.id }))}
              />
            </Form.Item>
          </Form.Item>
          <Form.Item
            name="adminLoginname"
            label="管理员账号"
            rules={[{ required: true, message: '请输入管理员账号' }]}
          >
            <Input placeholder="请输入管理员账号" />
          </Form.Item>
          <Form.Item
            name="adminPassword"
            label={editingId ? '管理员密码（留空不改）' : '管理员密码'}
            rules={editingId ? [] : [{ required: true, message: '请输入管理员密码' }]}
          >
            <Input.Password placeholder="请输入管理员密码" />
          </Form.Item>
          <Form.Item name="hasExpire" label="是否到期">
            <Radio.Group
              options={[
                { label: '否', value: false },
                { label: '是', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item noStyle shouldUpdate>
            {({ getFieldValue }) =>
              getFieldValue('hasExpire') ? (
                <Form.Item
                  name="expireAt"
                  label="到期时间"
                  rules={[{ required: true, message: '请选择到期时间' }]}
                >
                  <DatePicker showTime style={{ width: '100%' }} />
                </Form.Item>
              ) : null
            }
          </Form.Item>
          <Form.Item name="disabled" label="租户状态">
            <Radio.Group
              options={[
                { label: '启用', value: false },
                { label: '禁用', value: true },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改到期时间"
        open={expireModalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submitExpire}
        onCancel={() => setExpireModalOpen(false)}
        destroyOnHidden
      >
        <Form<ExpireFormValues> form={expireForm} layout="vertical">
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="hasExpire" label="是否到期">
            <Radio.Group
              options={[
                { label: '否', value: false },
                { label: '是', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item noStyle shouldUpdate>
            {({ getFieldValue }) =>
              getFieldValue('hasExpire') ? (
                <Form.Item
                  name="expireAt"
                  label="到期时间"
                  rules={[{ required: true, message: '请选择到期时间' }]}
                >
                  <DatePicker showTime style={{ width: '100%' }} />
                </Form.Item>
              ) : null
            }
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default TenantPage;
