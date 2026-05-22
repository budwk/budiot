import {
  DeleteOutlined,
  EyeOutlined,
  PlusOutlined,
  UsergroupAddOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Drawer,
  Form,
  Input,
  Modal,
  Radio,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import { getPubUserPage, getPubUserUnitTree } from '@/services/budiot/pub/user';
import {
  createMsg,
  getMsgDetail,
  getMsgMeta,
  getMsgPage,
  getMsgViewUsers,
  revokeMsg,
} from '@/services/budiot/sys/msg';
import type {
  SysMsgRecord,
  SysMsgScopeOption,
  SysMsgTypeOption,
  SysUnitRecord,
  SysUserRecord,
} from '@/services/budiot/typing';
import { buildTree } from '@/utils/tree';

type MsgFormValues = {
  title: string;
  url?: string;
  type: string;
  scope: string;
  note: string;
};

type MsgTableParams = {
  current?: number;
  pageSize?: number;
  title?: string;
  type?: string;
};

const toTreeData = (items: SysUnitRecord[]): any[] =>
  items.map((item) => ({
    value: item.path,
    title: item.name,
    children: item.children ? toTreeData(item.children) : undefined,
  }));

const getSortParams = (
  sort: Record<string, 'ascend' | 'descend' | null>,
  defaultField: string,
) => {
  const current = Object.entries(sort || {}).find(([, value]) => value);
  return {
    pageOrderName: current?.[0] || defaultField,
    pageOrderBy: current?.[1] === 'ascend' ? 'ascending' : 'descending',
  };
};

const MsgPage = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [msgForm] = Form.useForm<MsgFormValues>();
  const [pickerForm] = Form.useForm<{ unitPath?: string; keyword?: string }>();
  const [submitting, setSubmitting] = React.useState(false);
  const [pickerLoading, setPickerLoading] = React.useState(false);
  const [viewUserLoading, setViewUserLoading] = React.useState(false);
  const [types, setTypes] = React.useState<SysMsgTypeOption[]>([]);
  const [scopes, setScopes] = React.useState<SysMsgScopeOption[]>([]);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [selectedUsers, setSelectedUsers] = React.useState<SysUserRecord[]>([]);
  const [pickerOpen, setPickerOpen] = React.useState(false);
  const [pickerUnits, setPickerUnits] = React.useState<SysUnitRecord[]>([]);
  const [pickerTableData, setPickerTableData] = React.useState<SysUserRecord[]>([]);
  const [pickerPageNo, setPickerPageNo] = React.useState(1);
  const [pickerPageSize, setPickerPageSize] = React.useState(10);
  const [pickerTotal, setPickerTotal] = React.useState(0);
  const [pickerSelectedRowKeys, setPickerSelectedRowKeys] = React.useState<React.Key[]>([]);
  const [pickerSelectedRows, setPickerSelectedRows] = React.useState<SysUserRecord[]>([]);
  const [detailRecord, setDetailRecord] = React.useState<SysMsgRecord>();
  const [viewUserDrawerOpen, setViewUserDrawerOpen] = React.useState(false);
  const [viewUserTitle, setViewUserTitle] = React.useState('全部用户');
  const [viewUserParams, setViewUserParams] = React.useState<{ id?: string; type?: string }>({});
  const [viewUserTableData, setViewUserTableData] = React.useState<SysUserRecord[]>([]);
  const [viewUserPageNo, setViewUserPageNo] = React.useState(1);
  const [viewUserPageSize, setViewUserPageSize] = React.useState(10);
  const [viewUserTotal, setViewUserTotal] = React.useState(0);

  const loadPickerUsers = React.useCallback(
    async (paging?: { pageNo?: number; pageSize?: number }) => {
      setPickerLoading(true);
      try {
        const response = await getPubUserPage({
          unitPath: pickerForm.getFieldValue('unitPath') || '',
          keyword: pickerForm.getFieldValue('keyword') || '',
          users: selectedUsers.map((item) => item.loginname).join(','),
          pageNo: paging?.pageNo ?? pickerPageNo,
          pageSize: paging?.pageSize ?? pickerPageSize,
          totalCount: 0,
          pageOrderName: 'updatedAt',
          pageOrderBy: 'descending',
        });
        setPickerTableData(response.data.list || []);
        setPickerTotal(response.data.totalCount || 0);
      } finally {
        setPickerLoading(false);
      }
    },
    [pickerForm, pickerPageNo, pickerPageSize, selectedUsers],
  );

  const loadViewUsers = React.useCallback(
    async (
      paging?: { pageNo?: number; pageSize?: number },
      params?: { id?: string; type?: string },
    ) => {
      const currentParams = params || viewUserParams;
      if (!currentParams.id || !currentParams.type) {
        setViewUserTableData([]);
        setViewUserTotal(0);
        return;
      }
      setViewUserLoading(true);
      try {
        const response = await getMsgViewUsers({
          id: currentParams.id,
          type: currentParams.type,
          pageNo: paging?.pageNo ?? viewUserPageNo,
          pageSize: paging?.pageSize ?? viewUserPageSize,
          totalCount: 0,
          pageOrderName: 'createdAt',
          pageOrderBy: 'descending',
        });
        setViewUserTableData(response.data.list || []);
        setViewUserTotal(response.data.totalCount || 0);
      } finally {
        setViewUserLoading(false);
      }
    },
    [viewUserPageNo, viewUserPageSize, viewUserParams],
  );

  React.useEffect(() => {
    const initialize = async () => {
      const [metaResponse, unitResponse] = await Promise.all([
        getMsgMeta(),
        getPubUserUnitTree(),
      ]);
      setTypes(metaResponse.data.types || []);
      setScopes(metaResponse.data.scopes || []);
      setPickerUnits(buildTree(unitResponse.data || []));
    };

    initialize().catch(() => undefined);
  }, []);

  const openCreate = () => {
    msgForm.resetFields();
    msgForm.setFieldsValue({
      title: '',
      url: '',
      type: types[0]?.value || 'USER',
      scope:
        scopes.find((item) => item.value === 'SCOPE')?.value ||
        scopes[0]?.value ||
        'SCOPE',
      note: '',
    });
    setSelectedUsers([]);
    setModalOpen(true);
  };

  const openPicker = async () => {
    setPickerSelectedRowKeys([]);
    setPickerSelectedRows([]);
    setPickerPageNo(1);
    setPickerOpen(true);
    await loadPickerUsers({ pageNo: 1, pageSize: pickerPageSize });
  };

  const submitMsg = async () => {
    const values = await msgForm.validateFields();
    if (values.scope === 'SCOPE' && !selectedUsers.length) {
      message.warning('请选择发送对象');
      return;
    }
    setSubmitting(true);
    try {
      await createMsg({
        ...values,
        users: selectedUsers.map((item) => item.id),
      });
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<SysMsgRecord>[]>(
    () => [
      {
        title: '标题',
        dataIndex: 'title',
        ellipsis: true,
      },
      {
        title: '消息类型',
        dataIndex: 'type',
        valueType: 'select',
        fieldProps: {
          options: [
            { label: '全部消息', value: '' },
            ...types.map((item) => ({ label: item.text, value: item.value })),
          ],
        },
        initialValue: '',
        width: 120,
        render: (_, record) => types.find((item) => item.value === record.type)?.text || record.type || '-',
      },
      {
        title: '全部用户',
        dataIndex: 'all_num',
        search: false,
        width: 100,
        align: 'center',
        render: (_, record) => (
          <Button
            type="link"
            onClick={async () => {
              setViewUserTitle('全部用户');
              const nextParams = { id: record.id, type: 'all' };
              setViewUserParams(nextParams);
              setViewUserDrawerOpen(true);
              setViewUserPageNo(1);
              await loadViewUsers({ pageNo: 1, pageSize: viewUserPageSize }, nextParams);
            }}
          >
            {record.all_num || 0}
          </Button>
        ),
      },
      {
        title: '未读用户',
        dataIndex: 'unread_num',
        search: false,
        width: 100,
        align: 'center',
        render: (_, record) => (
          <Button
            type="link"
            onClick={async () => {
              setViewUserTitle('未读用户');
              const nextParams = { id: record.id, type: 'unread' };
              setViewUserParams(nextParams);
              setViewUserDrawerOpen(true);
              setViewUserPageNo(1);
              await loadViewUsers({ pageNo: 1, pageSize: viewUserPageSize }, nextParams);
            }}
          >
            {record.unread_num || 0}
          </Button>
        ),
      },
      {
        title: '发送时间',
        dataIndex: 'sendAt',
        search: false,
        width: 180,
        sorter: true,
        render: (_, record) =>
          record.sendAt ? dayjs(record.sendAt).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        title: '消息状态',
        dataIndex: 'delFlag',
        search: false,
        width: 120,
        render: (_, record) =>
          record.delFlag ? <Tag color="error">已撤销</Tag> : <Tag color="success">已发送</Tag>,
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
                key: 'detail',
                label: '详情',
                icon: <EyeOutlined />,
                onClick: async () => {
                  const response = await getMsgDetail(record.id);
                  setDetailRecord(response.data);
                },
              },
              {
                key: 'revoke',
                label: '撤销',
                icon: <DeleteOutlined />,
                danger: true,
                disabled:
                  record.delFlag || !access.hasPermission('sys.manage.msg.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认撤销消息',
                    content: `确定撤销 ${record.title} 吗？`,
                    onOk: async () => {
                      await revokeMsg(record.id);
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
    [access, loadViewUsers, modal, types, viewUserPageSize],
  );

  return (
    <PageContainer title="站内消息">
      <PlatformProTable<SysMsgRecord, MsgTableParams>
        persistenceKey="platform-sys-msg-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="消息列表"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.msg.create')}
            onClick={openCreate}
          >
            发送消息
          </Button>,
        ]}
        request={async (params, sort) => {
          const sortParams = getSortParams(sort, 'sendAt');
          const response = await getMsgPage({
            title: params.title || '',
            type: params.type || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
            ...sortParams,
          });
          return {
            data: response.data.list || [],
            total: response.data.totalCount || 0,
            success: true,
          };
        }}
      />

      <Modal
        title="发送消息"
        open={modalOpen}
        forceRender
        width={760}
        confirmLoading={submitting}
        onOk={submitMsg}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form<MsgFormValues> form={msgForm} layout="vertical">
          <Form.Item
            name="title"
            label="消息标题"
            rules={[{ required: true, message: '请输入消息标题' }]}
          >
            <Input placeholder="请输入消息标题" />
          </Form.Item>
          <Form.Item name="url" label="URL路径">
            <Input placeholder="跳转路径" />
          </Form.Item>
          <Form.Item
            name="type"
            label="消息类型"
            rules={[{ required: true, message: '请选择消息类型' }]}
          >
            <Radio.Group
              options={types.map((item) => ({
                label: item.text,
                value: item.value,
              }))}
            />
          </Form.Item>
          <Form.Item
            name="scope"
            label="发送范围"
            rules={[{ required: true, message: '请选择发送范围' }]}
          >
            <Radio.Group
              options={scopes.map((item) => ({
                label: item.text,
                value: item.value,
              }))}
            />
          </Form.Item>
          <Form.Item noStyle shouldUpdate>
            {({ getFieldValue }) =>
              getFieldValue('scope') === 'SCOPE' ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 16 }}>
                  <Space>
                    <Button icon={<UsergroupAddOutlined />} onClick={openPicker}>
                      选择用户
                    </Button>
                    <Button danger onClick={() => setSelectedUsers([])}>
                      清除选择
                    </Button>
                  </Space>
                  <Table<SysUserRecord>
                    rowKey="id"
                    size="small"
                    pagination={false}
                    dataSource={selectedUsers}
                    columns={[
                      {
                        title: '用户名',
                        key: 'loginname',
                        render: (_, record) => `${record.loginname} (${record.username})`,
                      },
                      { title: '手机号', dataIndex: 'mobile', key: 'mobile' },
                      {
                        title: '所属单位',
                        key: 'unit',
                        render: (_, record) => record.unit?.name || '-',
                      },
                    ]}
                  />
                </div>
              ) : (
                <Tag color="processing" style={{ marginBottom: 16 }}>
                  全部用户不含已被禁用用户
                </Tag>
              )
            }
          </Form.Item>
          <Form.Item
            name="note"
            label="消息内容"
            rules={[{ required: true, message: '请输入消息内容' }]}
          >
            <Input.TextArea rows={5} maxLength={500} placeholder="请输入消息内容" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="选择用户"
        open={pickerOpen}
        forceRender
        width={920}
        onOk={() => {
          const merged = new Map<string, SysUserRecord>();
          [...selectedUsers, ...pickerSelectedRows].forEach((item) => {
            merged.set(item.id, item);
          });
          setSelectedUsers(Array.from(merged.values()));
          setPickerSelectedRowKeys([]);
          setPickerSelectedRows([]);
          setPickerOpen(false);
        }}
        onCancel={() => {
          setPickerSelectedRowKeys([]);
          setPickerSelectedRows([]);
          setPickerOpen(false);
        }}
        destroyOnHidden
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Form form={pickerForm} layout="inline">
            <Form.Item name="unitPath">
              <TreeSelect
                allowClear
                treeDefaultExpandAll
                placeholder="选择单位"
                treeData={toTreeData(pickerUnits)}
                style={{ minWidth: 220 }}
                onChange={async () => {
                  setPickerPageNo(1);
                  await loadPickerUsers({ pageNo: 1, pageSize: pickerPageSize });
                }}
              />
            </Form.Item>
            <Form.Item name="keyword">
              <Input
                allowClear
                placeholder="请输入用户名或姓名"
                onPressEnter={async () => {
                  setPickerPageNo(1);
                  await loadPickerUsers({ pageNo: 1, pageSize: pickerPageSize });
                }}
              />
            </Form.Item>
            <Button
              type="primary"
              onClick={async () => {
                setPickerPageNo(1);
                await loadPickerUsers({ pageNo: 1, pageSize: pickerPageSize });
              }}
            >
              搜索
            </Button>
          </Form>

          <Table<SysUserRecord>
            rowKey="id"
            loading={pickerLoading}
            dataSource={pickerTableData}
            rowSelection={{
              selectedRowKeys: pickerSelectedRowKeys,
              onChange: (keys, rows) => {
                setPickerSelectedRowKeys(keys);
                setPickerSelectedRows(rows);
              },
            }}
            columns={[
              {
                title: '姓名/用户名',
                key: 'username',
                render: (_, record) => `${record.username} (${record.loginname})`,
              },
              {
                title: '所属单位',
                key: 'unit',
                render: (_, record) => record.unit?.name || '-',
              },
              {
                title: '手机号',
                dataIndex: 'mobile',
                key: 'mobile',
              },
            ]}
            pagination={{
              current: pickerPageNo,
              pageSize: pickerPageSize,
              total: pickerTotal,
              showSizeChanger: true,
              onChange: async (nextPage, nextPageSize) => {
                setPickerPageNo(nextPage);
                setPickerPageSize(nextPageSize);
                await loadPickerUsers({ pageNo: nextPage, pageSize: nextPageSize });
              },
            }}
          />
        </div>
      </Modal>

      <Drawer
        title="消息详情"
        open={Boolean(detailRecord)}
        size="large"
        onClose={() => setDetailRecord(undefined)}
      >
        {detailRecord ? (
          <Space orientation="vertical" size={12} style={{ width: '100%' }}>
            <Typography.Text>
              发送人：
              {detailRecord.createdByUser
                ? `${detailRecord.createdByUser.username}(${detailRecord.createdByUser.loginname})`
                : '-'}
            </Typography.Text>
            <Typography.Text>
              发送时间：
              {detailRecord.sendAt ? dayjs(detailRecord.sendAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
            </Typography.Text>
            <Typography.Title level={5} style={{ margin: 0 }}>
              {detailRecord.title}
            </Typography.Title>
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
              {detailRecord.note || '-'}
            </Typography.Paragraph>
          </Space>
        ) : null}
      </Drawer>

      <Drawer
        title={viewUserTitle}
        open={viewUserDrawerOpen}
        size="large"
        onClose={() => setViewUserDrawerOpen(false)}
      >
        <Tag color="warning" style={{ marginBottom: 12 }}>
          若用户数量与列表显示不一致，可能是用户已被删除造成的
        </Tag>
        <Table<SysUserRecord>
          rowKey="id"
          loading={viewUserLoading}
          dataSource={viewUserTableData}
          columns={[
            { title: '用户名', dataIndex: 'loginname', key: 'loginname', width: 160 },
            { title: '姓名', dataIndex: 'username', key: 'username', width: 120 },
            { title: '手机号', dataIndex: 'mobile', key: 'mobile', width: 140 },
            { title: '邮箱', dataIndex: 'email', key: 'email' },
            {
              title: '所属单位',
              key: 'unit',
              render: (_, record) => record.unit?.name || '-',
            },
          ]}
          pagination={{
            current: viewUserPageNo,
            pageSize: viewUserPageSize,
            total: viewUserTotal,
            showSizeChanger: true,
            onChange: async (nextPage, nextPageSize) => {
              setViewUserPageNo(nextPage);
              setViewUserPageSize(nextPageSize);
              await loadViewUsers({ pageNo: nextPage, pageSize: nextPageSize });
            },
          }}
        />
      </Drawer>
    </PageContainer>
  );
};

export default MsgPage;
