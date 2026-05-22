import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  ImportOutlined,
  KeyOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { UploadFile } from 'antd';
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Spin,
  Switch,
  Tag,
  Tree,
  TreeSelect,
  Typography,
  Upload,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createUser,
  deleteUser,
  downloadUserImportTemplate,
  exportUsers,
  getUserDetail,
  getUserList,
  getUserPosts,
  getUserRoleGroups,
  getUserSerialNo,
  getUserUnitTree,
  importUsers,
  resetUserPassword,
  updateUser,
  updateUserDisabled,
} from '@/services/budiot/sys/user';
import type {
  SysPostRecord,
  SysRoleGroupRecord,
  SysUnitRecord,
  SysUserDetail,
  SysUserRecord,
} from '@/services/budiot/typing';
import { buildTree } from '@/utils/tree';

interface SearchValues {
  username?: string;
  loginname?: string;
  mobile?: string;
  disabled?: boolean;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs];
}

type UserFormValues = SysUserDetail;

type UserTableParams = {
  current?: number;
  pageSize?: number;
  username?: string;
  loginname?: string;
  mobile?: string;
  disabled?: boolean;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs];
  unitPath?: string;
};

const maskMobile = (value?: string) => {
  if (!value) return '-';
  if (!/^1\d{10}$/.test(value)) return value;
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
};

const toTreeData = (items: SysUnitRecord[]): any[] =>
  items.map((item) => ({
    key: item.id,
    value: item.id,
    title: item.name,
    path: item.path,
    children: item.children ? toTreeData(item.children) : undefined,
  }));

const buildSearchPayload = (
  values: SearchValues,
  unitPath: string,
  paging: { pageNo: number; pageSize: number },
) => ({
  unitPath,
  username: values.username,
  loginname: values.loginname,
  mobile: values.mobile,
  disabled: values.disabled,
  beginTime: values.dateRange?.[0]?.valueOf(),
  endTime: values.dateRange?.[1]?.valueOf(),
  pageNo: paging.pageNo,
  pageSize: paging.pageSize,
  pageOrderName: 'updatedAt',
  pageOrderBy: 'descending',
});

const defaultUserFormValues = (): UserFormValues => ({
  unitId: '',
  unitPath: '',
  postId: '',
  roleIds: [],
  username: '',
  loginname: '',
  password: '',
  email: '',
  mobile: '',
  serialNo: '',
  sex: 0,
  disabled: false,
});

const UserPage = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const latestExportParamsRef = React.useRef<Record<string, unknown>>({});
  const [userForm] = Form.useForm<UserFormValues>();
  const [treeLoading, setTreeLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [importing, setImporting] = React.useState(false);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);
  const [unitTree, setUnitTree] = React.useState<SysUnitRecord[]>([]);
  const [posts, setPosts] = React.useState<SysPostRecord[]>([]);
  const [groups, setGroups] = React.useState<SysRoleGroupRecord[]>([]);
  const [currentUnitId, setCurrentUnitId] = React.useState<string>('');
  const [currentUnitPath, setCurrentUnitPath] = React.useState<string>('');
  const [currentUnitName, setCurrentUnitName] = React.useState<string>('');
  const [selectedKeys, setSelectedKeys] = React.useState<React.Key[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [showSerialNoEditor, setShowSerialNoEditor] = React.useState(false);
  const [importModalOpen, setImportModalOpen] = React.useState(false);
  const [importFileList, setImportFileList] = React.useState<UploadFile[]>([]);
  const [importPassword, setImportPassword] = React.useState('');
  const [importUpdateSupport, setImportUpdateSupport] = React.useState(false);

  React.useEffect(() => {
    const initialize = async () => {
      setTreeLoading(true);
      try {
        const [unitResponse, postResponse] = await Promise.all([
          getUserUnitTree(),
          getUserPosts(),
        ]);
        setUnitTree(buildTree(unitResponse.data || []));
        setPosts(postResponse.data || []);
      } finally {
        setTreeLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const loadRoleGroups = React.useCallback(async (unitId?: string) => {
    if (!unitId) {
      setGroups([]);
      return;
    }
    const response = await getUserRoleGroups(unitId);
    setGroups(response.data || []);
  }, []);

  const handleUnitSelect = async (_keys: React.Key[], info: any) => {
    const nextPath = info.node?.path || '';
    const nextId = info.node?.key || '';
    const nextName = info.node?.title || '';
    setSelectedKeys(info.selected ? [info.node.key] : []);
    setCurrentUnitId(nextId);
    setCurrentUnitPath(nextPath);
    setCurrentUnitName(String(nextName || ''));
  };

  const handleDisabledChange = async (record: SysUserRecord, checked: boolean) => {
    setSwitchingIds((prev) => [...prev, record.id]);
    try {
      await updateUserDisabled({
        id: record.id,
        loginname: record.loginname,
        disabled: !checked,
      });
      actionRef.current?.reload();
    } finally {
      setSwitchingIds((prev) => prev.filter((item) => item !== record.id));
    }
  };

  const findPostName = React.useCallback(
    (postId?: string) => posts.find((item) => item.id === postId)?.name || '-',
    [posts],
  );

  const handleFormUnitChange = async (unitId?: string) => {
    const unitMap = new Map<string, SysUnitRecord>();
    const visit = (items: SysUnitRecord[]) => {
      items.forEach((item) => {
        unitMap.set(item.id, item);
        if (item.children?.length) {
          visit(item.children);
        }
      });
    };
    visit(unitTree);
    userForm.setFieldValue('unitPath', unitId ? unitMap.get(unitId)?.path || '' : '');
    userForm.setFieldValue('roleIds', []);
    await loadRoleGroups(unitId);
  };

  const openCreate = async () => {
    userForm.resetFields();
    setEditingId(undefined);
    setShowSerialNoEditor(false);
    setGroups([]);
    const [serialResponse] = await Promise.all([
      getUserSerialNo(),
      currentUnitId ? loadRoleGroups(currentUnitId) : Promise.resolve(),
    ]);
    userForm.setFieldsValue({
      ...defaultUserFormValues(),
      unitId: currentUnitId,
      unitPath: currentUnitPath,
      serialNo: serialResponse.data || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysUserRecord) => {
    setEditingId(record.id);
    setShowSerialNoEditor(false);
    const response = await getUserDetail(record.id);
    const detail = response.data.user;
    await loadRoleGroups(detail.unitId);
    userForm.setFieldsValue({
      ...defaultUserFormValues(),
      ...detail,
      roleIds: response.data.roleIds || [],
      password: '',
    });
    setModalOpen(true);
  };

  const submitUser = async () => {
    const values = await userForm.validateFields();
    setSubmitting(true);
    try {
      const payload: Record<string, unknown> = { ...values };
      if (editingId) {
        delete payload.password;
        await updateUser(payload);
      } else {
        await createUser(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const handleExport = async () => {
    await exportUsers(
      {
        ...latestExportParamsRef.current,
        pageNo: 1,
        pageSize: actionRef.current?.pageInfo?.pageSize || 10,
      },
      `user_${Date.now()}.xlsx`,
    );
  };

  const handleImport = () => {
    setImportPassword('');
    setImportUpdateSupport(false);
    setImportFileList([]);
    setImportModalOpen(true);
  };

  const submitImport = async () => {
    const file = importFileList[0]?.originFileObj;
    if (!file) {
      message.warning('请先选择导入文件');
      return;
    }
    const formData = new FormData();
    formData.append('Filedata', file as File);
    setImporting(true);
    try {
      const response = await importUsers(formData, {
        updateSupport: importUpdateSupport,
        pwd: importPassword,
      });
      if (response.code !== 200) {
        message.error(response.msg || '导入失败');
        return;
      }
      setImportModalOpen(false);
      modal.info({
        title: '导入结果',
        width: 680,
        content: (
          <Typography.Paragraph
            style={{ maxHeight: 420, overflow: 'auto', whiteSpace: 'pre-wrap' }}
          >
            {String(response.msg || '导入完成')
              .replace(/<br\s*\/?>/gi, '\n')
              .replace(/<[^>]+>/g, '')}
          </Typography.Paragraph>
        ),
      });
      actionRef.current?.reload();
    } finally {
      setImporting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysUserRecord>[]>(
    () => [
      {
        title: '用户姓名',
        dataIndex: 'username',
        width: 140,
      },
      {
        title: '用户名',
        dataIndex: 'loginname',
        width: 160,
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
        hideInTable: true,
      },
      {
        title: '用户状态',
        dataIndex: 'disabled',
        hideInTable: true,
        valueType: 'select',
        fieldProps: {
          allowClear: true,
          options: [
            { label: '启用', value: false },
            { label: '禁用', value: true },
          ],
        },
      },
      {
        title: '用户编号',
        dataIndex: 'serialNo',
        search: false,
        width: 120,
      },
      {
        title: '所属单位',
        dataIndex: ['unit', 'name'],
        search: false,
        width: 180,
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '职务',
        dataIndex: 'postId',
        search: false,
        width: 140,
        render: (_, record) => findPostName(record.postId),
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
        search: false,
        width: 140,
        render: (_, record) => maskMobile(record.mobile),
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        search: false,
        width: 100,
        render: (_, record) => (
          <Switch
            checked={!record.disabled}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            disabled={!access.hasPermission('sys.manage.user.update')}
            loading={switchingIds.includes(record.id)}
            onChange={(checked) => handleDisabledChange(record, checked)}
          />
        ),
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        search: false,
        width: 180,
        render: (_, record) =>
          record.createdAt ? dayjs(record.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 200,
        fixed: 'right',
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('sys.manage.user.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'reset',
                label: '重置密码',
                icon: <KeyOutlined />,
                disabled: !access.hasPermission('sys.manage.user.update'),
                onClick: () => {
                  modal.confirm({
                    title: '确认重置密码',
                    content: `确定重置 ${record.username}(${record.loginname}) 的密码吗？`,
                    onOk: async () => {
                      const response = await resetUserPassword(record.id);
                      modal.success({
                        title: '密码已重置',
                        content: `新密码：${response.data}`,
                      });
                    },
                  });
                },
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('sys.manage.user.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除用户',
                    content: `确定删除 ${record.username} 吗？`,
                    onOk: async () => {
                      await deleteUser(record.id, record.loginname);
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
    [access, findPostName, modal, switchingIds],
  );

  return (
    <PageContainer title="用户管理">
      <ProCard split="vertical">
        <ProCard title="单位树" colSpan="22%">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="请输入单位名称"
            style={{ marginBottom: 16 }}
            onChange={async (event) => {
              const response = await getUserUnitTree({
                name: event.target.value || undefined,
              });
              setUnitTree(buildTree(response.data || []));
            }}
          />
          <Spin spinning={treeLoading}>
            <Tree
              blockNode
              showLine
              selectedKeys={selectedKeys}
              treeData={toTreeData(unitTree)}
              onSelect={handleUnitSelect}
            />
          </Spin>
        </ProCard>
        <ProCard title="用户列表" colSpan="78%">
          <PlatformProTable<SysUserRecord, UserTableParams>
            persistenceKey="platform-sys-user-table"
            actionRef={actionRef}
            rowKey="id"
            headerTitle="用户列表"
            params={{ unitPath: currentUnitPath }}
            scroll={{ x: 1480 }}
            columns={columns}
            toolBarRender={() => [
              <Button
                key="create"
                type="primary"
                icon={<PlusOutlined />}
                disabled={!access.hasPermission('sys.manage.user.create')}
                onClick={openCreate}
              >
                新增
              </Button>,
              <Button
                key="import"
                icon={<ImportOutlined />}
                disabled={!access.hasPermission('sys.manage.user.import')}
                onClick={handleImport}
              >
                导入
              </Button>,
              <Button
                key="export"
                icon={<DownloadOutlined />}
                disabled={!access.hasPermission('sys.manage.user.export')}
                onClick={handleExport}
              >
                导出
              </Button>,
              <Tag key="unit" color="processing">
                当前单位：{currentUnitName || '全部'}
              </Tag>,
            ]}
            request={async (params) => {
              const payload = buildSearchPayload(
                {
                  username: params.username,
                  loginname: params.loginname,
                  mobile: params.mobile,
                  disabled: params.disabled,
                  dateRange: params.dateRange,
                },
                currentUnitPath,
                {
                  pageNo: params.current || 1,
                  pageSize: params.pageSize || 10,
                },
              );
              latestExportParamsRef.current = payload;
              const response = await getUserList(payload);
              return {
                data: response.data.list || [],
                total: response.data.totalCount || 0,
                success: true,
              };
            }}
          />
        </ProCard>
      </ProCard>

      <Modal
        title={editingId ? '修改用户' : '新增用户'}
        open={modalOpen}
        forceRender
        width={760}
        confirmLoading={submitting}
        onOk={submitUser}
        onCancel={() => setModalOpen(false)}
      >
        <Form<UserFormValues> form={userForm} layout="vertical">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
            <Form.Item
              name="username"
              label="用户姓名"
              rules={[{ required: true, message: '请输入用户姓名' }]}
            >
              <Input placeholder="请输入用户姓名" />
            </Form.Item>
            <Form.Item
              name="unitId"
              label="所属单位"
              rules={[{ required: true, message: '请选择所属单位' }]}
            >
              <TreeSelect
                treeDefaultExpandAll
                placeholder="选择所属单位"
                treeData={toTreeData(unitTree)}
                onChange={handleFormUnitChange}
              />
            </Form.Item>
            <Form.Item name="serialNo" label="用户编号">
              {showSerialNoEditor ? (
                <Input placeholder="员工编号" />
              ) : (
                <Space>
                  <Typography.Text>{userForm.getFieldValue('serialNo') || '-'}</Typography.Text>
                  <Button type="link" onClick={() => setShowSerialNoEditor(true)}>
                    修改
                  </Button>
                </Space>
              )}
            </Form.Item>
            <Form.Item name="postId" label="单位职务">
              <Select
                allowClear
                placeholder="选择单位职务"
                options={posts.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
              />
            </Form.Item>
            <Form.Item
              name="loginname"
              label="登录用户名"
              rules={[{ required: true, message: '请输入登录用户名' }]}
            >
              <Input placeholder="请输入登录用户名" />
            </Form.Item>
            {!editingId ? (
              <Form.Item
                name="password"
                label="登录密码"
                rules={[{ required: true, message: '请输入登录密码' }]}
              >
                <Input.Password placeholder="请输入登录密码" />
              </Form.Item>
            ) : null}
            <Form.Item name="mobile" label="手机号码">
              <Input placeholder="请输入手机号码" />
            </Form.Item>
            <Form.Item name="email" label="电子信箱">
              <Input placeholder="请输入电子信箱" />
            </Form.Item>
            <Form.Item name="disabled" label="用户状态">
              <Radio.Group
                options={[
                  { label: '启用', value: false },
                  { label: '禁用', value: true },
                ]}
              />
            </Form.Item>
            <Form.Item name="sex" label="用户性别">
              <Radio.Group
                options={[
                  { label: '男', value: 1 },
                  { label: '女', value: 2 },
                  { label: '未知', value: 0 },
                ]}
              />
            </Form.Item>
            <Form.Item name="roleIds" label="用户角色" style={{ gridColumn: '1 / span 2' }}>
              <Select
                mode="multiple"
                placeholder="分配角色"
                options={groups.flatMap((group) =>
                  (group.roles || []).map((role) => ({
                    label: `${group.name} / ${role.name}`,
                    value: role.id,
                  })),
                )}
              />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title="导入用户"
        open={importModalOpen}
        confirmLoading={importing}
        onOk={submitImport}
        onCancel={() => setImportModalOpen(false)}
        destroyOnHidden
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Upload.Dragger
            accept=".xlsx,.xls"
            maxCount={1}
            beforeUpload={() => false}
            fileList={importFileList}
            onChange={({ fileList }) => setImportFileList(fileList)}
          >
            <p>将文件拖到此处，或点击上传</p>
            <p style={{ color: '#999' }}>仅允许导入 xls、xlsx 格式文件</p>
          </Upload.Dragger>
          <Input.Password
            allowClear
            value={importPassword}
            onChange={(event) => setImportPassword(event.target.value)}
            placeholder="新用户默认登录密码（不填则随机生成）"
          />
          <label>
            <input
              type="checkbox"
              checked={importUpdateSupport}
              onChange={(event) => setImportUpdateSupport(event.target.checked)}
              style={{ marginRight: 8 }}
            />
            是否更新已经存在的用户数据（不更新密码）
          </label>
          <Button
            type="link"
            onClick={() => downloadUserImportTemplate(`user_template_${Date.now()}.xlsx`)}
          >
            下载模板
          </Button>
        </div>
      </Modal>
    </PageContainer>
  );
};

export default UserPage;
