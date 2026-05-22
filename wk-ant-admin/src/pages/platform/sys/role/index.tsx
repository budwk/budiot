import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SaveOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { TabsProps, TableColumnsType } from 'antd';
import {
  App,
  Button,
  Empty,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Tree,
  Typography,
} from 'antd';
import * as React from 'react';
import {
  createRole,
  createRoleGroup,
  deleteRole,
  deleteRoleGroup,
  getRoleApps,
  getRoleGroups,
  getRoleMenus,
  getRolePosts,
  getRoleSelectableUsers,
  getRoleUnits,
  getRoleUsers,
  linkRoleUsers,
  saveRoleMenus,
  unlinkRoleUser,
  updateRole,
  updateRoleGroup,
} from '@/services/budiot/sys/role';
import type {
  BudiotAppOption,
  SysPostRecord,
  SysRoleGroupRecord,
  SysRoleRecord,
  SysUnitRecord,
  SysUserRecord,
} from '@/services/budiot/typing';
import { buildTree, collectTreeKeys } from '@/utils/tree';

type RoleFormMode = 'create' | 'update';
type RoleEntityType = 'role' | 'group';

interface RoleFormValues {
  id?: string;
  type: RoleEntityType;
  name: string;
  code?: string;
  note?: string;
  groupId?: string;
  disabled?: boolean;
}

const toMenuTreeData = (items: any[]): any[] =>
  items.map((item) => ({
    id: item.id,
    key: item.id,
    title: item.name,
    type: item.type,
    children: item.children ? toMenuTreeData(item.children) : undefined,
  }));

const RolePage = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const [roleSearchForm] = Form.useForm<{ username?: string }>();
  const [linkUserForm] = Form.useForm<{ username?: string }>();
  const [roleForm] = Form.useForm<RoleFormValues>();

  const [units, setUnits] = React.useState<SysUnitRecord[]>([]);
  const [apps, setApps] = React.useState<BudiotAppOption[]>([]);
  const [posts, setPosts] = React.useState<SysPostRecord[]>([]);
  const [groups, setGroups] = React.useState<SysRoleGroupRecord[]>([]);
  const [selectedUnitId, setSelectedUnitId] = React.useState<string>();
  const [selectedRole, setSelectedRole] = React.useState<SysRoleRecord>();
  const [activeTab, setActiveTab] = React.useState<string>('USERLIST');

  const [userLoading, setUserLoading] = React.useState(false);
  const [tableData, setTableData] = React.useState<SysUserRecord[]>([]);
  const [pageNo, setPageNo] = React.useState(1);
  const [pageSize, setPageSize] = React.useState(10);
  const [total, setTotal] = React.useState(0);

  const [linkModalOpen, setLinkModalOpen] = React.useState(false);
  const [linkLoading, setLinkLoading] = React.useState(false);
  const [linkTableData, setLinkTableData] = React.useState<SysUserRecord[]>([]);
  const [linkPageNo, setLinkPageNo] = React.useState(1);
  const [linkPageSize, setLinkPageSize] = React.useState(10);
  const [linkTotal, setLinkTotal] = React.useState(0);
  const [selectedLinkUsers, setSelectedLinkUsers] = React.useState<SysUserRecord[]>([]);

  const [roleModalOpen, setRoleModalOpen] = React.useState(false);
  const [roleModalMode, setRoleModalMode] = React.useState<RoleFormMode>('create');
  const [roleModalTitle, setRoleModalTitle] = React.useState('新增角色|角色组');
  const [roleSubmitting, setRoleSubmitting] = React.useState(false);

  const [treeStrict, setTreeStrict] = React.useState(true);
  const [menuTreeLoading, setMenuTreeLoading] = React.useState(false);
  const [menuTreeData, setMenuTreeData] = React.useState<any[]>([]);
  const [menuCheckedKeys, setMenuCheckedKeys] = React.useState<string[]>([]);
  const [menuExpandedKeys, setMenuExpandedKeys] = React.useState<React.Key[]>([]);

  const currentAppId =
    activeTab === 'USERLIST' ? apps[0]?.id : activeTab || apps[0]?.id;

  const loadRoleGroups = React.useCallback(
    async (unitId?: string) => {
      if (!unitId) {
        setGroups([]);
        setSelectedRole(undefined);
        return;
      }
      const response = await getRoleGroups(unitId);
      const nextGroups = response.data || [];
      setGroups(nextGroups);
      const firstRole = nextGroups[0]?.roles?.[0];
      setSelectedRole(firstRole);
      setPageNo(1);
      return firstRole;
    },
    [],
  );

  const loadRoleUsers = React.useCallback(
    async (
      role?: SysRoleRecord,
      paging?: { pageNo?: number; pageSize?: number },
      username?: string,
    ) => {
      if (!role?.id) {
        setTableData([]);
        setTotal(0);
        return;
      }
      setUserLoading(true);
      try {
        const response = await getRoleUsers({
          roleId: role.id,
          username:
            username === undefined
              ? roleSearchForm.getFieldValue('username')
              : username,
          pageNo: paging?.pageNo ?? pageNo,
          pageSize: paging?.pageSize ?? pageSize,
          pageOrderName: 'updatedAt',
          pageOrderBy: 'descending',
        });
        setTableData(response.data.list || []);
        setTotal(response.data.totalCount || 0);
      } finally {
        setUserLoading(false);
      }
    },
    [pageNo, pageSize, roleSearchForm],
  );

  const loadSelectableUsers = React.useCallback(
    async (
      role?: SysRoleRecord,
      paging?: { pageNo?: number; pageSize?: number },
      username?: string,
    ) => {
      if (!role?.id || !selectedUnitId) {
        setLinkTableData([]);
        setLinkTotal(0);
        return;
      }
      setLinkLoading(true);
      try {
        const response = await getRoleSelectableUsers({
          roleId: role.id,
          unitId: selectedUnitId,
          username:
            username === undefined
              ? linkUserForm.getFieldValue('username')
              : username,
          pageNo: paging?.pageNo ?? linkPageNo,
          pageSize: paging?.pageSize ?? linkPageSize,
          pageOrderName: 'updatedAt',
          pageOrderBy: 'descending',
        });
        setLinkTableData(response.data.list || []);
        setLinkTotal(response.data.totalCount || 0);
      } finally {
        setLinkLoading(false);
      }
    },
    [linkPageNo, linkPageSize, linkUserForm, selectedUnitId],
  );

  const loadRoleMenus = React.useCallback(
    async (role?: SysRoleRecord, appId?: string) => {
      if (!role?.id || !appId) {
        setMenuTreeData([]);
        setMenuCheckedKeys([]);
        setMenuExpandedKeys([]);
        return;
      }
      setMenuTreeLoading(true);
      try {
        const response = await getRoleMenus(role.id, appId);
        const tree = buildTree(response.data.menuList || []);
        const menuTree = toMenuTreeData(tree);
        setMenuTreeData(menuTree);
        setMenuCheckedKeys((response.data.menuIds || []).map(String));
        setMenuExpandedKeys(collectTreeKeys(menuTree).map(String));
      } finally {
        setMenuTreeLoading(false);
      }
    },
    [],
  );

  React.useEffect(() => {
    const initialize = async () => {
      const [unitResponse, appResponse, postResponse] = await Promise.all([
        getRoleUnits(),
        getRoleApps(),
        getRolePosts(),
      ]);
      const nextUnits = unitResponse.data || [];
      const nextApps = appResponse.data || [];
      setUnits(nextUnits);
      setApps(nextApps);
      setPosts(postResponse.data || []);

      const firstUnitId = nextUnits[0]?.id;
      if (firstUnitId) {
        setSelectedUnitId(firstUnitId);
        const firstRole = await loadRoleGroups(firstUnitId);
        if (firstRole) {
          await loadRoleUsers(firstRole, { pageNo: 1, pageSize });
          if (nextApps[0]?.id) {
            await loadRoleMenus(firstRole, nextApps[0].id);
          }
        }
      }
    };

    initialize().catch(() => undefined);
  }, [loadRoleGroups, loadRoleMenus, loadRoleUsers, pageSize]);

  React.useEffect(() => {
    if (activeTab !== 'USERLIST') {
      loadRoleMenus(selectedRole, activeTab).catch(() => undefined);
    }
  }, [activeTab, loadRoleMenus, selectedRole]);

  const findPostName = React.useCallback(
    (postId?: string) => posts.find((item) => item.id === postId)?.name || '-',
    [posts],
  );

  const openRoleModal = (
    mode: RoleFormMode,
    type: RoleEntityType,
    record?: SysRoleRecord | SysRoleGroupRecord,
  ) => {
    setRoleModalMode(mode);
    setRoleModalTitle(
      mode === 'create' ? '新增角色|角色组' : type === 'role' ? '修改角色' : '修改角色组',
    );
    setRoleModalOpen(true);
    roleForm.resetFields();
    roleForm.setFieldsValue({
      type,
      id: (record as any)?.id,
      name: (record as any)?.name,
      code: (record as any)?.code,
      note: (record as any)?.note,
      groupId: type === 'role' ? (record as any)?.groupId || groups[0]?.id : undefined,
      disabled: type === 'role' ? (record as any)?.disabled ?? false : false,
    });
  };

  const submitRoleForm = async () => {
    const values = await roleForm.validateFields();
    if (!selectedUnitId) {
      message.error('请先选择单位');
      return;
    }

    setRoleSubmitting(true);
    try {
      const payload = {
        ...values,
        unitId: selectedUnitId,
      };
      if (roleModalMode === 'create') {
        if (values.type === 'group') {
          await createRoleGroup(payload);
        } else {
          await createRole(payload);
        }
      } else if (values.type === 'group') {
        await updateRoleGroup(payload);
      } else {
        await updateRole(payload);
      }
      setRoleModalOpen(false);
      const firstRole = await loadRoleGroups(selectedUnitId);
      if (firstRole) {
        await loadRoleUsers(firstRole, { pageNo: 1, pageSize });
        await loadRoleMenus(firstRole, currentAppId);
      }
    } finally {
      setRoleSubmitting(false);
    }
  };

  const handleUnitChange = async (value: string) => {
    setSelectedUnitId(value);
    setActiveTab('USERLIST');
    const firstRole = await loadRoleGroups(value);
    if (firstRole) {
      await loadRoleUsers(firstRole, { pageNo: 1, pageSize });
      await loadRoleMenus(firstRole, apps[0]?.id);
    }
  };

  const handleRoleSelect = async (role: SysRoleRecord) => {
    setSelectedRole(role);
    setPageNo(1);
    await loadRoleUsers(role, { pageNo: 1, pageSize });
    if (activeTab !== 'USERLIST') {
      await loadRoleMenus(role, currentAppId);
    }
  };

  const userColumns = React.useMemo<TableColumnsType<SysUserRecord>>(
    () => [
      {
        title: '用户',
        dataIndex: 'loginname',
        key: 'loginname',
        render: (_, record) => `${record.loginname}(${record.username})`,
      },
      {
        title: '单位',
        dataIndex: ['unit', 'name'],
        key: 'unit',
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '职务',
        dataIndex: 'postId',
        key: 'postId',
        render: (value) => findPostName(value),
      },
      {
        title: '操作',
        key: 'actions',
        width: 120,
        render: (_, record) => (
          <Button
            danger
            type="link"
            disabled={!access.hasPermission('sys.manage.role.delete')}
            onClick={() => {
              if (!selectedRole) return;
              modal.confirm({
                title: '确认移除用户',
                content: `确定从角色 ${selectedRole.name} 中移除 ${record.loginname}(${record.username}) 吗？`,
                onOk: async () => {
                  await unlinkRoleUser({
                    roleId: selectedRole.id,
                    roleCode: selectedRole.code,
                    id: record.id,
                    name: `${record.loginname}(${record.username})`,
                  });
                  await loadRoleUsers(selectedRole, { pageNo: 1, pageSize });
                },
              });
            }}
          >
            移除
          </Button>
        ),
      },
    ],
    [access, findPostName, loadRoleUsers, modal, pageSize, selectedRole],
  );

  const linkUserColumns = React.useMemo<TableColumnsType<SysUserRecord>>(
    () => [
      {
        title: '用户',
        dataIndex: 'loginname',
        key: 'loginname',
        render: (_, record) => `${record.loginname}(${record.username})`,
      },
      {
        title: '单位',
        dataIndex: ['unit', 'name'],
        key: 'unit',
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '职务',
        dataIndex: 'postId',
        key: 'postId',
        render: (value) => findPostName(value),
      },
    ],
    [findPostName],
  );

  const checkedKeysProp = treeStrict
    ? { checked: menuCheckedKeys, halfChecked: [] as string[] }
    : menuCheckedKeys;

  const tabItems = React.useMemo<TabsProps['items']>(
    () => [
      {
        key: 'USERLIST',
        label: '用户列表',
        children: (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                gap: 16,
                flexWrap: 'wrap',
              }}
            >
              <Form form={roleSearchForm} layout="inline" onFinish={async () => {
                setPageNo(1);
                await loadRoleUsers(selectedRole, { pageNo: 1, pageSize });
              }}>
                <Form.Item name="username">
                  <Input allowClear placeholder="请输入姓名或用户名" />
                </Form.Item>
                <Form.Item>
                  <Space>
                    <Button type="primary" htmlType="submit">
                      搜索
                    </Button>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={async () => {
                        roleSearchForm.resetFields();
                        setPageNo(1);
                        await loadRoleUsers(selectedRole, { pageNo: 1, pageSize }, '');
                      }}
                    >
                      重置
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
              <Button
                type="primary"
                icon={<UserAddOutlined />}
                disabled={
                  !selectedRole ||
                  selectedRole.code === 'public' ||
                  !access.hasPermission('sys.manage.role.update')
                }
                onClick={async () => {
                  setLinkModalOpen(true);
                  setSelectedLinkUsers([]);
                  setLinkPageNo(1);
                  await loadSelectableUsers(selectedRole, {
                    pageNo: 1,
                    pageSize: linkPageSize,
                  });
                }}
              >
                {selectedRole?.code === 'public' ? '公共角色不可关联用户' : '关联用户到角色'}
              </Button>
            </div>
            <Table<SysUserRecord>
              rowKey="id"
              loading={userLoading}
              columns={userColumns}
              dataSource={tableData}
              pagination={{
                current: pageNo,
                pageSize,
                total,
                showSizeChanger: true,
                onChange: async (nextPage, nextSize) => {
                  setPageNo(nextPage);
                  setPageSize(nextSize);
                  await loadRoleUsers(selectedRole, {
                    pageNo: nextPage,
                    pageSize: nextSize,
                  });
                },
              }}
            />
          </div>
        ),
      },
      ...apps.map((app) => ({
        key: app.id,
        label: app.name,
        children: (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Space wrap>
              <Button
                onClick={() =>
                  setMenuCheckedKeys(collectTreeKeys(menuTreeData).map(String))
                }
              >
                全选
              </Button>
              <Button onClick={() => setMenuCheckedKeys([])}>清空</Button>
              <Switch
                checked={!treeStrict}
                checkedChildren="勾选联动"
                unCheckedChildren="勾选不联动"
                onChange={(checked) => setTreeStrict(!checked)}
              />
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={menuTreeLoading}
                disabled={!selectedRole || !access.hasPermission('sys.manage.role.update')}
                onClick={async () => {
                  if (!selectedRole || !currentAppId) {
                    message.error('请先选择角色');
                    return;
                  }
                  await saveRoleMenus({
                    roleId: selectedRole.id,
                    roleCode: selectedRole.code,
                    appId: currentAppId,
                    menuIds: menuCheckedKeys.toString(),
                  });
                }}
              >
                保存权限
              </Button>
            </Space>
            <Tree
              key={currentAppId}
              checkable
              selectable={false}
              expandedKeys={menuExpandedKeys}
              checkStrictly={treeStrict}
              checkedKeys={checkedKeysProp}
              treeData={menuTreeData}
              onExpand={(keys) => setMenuExpandedKeys(keys)}
              onCheck={(keys) => {
                if (Array.isArray(keys)) {
                  setMenuCheckedKeys(keys as string[]);
                } else {
                  setMenuCheckedKeys((keys.checked || []) as string[]);
                }
              }}
            />
          </div>
        ),
      })),
    ],
    [
      access,
      apps,
      checkedKeysProp,
      currentAppId,
      linkPageSize,
      loadRoleUsers,
      loadSelectableUsers,
      menuCheckedKeys,
      menuTreeData,
      menuTreeLoading,
      pageNo,
      pageSize,
      roleSearchForm,
      selectedRole,
      tableData,
      total,
      treeStrict,
      userColumns,
      userLoading,
      message,
    ],
  );

  return (
    <PageContainer title="角色管理">
      <ProCard split="vertical">
        <ProCard title="角色树" colSpan="24%">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Select
              value={selectedUnitId}
              placeholder="请选择单位"
              onChange={handleUnitChange}
              options={units.map((item) => ({
                label: item.name,
                value: item.id,
              }))}
            />
            <Button
              type="link"
              icon={<PlusOutlined />}
              style={{ paddingInline: 0, justifyContent: 'flex-start' }}
              disabled={!access.hasPermission('sys.manage.role.create')}
              onClick={() => openRoleModal('create', 'role')}
            >
              新增角色|角色组
            </Button>
            {groups.length ? (
              groups.map((group) => (
                <ProCard
                  key={group.id}
                  title={group.name}
                  size="small"
                  extra={
                    <Space size={4}>
                      <Tooltip title="编辑角色组">
                        <Button
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          disabled={!access.hasPermission('sys.manage.role.update')}
                          onClick={() => openRoleModal('update', 'group', group)}
                        />
                      </Tooltip>
                      <Tooltip title="删除角色组">
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          disabled={!access.hasPermission('sys.manage.role.delete')}
                          onClick={() => {
                            modal.confirm({
                              title: '确认删除角色组',
                              content: `确定删除角色组 ${group.name} 吗？`,
                              onOk: async () => {
                                await deleteRoleGroup(group.id);
                                const firstRole = await loadRoleGroups(selectedUnitId);
                                if (firstRole) {
                                  await loadRoleUsers(firstRole, { pageNo: 1, pageSize });
                                  await loadRoleMenus(firstRole, currentAppId);
                                }
                              },
                            });
                          }}
                        />
                      </Tooltip>
                    </Space>
                  }
                >
                  {group.roles?.length ? (
                    <div>
                      {group.roles.map((role, index) => (
                        <div
                          key={role.id}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: 12,
                            cursor: 'pointer',
                            background:
                              selectedRole?.id === role.id
                                ? 'rgba(22,119,255,0.08)'
                                : 'transparent',
                            borderRadius: 6,
                            padding: '8px 8px',
                            borderBottom:
                              index === group.roles.length - 1
                                ? 'none'
                                : '1px solid rgba(5, 5, 5, 0.06)',
                          }}
                          onClick={() => handleRoleSelect(role)}
                        >
                          <Space>
                            <Typography.Text strong={selectedRole?.id === role.id}>
                              {role.name}
                            </Typography.Text>
                            {role.note ? <Tag>{role.note}</Tag> : null}
                          </Space>
                          <Space size={0}>
                            <Tooltip title="编辑角色">
                              <Button
                                type="text"
                                size="small"
                                icon={<EditOutlined />}
                                disabled={
                                  role.code === 'public' ||
                                  !access.hasPermission('sys.manage.role.update')
                                }
                                onClick={(event) => {
                                  event.stopPropagation();
                                  openRoleModal('update', 'role', role);
                                }}
                              />
                            </Tooltip>
                            <Tooltip title="删除角色">
                              <Button
                                type="text"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                                disabled={
                                  role.code === 'public' ||
                                  !access.hasPermission('sys.manage.role.delete')
                                }
                                onClick={(event) => {
                                  event.stopPropagation();
                                  modal.confirm({
                                    title: '确认删除角色',
                                    content: `确定删除角色 ${role.name} 吗？`,
                                    onOk: async () => {
                                      await deleteRole(role.id);
                                      const firstRole = await loadRoleGroups(selectedUnitId);
                                      if (firstRole) {
                                        await loadRoleUsers(firstRole, {
                                          pageNo: 1,
                                          pageSize,
                                        });
                                        await loadRoleMenus(firstRole, currentAppId);
                                      }
                                    },
                                  });
                                }}
                              />
                            </Tooltip>
                          </Space>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
                  )}
                </ProCard>
              ))
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无角色数据" />
            )}
          </div>
        </ProCard>
        <ProCard title={selectedRole ? `当前角色：${selectedRole.name}` : '角色详情'} colSpan="76%">
          <Tabs activeKey={activeTab} items={tabItems} onChange={setActiveTab} />
        </ProCard>
      </ProCard>

      <Modal
        title={roleModalTitle}
        open={roleModalOpen}
        forceRender
        confirmLoading={roleSubmitting}
        onOk={submitRoleForm}
        onCancel={() => setRoleModalOpen(false)}
        destroyOnHidden
      >
        <Form form={roleForm} layout="vertical" initialValues={{ type: 'role', disabled: false }}>
          <Form.Item name="type" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
            <Radio.Group disabled={roleModalMode === 'update'}>
              <Radio value="role">角色</Radio>
              <Radio value="group">角色组</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item noStyle shouldUpdate>
            {({ getFieldValue }) =>
              getFieldValue('type') === 'role' ? (
                <>
                  <Form.Item
                    name="groupId"
                    label="所属角色组"
                    rules={[{ required: true, message: '请选择角色组' }]}
                  >
                    <Select
                      placeholder="请选择角色组"
                      options={groups.map((item) => ({
                        label: item.name,
                        value: item.id,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item
                    name="name"
                    label="角色名称"
                    rules={[{ required: true, message: '请输入角色名称' }]}
                  >
                    <Input placeholder="请输入角色名称" />
                  </Form.Item>
                  <Form.Item
                    name="code"
                    label="角色代码"
                    rules={[{ required: true, message: '请输入角色代码' }]}
                  >
                    <Input placeholder="请输入角色代码" />
                  </Form.Item>
                  <Form.Item name="note" label="角色说明">
                    <Input placeholder="请输入角色说明" />
                  </Form.Item>
                  <Form.Item name="disabled" label="启用状态" valuePropName="checked">
                    <Switch checkedChildren="启用" unCheckedChildren="禁用" />
                  </Form.Item>
                </>
              ) : (
                <Form.Item
                  name="name"
                  label="组名"
                  rules={[{ required: true, message: '请输入组名' }]}
                >
                  <Input placeholder="请输入组名" />
                </Form.Item>
              )
            }
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="关联用户"
        open={linkModalOpen}
        onOk={async () => {
          if (!selectedRole || !selectedLinkUsers.length) {
            message.warning('请先选择用户');
            return;
          }
          await linkRoleUsers({
            roleId: selectedRole.id,
            roleCode: selectedRole.code,
            ids: selectedLinkUsers.map((item) => item.id).toString(),
            names: selectedLinkUsers
              .map((item) => `${item.loginname}(${item.username})`)
              .toString(),
          });
          setLinkModalOpen(false);
          await loadRoleUsers(selectedRole, { pageNo: 1, pageSize });
        }}
        onCancel={() => setLinkModalOpen(false)}
        width={900}
        destroyOnHidden
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Form form={linkUserForm} layout="inline" onFinish={async () => {
            setLinkPageNo(1);
            await loadSelectableUsers(selectedRole, { pageNo: 1, pageSize: linkPageSize });
          }}>
            <Form.Item name="username">
              <Input allowClear placeholder="请输入姓名或用户名" />
            </Form.Item>
            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  搜索
                </Button>
                <Button
                  icon={<ReloadOutlined />}
                  onClick={async () => {
                    linkUserForm.resetFields();
                    setLinkPageNo(1);
                    await loadSelectableUsers(selectedRole, { pageNo: 1, pageSize: linkPageSize }, '');
                  }}
                >
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>

          <Table<SysUserRecord>
            rowKey="id"
            loading={linkLoading}
            columns={linkUserColumns}
            dataSource={linkTableData}
            rowSelection={{
              onChange: (_, rows) => setSelectedLinkUsers(rows),
            }}
            pagination={{
              current: linkPageNo,
              pageSize: linkPageSize,
              total: linkTotal,
              showSizeChanger: true,
              onChange: async (nextPage, nextSize) => {
                setLinkPageNo(nextPage);
                setLinkPageSize(nextSize);
                await loadSelectableUsers(selectedRole, {
                  pageNo: nextPage,
                  pageSize: nextSize,
                });
              },
            }}
          />
        </div>
      </Modal>
    </PageContainer>
  );
};

export default RolePage;
