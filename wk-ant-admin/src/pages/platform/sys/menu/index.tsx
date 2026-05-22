import {
  AppstoreOutlined,
  DeleteOutlined,
  EditOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  PlusOutlined,
  SortAscendingOutlined,
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
  Select,
  Space,
  Switch,
  Tag,
  Tooltip,
  Tree,
  TreeSelect,
  Typography,
} from 'antd';
import * as React from 'react';
import IconPicker, { renderNamedIcon } from '@/components/IconPicker';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createMenu,
  deleteMenu,
  getMenuApps,
  getMenuDetail,
  getMenuList,
  sortMenus,
  updateMenu,
  updateMenuData,
  updateMenuDisabled,
} from '@/services/budiot/sys/menu';
import type {
  BudiotAppOption,
  SysMenuButtonRecord,
  SysMenuDetail,
  SysMenuRecord,
} from '@/services/budiot/typing';
import { buildTree, collectTreeKeys } from '@/utils/tree';

interface MenuFormValues {
  id?: string;
  parentId?: string;
  appId?: string;
  name: string;
  alias?: string;
  permission: string;
  href?: string;
  icon?: string;
  type?: string;
  showit: boolean;
  disabled: boolean;
  children: 'true' | 'false';
  buttons: SysMenuButtonRecord[];
}

interface PermissionFormValues {
  id?: string;
  name: string;
  alias?: string;
  permission: string;
  disabled: boolean;
}

type MenuTableParams = {
  name?: string;
  href?: string;
  appId?: string;
};

type SortTreeNode = {
  key: string;
  title: string;
  parentId?: string;
  children?: SortTreeNode[];
};

const defaultMenuFormValues = (): MenuFormValues => ({
  parentId: '',
  name: '',
  permission: '',
  alias: '',
  href: '',
  icon: '',
  type: 'menu',
  showit: true,
  disabled: false,
  children: 'false',
  buttons: [],
});

const toTreeSelectData = (items: SysMenuRecord[]): any[] =>
  items.map((item) => ({
    value: item.id,
    title: item.name,
    children: item.children ? toTreeSelectData(item.children) : undefined,
  }));

const toSortTreeData = (items: SysMenuRecord[]): SortTreeNode[] =>
  items.map((item) => ({
    key: item.id,
    title: item.name,
    parentId: item.parentId,
    children: item.children?.length ? toSortTreeData(item.children) : undefined,
  }));

const moveSortNode = (
  tree: SortTreeNode[],
  dragKey: React.Key,
  dropKey: React.Key,
  position: 'before' | 'after',
) => {
  if (dragKey === dropKey) {
    return tree;
  }
  const clone = JSON.parse(JSON.stringify(tree)) as SortTreeNode[];
  const removeNode = (items: SortTreeNode[]): SortTreeNode | undefined => {
    for (let index = 0; index < items.length; index += 1) {
      const item = items[index];
      if (item.key === dragKey) {
        return items.splice(index, 1)[0];
      }
      if (item.children?.length) {
        const removed = removeNode(item.children);
        if (removed) {
          return removed;
        }
      }
    }
    return undefined;
  };

  const draggedNode = removeNode(clone);
  if (!draggedNode) {
    return clone;
  }

  const insertNode = (items: SortTreeNode[]): boolean => {
    for (let index = 0; index < items.length; index += 1) {
      const item = items[index];
      if (item.key === dropKey) {
        items.splice(position === 'before' ? index : index + 1, 0, draggedNode);
        return true;
      }
      if (item.children?.length && insertNode(item.children)) {
        return true;
      }
    }
    return false;
  };

  return insertNode(clone) ? clone : tree;
};

const flattenSortIds = (items: SortTreeNode[], ids: string[] = []) => {
  items.forEach((item) => {
    ids.push(item.key);
    if (item.children?.length) {
      flattenSortIds(item.children, ids);
    }
  });
  return ids;
};

const normalizeMenuDetail = (detail: SysMenuDetail): MenuFormValues => ({
  id: detail.id,
  parentId: detail.parentId || '',
  appId: detail.appId,
  name: detail.name || '',
  alias: detail.alias || '',
  permission: detail.permission || '',
  href: detail.href || '',
  icon: detail.icon || '',
  type: detail.type || '',
  showit: detail.showit ?? true,
  disabled: detail.disabled ?? false,
  children: detail.children === 'true' ? 'true' : 'false',
  buttons: (detail.buttons || []).map((item, index) => ({
    key: item.id || item.key || `${detail.id}-${index}`,
    name: item.name || '',
    permission: item.permission || '',
  })),
});

const normalizeMenuPayload = (
  values: MenuFormValues,
  selectedAppId?: string,
) => ({
  ...values,
  parentId: values.parentId || '',
  appId: values.appId || selectedAppId || '',
  alias: values.alias || '',
  href: values.href || '',
  icon: values.icon || '',
  type: values.type || 'menu',
  buttons: undefined,
});

const MenuPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [menuForm] = Form.useForm<MenuFormValues>();
  const [permissionForm] = Form.useForm<PermissionFormValues>();
  const [appsLoading, setAppsLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [apps, setApps] = React.useState<BudiotAppOption[]>([]);
  const [selectedAppId, setSelectedAppId] = React.useState<string>();
  const [tableData, setTableData] = React.useState<SysMenuRecord[]>([]);
  const [menuOptions, setMenuOptions] = React.useState<SysMenuRecord[]>([]);
  const [expanded, setExpanded] = React.useState(true);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);
  const [menuModalOpen, setMenuModalOpen] = React.useState(false);
  const [permissionModalOpen, setPermissionModalOpen] = React.useState(false);
  const [editingMenuId, setEditingMenuId] = React.useState<string>();
  const [sortOpen, setSortOpen] = React.useState(false);
  const [sortSubmitting, setSortSubmitting] = React.useState(false);
  const [sortTreeData, setSortTreeData] = React.useState<SortTreeNode[]>([]);

  const expandedRowKeys = React.useMemo(
    () => (expanded ? collectTreeKeys(tableData) : []),
    [expanded, tableData],
  );

  React.useEffect(() => {
    const initialize = async () => {
      setAppsLoading(true);
      try {
        const response = await getMenuApps();
        const nextApps = response.data.apps || [];
        setApps(nextApps);
        if (nextApps[0]?.id) {
          setSelectedAppId(nextApps[0].id);
        }
      } finally {
        setAppsLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const openCreate = (record?: SysMenuRecord) => {
    setEditingMenuId(undefined);
    menuForm.resetFields();
    menuForm.setFieldsValue({
      ...defaultMenuFormValues(),
      appId: selectedAppId,
      parentId: record?.id || '',
    });
    setMenuModalOpen(true);
  };

  const openEdit = async (record: SysMenuRecord) => {
    const response = await getMenuDetail(record.id);
    setEditingMenuId(record.id);
    menuForm.setFieldsValue(normalizeMenuDetail(response.data));
    setMenuModalOpen(true);
  };

  const openEditPermission = async (record: SysMenuRecord) => {
    const response = await getMenuDetail(record.id);
    const detail = response.data;
    permissionForm.setFieldsValue({
      id: detail.id,
      name: detail.name || '',
      alias: detail.alias || '',
      permission: detail.permission || '',
      disabled: detail.disabled ?? false,
    });
    setPermissionModalOpen(true);
  };

  const handleDisabledChange = async (record: SysMenuRecord, checked: boolean) => {
    setSwitchingIds((prev) => [...prev, record.id]);
    try {
      await updateMenuDisabled({
        id: record.id,
        path: record.path,
        disabled: checked,
      });
      actionRef.current?.reload();
    } finally {
      setSwitchingIds((prev) => prev.filter((item) => item !== record.id));
    }
  };

  const submitMenu = async () => {
    await menuForm.validateFields();
    const values = menuForm.getFieldsValue(true) as MenuFormValues;
    const buttons =
      values.children === 'true'
        ? (values.buttons || []).map((item, index) => ({
            key: item.key || `${Date.now()}-${index}`,
            name: item.name,
            permission: item.permission,
          }))
        : [];
    const payload = {
      ...normalizeMenuPayload(values, selectedAppId),
    };
    setSubmitting(true);
    try {
      if (editingMenuId) {
        await updateMenu(JSON.stringify(payload), JSON.stringify(buttons));
      } else {
        await createMenu(JSON.stringify(payload), JSON.stringify(buttons), selectedAppId || '');
      }
      setMenuModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const submitPermission = async () => {
    await permissionForm.validateFields();
    const values = permissionForm.getFieldsValue(true) as PermissionFormValues;
    setSubmitting(true);
    try {
      await updateMenuData({ ...values });
      setPermissionModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysMenuRecord>[]>(
    () => [
      {
        title: '菜单名称',
        dataIndex: 'name',
        width: 260,
        render: (_, record) => (
          <Space size={8}>
            {record.icon ? renderNamedIcon(record.icon) : null}
            {record.icon ? <Tag>{record.icon}</Tag> : null}
            <span>{record.name}</span>
          </Space>
        ),
      },
      {
        title: 'URL',
        dataIndex: 'href',
        render: (_, record) =>
          record.href ? <Typography.Text code>{record.href}</Typography.Text> : '-',
      },
      {
        title: '菜单类型',
        dataIndex: 'type',
        search: false,
        width: 110,
        render: (_, record) =>
          record.type === 'menu' ? <Tag color="processing">菜单</Tag> : <Tag>权限</Tag>,
      },
      {
        title: '权限标识',
        dataIndex: 'permission',
        search: false,
        width: 220,
        render: (_, record) =>
          record.permission ? <Typography.Text code>{record.permission}</Typography.Text> : '-',
      },
      {
        title: '是否展示',
        dataIndex: 'showit',
        search: false,
        width: 100,
        render: (_, record) =>
          record.showit ? <Tag color="success">显示</Tag> : <Tag>隐藏</Tag>,
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        search: false,
        width: 110,
        render: (_, record) => (
          <Switch
            checked={!record.disabled}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            loading={switchingIds.includes(record.id)}
            disabled={!access.hasPermission('sys.manage.menu.update')}
            onChange={(checked) => handleDisabledChange(record, !checked)}
          />
        ),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 200,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'child',
                label: '新增下级',
                icon: <PlusOutlined />,
                hidden: record.type !== 'menu',
                disabled: !access.hasPermission('sys.manage.menu.create'),
                onClick: () => openCreate(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('sys.manage.menu.update'),
                onClick: () =>
                  record.type === 'menu' ? openEdit(record) : openEditPermission(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('sys.manage.menu.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除菜单',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteMenu(record.id);
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
    [access, modal, switchingIds],
  );

  return (
    <PageContainer title="菜单管理">
      <PlatformProTable<SysMenuRecord, MenuTableParams>
        persistenceKey="platform-sys-menu-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="菜单列表"
        pagination={false}
        params={{ appId: selectedAppId }}
        scroll={{ x: 1300 }}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.menu.create')}
            onClick={() => openCreate()}
          >
            新增菜单
          </Button>,
          <Select
            key="app"
            style={{ minWidth: 220 }}
            placeholder="切换应用"
            loading={appsLoading}
            value={selectedAppId}
            onChange={(value) => setSelectedAppId(value)}
            options={apps.map((item) => ({
              label: item.name,
              value: item.id,
            }))}
            suffixIcon={<AppstoreOutlined />}
          />,
          <Button
            key="expand"
            icon={expanded ? <FolderOutlined /> : <FolderOpenOutlined />}
            onClick={() => setExpanded((prev) => !prev)}
          >
            {expanded ? '折叠' : '展开'}
          </Button>,
          <Button
            key="sort"
            icon={<SortAscendingOutlined />}
            disabled={!selectedAppId || !access.hasPermission('sys.manage.menu.update')}
            onClick={() => {
              setSortTreeData(toSortTreeData(tableData));
              setSortOpen(true);
            }}
          >
            排序
          </Button>,
        ]}
        request={async (params) => {
          if (!selectedAppId) {
            return { data: [], total: 0, success: true };
          }
          const response = await getMenuList({
            appId: selectedAppId,
            name: params.name,
            href: params.href,
          });
          const treeData = buildTree(response.data || []);
          setTableData(treeData);
          setMenuOptions(treeData);
          return {
            data: treeData,
            total: treeData.length,
            success: true,
          };
        }}
        expandable={{
          expandedRowKeys,
          defaultExpandAllRows: true,
        }}
      />

      <Modal
        title={editingMenuId ? '修改菜单' : '新增菜单'}
        open={menuModalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submitMenu}
        onCancel={() => setMenuModalOpen(false)}
        destroyOnHidden
        width={760}
      >
        <Form<MenuFormValues>
          form={menuForm}
          layout="vertical"
          onValuesChange={(changedValues, values) => {
            if (typeof changedValues.permission === 'string') {
              menuForm.setFieldValue('alias', changedValues.permission);
            }
            if (changedValues.children === 'false') {
              menuForm.setFieldValue('buttons', []);
            }
            if (changedValues.children === 'true' && !(values.buttons || []).length) {
              menuForm.setFieldValue('buttons', [{ key: Date.now(), name: '', permission: '' }]);
            }
          }}
        >
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
            <Form.Item name="parentId" label="上级菜单">
              <TreeSelect
                allowClear
                treeDefaultExpandAll
                placeholder="选择上级菜单"
                treeData={toTreeSelectData(menuOptions)}
              />
            </Form.Item>
            <Form.Item name="appId" label="所属应用">
              <Select
                disabled
                options={apps.map((item) => ({ label: item.name, value: item.id }))}
              />
            </Form.Item>
            <Form.Item
              name="name"
              label="菜单名称"
              rules={[{ required: true, message: '请输入菜单名称' }]}
            >
              <Input placeholder="菜单名称" />
            </Form.Item>
            <Form.Item
              name="permission"
              label="权限标识"
              rules={[{ required: true, message: '请输入权限标识' }]}
            >
              <Input placeholder="例如 sys.manage.menu" />
            </Form.Item>
            <Form.Item name="alias" label="菜单别名">
              <Input disabled placeholder="菜单别名" />
            </Form.Item>
            <Form.Item name="href" label="访问路径">
              <Input placeholder="后台访问路径前缀为 /platform/" />
            </Form.Item>
            <Form.Item name="icon" label="菜单图标">
              <IconPicker />
            </Form.Item>
            <Form.Item name="showit" label="是否显示">
              <Radio.Group
                options={[
                  { label: '显示', value: true },
                  { label: '隐藏', value: false },
                ]}
              />
            </Form.Item>
            <Form.Item name="disabled" label="菜单状态">
              <Radio.Group
                options={[
                  { label: '启用', value: false },
                  { label: '禁用', value: true },
                ]}
              />
            </Form.Item>
            <Form.Item name="children" label="有子权限">
              <Radio.Group
                options={[
                  { label: '否', value: 'false' },
                  { label: '是', value: 'true' },
                ]}
              />
            </Form.Item>
          </div>

          <Form.Item noStyle shouldUpdate>
            {({ getFieldValue }) =>
              getFieldValue('children') === 'true' ? (
                <Form.List name="buttons">
                  {(fields, { add, remove }) => (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                      <div>
                        <Button
                          type="dashed"
                          icon={<PlusOutlined />}
                          onClick={() => add({ key: Date.now(), name: '', permission: '' })}
                        >
                          添加权限
                        </Button>
                      </div>
                      {fields.map((field, index) => (
                        <div
                          key={field.key}
                          style={{
                            display: 'grid',
                            gridTemplateColumns: '180px minmax(0, 280px) auto',
                            gap: 12,
                            alignItems: 'end',
                          }}
                        >
                          <Form.Item
                            {...field}
                            label={`权限${index + 1}`}
                            name={[field.name, 'name']}
                            rules={[{ required: true, message: '请输入权限名称' }]}
                            style={{ marginBottom: 0 }}
                          >
                            <Input placeholder="权限名称" />
                          </Form.Item>
                          <Form.Item
                            {...field}
                            label="权限标识"
                            name={[field.name, 'permission']}
                            rules={[{ required: true, message: '请输入权限标识' }]}
                            style={{ marginBottom: 0 }}
                          >
                            <Input placeholder="权限标识" />
                          </Form.Item>
                          <Tooltip title="删除权限">
                            <Button
                              danger
                              type="text"
                              size="small"
                              icon={<DeleteOutlined />}
                              onClick={() => remove(field.name)}
                            />
                          </Tooltip>
                        </div>
                      ))}
                    </div>
                  )}
                </Form.List>
              ) : null
            }
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="修改权限"
        open={permissionModalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submitPermission}
        onCancel={() => setPermissionModalOpen(false)}
        destroyOnHidden
      >
        <Form<PermissionFormValues>
          form={permissionForm}
          layout="vertical"
          onValuesChange={(changedValues) => {
            if (typeof changedValues.permission === 'string') {
              permissionForm.setFieldValue('alias', changedValues.permission);
            }
          }}
        >
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            name="name"
            label="权限名称"
            rules={[{ required: true, message: '请输入权限名称' }]}
          >
            <Input placeholder="权限名称" />
          </Form.Item>
          <Form.Item
            name="permission"
            label="权限标识"
            rules={[{ required: true, message: '请输入权限标识' }]}
          >
            <Input placeholder="请输入权限标识" />
          </Form.Item>
          <Form.Item name="alias" label="权限别名">
            <Input disabled />
          </Form.Item>
          <Form.Item name="disabled" label="权限状态">
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
        title="菜单排序"
        open={sortOpen}
        confirmLoading={sortSubmitting}
        onOk={async () => {
          if (!selectedAppId) {
            return;
          }
          setSortSubmitting(true);
          try {
            await sortMenus({
              ids: flattenSortIds(sortTreeData).toString(),
              appId: selectedAppId,
            });
            setSortOpen(false);
            actionRef.current?.reload();
          } finally {
            setSortSubmitting(false);
          }
        }}
        onCancel={() => setSortOpen(false)}
        destroyOnHidden
      >
        <Tree
          draggable
          blockNode
          treeData={sortTreeData}
          allowDrop={({ dragNode, dropNode, dropPosition }) => {
            const dragParentId = (dragNode as SortTreeNode).parentId || '';
            const dropParentId = (dropNode as SortTreeNode).parentId || '';
            return dragParentId === dropParentId && dropPosition !== 0;
          }}
          onDrop={(info) => {
            const dragNode = info.dragNode as SortTreeNode;
            const dropNode = info.node as SortTreeNode;
            if ((dragNode.parentId || '') !== (dropNode.parentId || '')) return;
            if (!info.dropToGap) return;
            setSortTreeData((prev) =>
              moveSortNode(prev, dragNode.key, dropNode.key, info.dropPosition < 0 ? 'before' : 'after'),
            );
          }}
        />
      </Modal>
    </PageContainer>
  );
};

export default MenuPage;
