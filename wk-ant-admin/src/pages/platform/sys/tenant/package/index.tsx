import {
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history, useAccess } from '@umijs/max';
import { App, Button, Form, Input, Modal, Radio, Switch, Tree } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import {
  createTenantPackage,
  deleteTenantPackage,
  getTenantPackageDetail,
  getTenantPackageMeta,
  getTenantPackagePage,
  updateTenantPackage,
  updateTenantPackageDisabled,
} from '@/services/budiot/sys/tenant';
import type { SysTenantPackageRecord, TenantMenuTreeNode } from '@/services/budiot/typing';

type TenantPackageFormValues = {
  id?: string;
  name: string;
  note?: string;
  disabled: boolean;
  menuIds: string[];
};

const toTreeData = (items: TenantMenuTreeNode[]): any[] =>
  items.map((item) => ({
    key: item.id,
    title: item.label || item.name || item.id,
    children: item.children ? toTreeData(item.children) : undefined,
  }));

const TenantPackagePage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [packageForm] = Form.useForm<TenantPackageFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [menuTree, setMenuTree] = React.useState<TenantMenuTreeNode[]>([]);
  const [checkedKeys, setCheckedKeys] = React.useState<React.Key[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();

  React.useEffect(() => {
    getTenantPackageMeta()
      .then((response) => setMenuTree(response.data.menuTree || []))
      .catch(() => undefined);
  }, []);

  const openCreate = () => {
    setEditingId(undefined);
    setCheckedKeys([]);
    packageForm.resetFields();
    packageForm.setFieldsValue({
      name: '',
      note: '',
      disabled: false,
      menuIds: [],
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysTenantPackageRecord) => {
    const response = await getTenantPackageDetail(record.id);
    setEditingId(record.id);
    setCheckedKeys(response.data.menuIds || []);
    packageForm.setFieldsValue({
      id: response.data.package.id,
      name: response.data.package.name,
      note: response.data.package.note || '',
      disabled: response.data.package.disabled,
      menuIds: response.data.menuIds || [],
    });
    setModalOpen(true);
  };

  const submitPackage = async () => {
    const values = await packageForm.validateFields();
    setSubmitting(true);
    try {
      const payload = {
        ...values,
        menuIds: checkedKeys.filter(
          (item): item is string => typeof item === 'string' && !item.startsWith('app-'),
        ),
      };
      if (editingId) {
        await updateTenantPackage(payload);
      } else {
        await createTenantPackage(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysTenantPackageRecord>[]>(
    () => [
      {
        title: '套餐名称',
        dataIndex: 'name',
      },
      {
        title: '说明',
        dataIndex: 'note',
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        search: false,
        width: 100,
        render: (_, record) => (
          <Switch
            checked={!record.disabled}
            disabled={!access.hasPermission('sys.manage.tenant.package.update')}
            onChange={async (checked) => {
              await updateTenantPackageDisabled({
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
        render: (_, record) => [
          <Button
            key="edit"
            type="link"
            icon={<EditOutlined />}
            disabled={!access.hasPermission('sys.manage.tenant.package.update')}
            onClick={() => openEdit(record)}
          >
            修改
          </Button>,
          <Button
            key="delete"
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={!access.hasPermission('sys.manage.tenant.package.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认删除套餐',
                content: `确定删除套餐 ${record.name} 吗？`,
                onOk: async () => {
                  await deleteTenantPackage(record.id);
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
    <PageContainer title="租户套餐">
      <PlatformProTable<
        SysTenantPackageRecord,
        { current?: number; pageSize?: number; name?: string }
      >
        persistenceKey="platform-sys-tenant-package-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="套餐列表"
        toolBarRender={() => [
          <Button key="back" icon={<ArrowLeftOutlined />} onClick={() => history.push('/platform/sys/tenant')}>
            返回
          </Button>,
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.tenant.package.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getTenantPackagePage({
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
        title={editingId ? '修改套餐' : '新增套餐'}
        open={modalOpen}
        forceRender
        width={760}
        confirmLoading={submitting}
        onOk={submitPackage}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form<TenantPackageFormValues> form={packageForm} layout="vertical">
          <Form.Item name="name" label="套餐名称" rules={[{ required: true, message: '请输入套餐名称' }]}>
            <Input placeholder="请输入套餐名称" />
          </Form.Item>
          <Form.Item name="note" label="套餐说明">
            <Input.TextArea rows={3} placeholder="请输入套餐说明" />
          </Form.Item>
          <Form.Item name="disabled" label="套餐状态">
            <Radio.Group
              options={[
                { label: '启用', value: false },
                { label: '禁用', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item label="菜单权限">
            <Tree
              checkable
              defaultExpandAll
              checkedKeys={checkedKeys}
              treeData={toTreeData(menuTree)}
              onCheck={(keys) => setCheckedKeys(Array.isArray(keys) ? keys : keys.checked)}
            />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default TenantPackagePage;
