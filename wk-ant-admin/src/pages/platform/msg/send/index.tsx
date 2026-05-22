import {
  PlusOutlined,
  ReloadOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Card,
  Empty,
  Form,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import * as React from 'react';
import {
  createMsgSend,
  getMsgSendMeta,
} from '@/services/budiot/msg/send';
import {
  getPubUserPage,
  getPubUserUnitTree,
} from '@/services/budiot/pub/user';
import type {
  MsgOption,
  MsgSendResultRecord,
  MsgTemplateRecord,
  SysUnitRecord,
  SysUserRecord,
} from '@/services/budiot/typing';

type MsgSendFormValues = {
  channelId: string;
  templateId?: string;
  bizType?: string;
  title?: string;
  content?: string;
  paramsJson?: string;
};

type UserPickerParams = {
  current?: number;
  pageSize?: number;
  unitId?: string;
  keywords?: string;
};

const defaultFormValues: MsgSendFormValues = {
  channelId: '',
  templateId: undefined,
  bizType: undefined,
  title: '',
  content: '',
  paramsJson: '',
};

const toTreeData = (items: SysUnitRecord[]): { value: string; title: string; children?: any[] }[] =>
  items.map((item) => ({
    value: item.id,
    title: item.name,
    children: item.children ? toTreeData(item.children) : undefined,
  }));

const MsgSendPage: React.FC = () => {
  const access = useAccess();
  const { message } = App.useApp();
  const [form] = Form.useForm<MsgSendFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [channels, setChannels] = React.useState<
    Array<{ id: string; name: string; channelType?: string; providerType?: string }>
  >([]);
  const [templates, setTemplates] = React.useState<MsgTemplateRecord[]>([]);
  const [bizTypes, setBizTypes] = React.useState<MsgOption[]>([]);
  const [sendMode, setSendMode] = React.useState<'USER' | 'RECEIVER'>('USER');
  const [selectedUsers, setSelectedUsers] = React.useState<SysUserRecord[]>([]);
  const [receiverText, setReceiverText] = React.useState('');
  const [pickerOpen, setPickerOpen] = React.useState(false);
  const [unitTree, setUnitTree] = React.useState<SysUnitRecord[]>([]);
  const [pickerParams, setPickerParams] = React.useState<UserPickerParams>({
    current: 1,
    pageSize: 10,
  });
  const [pickerLoading, setPickerLoading] = React.useState(false);
  const [pickerTotal, setPickerTotal] = React.useState(0);
  const [pickerRows, setPickerRows] = React.useState<SysUserRecord[]>([]);
  const [pickerSelectedRowKeys, setPickerSelectedRowKeys] = React.useState<React.Key[]>([]);
  const [pickerSelectedRows, setPickerSelectedRows] = React.useState<SysUserRecord[]>([]);
  const [resultOpen, setResultOpen] = React.useState(false);
  const [sendResults, setSendResults] = React.useState<MsgSendResultRecord[]>([]);

  const currentChannelId = Form.useWatch('channelId', form);

  const channelMap = React.useMemo(
    () => new Map(channels.map((item) => [item.id, item])),
    [channels],
  );

  const filteredTemplates = React.useMemo(() => {
    if (!currentChannelId) {
      return templates;
    }
    return templates.filter((item) => item.channelId === currentChannelId);
  }, [currentChannelId, templates]);

  const selectedTemplateMap = React.useMemo(
    () => new Map(templates.map((item) => [item.id, item])),
    [templates],
  );

  const initialize = React.useCallback(async () => {
    setLoading(true);
    try {
      const [metaResponse, unitResponse] = await Promise.all([
        getMsgSendMeta(),
        getPubUserUnitTree(),
      ]);
      setChannels(metaResponse.data.channels || []);
      setTemplates(metaResponse.data.templates || []);
      setBizTypes(metaResponse.data.bizTypes || []);
      setUnitTree(unitResponse.data || []);
      form.setFieldsValue({
        ...defaultFormValues,
        bizType: metaResponse.data.bizTypes?.[0]?.value,
      });
    } finally {
      setLoading(false);
    }
  }, [form]);

  React.useEffect(() => {
    initialize().catch(() => undefined);
  }, [initialize]);

  const loadUsers = React.useCallback(async (nextParams: UserPickerParams) => {
    setPickerLoading(true);
    try {
      const response = await getPubUserPage({
        unitId: nextParams.unitId || '',
        keywords: nextParams.keywords || '',
        pageNo: nextParams.current || 1,
        pageSize: nextParams.pageSize || 10,
      });
      setPickerRows(response.data.list || []);
      setPickerTotal(response.data.totalCount || 0);
    } finally {
      setPickerLoading(false);
    }
  }, []);

  React.useEffect(() => {
    if (!pickerOpen) {
      return;
    }
    loadUsers(pickerParams).catch(() => undefined);
  }, [loadUsers, pickerOpen, pickerParams]);

  const handleChannelChange = (channelId?: string) => {
    const firstTemplate = templates.find((item) => item.channelId === channelId);
    form.setFieldsValue({
      templateId: undefined,
      bizType: firstTemplate ? undefined : form.getFieldValue('bizType'),
    });
  };

  const handleTemplateChange = (templateId?: string) => {
    const template = selectedTemplateMap.get(templateId || '');
    if (!template) {
      return;
    }
    form.setFieldsValue({
      bizType:
        typeof template.bizType === 'string' ? template.bizType : template.bizType?.value,
      title: template.title || '',
      content: template.content || '',
      paramsJson: template.paramsJson || '',
      channelId: template.channelId,
    });
  };

  const openPicker = () => {
    setPickerSelectedRowKeys(selectedUsers.map((item) => item.id));
    setPickerSelectedRows(selectedUsers);
    setPickerOpen(true);
  };

  const confirmPicker = () => {
    const merged = new Map<string, SysUserRecord>();
    [...selectedUsers, ...pickerSelectedRows].forEach((item) => {
      merged.set(item.id, item);
    });
    setSelectedUsers(Array.from(merged.values()));
    setPickerSelectedRowKeys([]);
    setPickerSelectedRows([]);
    setPickerOpen(false);
  };

  const removeUser = (userId: string) => {
    setSelectedUsers((prev) => prev.filter((item) => item.id !== userId));
    setPickerSelectedRowKeys((prev) => prev.filter((item) => item !== userId));
  };

  const resetForm = () => {
    form.resetFields();
    form.setFieldsValue({
      ...defaultFormValues,
      bizType: bizTypes[0]?.value,
    });
    setSendMode('USER');
    setSelectedUsers([]);
    setReceiverText('');
    setSendResults([]);
  };

  const submit = async () => {
    const values = await form.validateFields();
    const receiverList = receiverText
      .split(/[\n,，;；]/)
      .map((item) => item.trim())
      .filter(Boolean);

    if (sendMode === 'USER' && selectedUsers.length === 0) {
      message.warning('请至少选择一个接收用户');
      return;
    }
    if (sendMode === 'RECEIVER' && receiverList.length === 0) {
      message.warning('请至少输入一个接收地址');
      return;
    }

    const channel = channelMap.get(values.channelId);
    setSubmitting(true);
    try {
      const response = await createMsgSend({
        ...values,
        channelType: channel?.channelType || '',
        providerType: channel?.providerType || '',
        userIds: sendMode === 'USER' ? selectedUsers.map((item) => item.id) : [],
        receivers: sendMode === 'RECEIVER' ? receiverList : [],
      });
      setSendResults(response.data || []);
      setResultOpen(true);
    } finally {
      setSubmitting(false);
    }
  };

  const userColumns = React.useMemo<ColumnsType<SysUserRecord>>(
    () => [
      {
        title: '姓名',
        dataIndex: 'username',
      },
      {
        title: '登录名',
        dataIndex: 'loginname',
      },
      {
        title: '所属单位',
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
      },
      {
        title: '操作',
        width: 100,
        render: (_, record) => (
          <Button type="link" danger onClick={() => removeUser(record.id)}>
            移除
          </Button>
        ),
      },
    ],
    [],
  );

  const pickerColumns = React.useMemo<ColumnsType<SysUserRecord>>(
    () => [
      {
        title: '姓名',
        dataIndex: 'username',
      },
      {
        title: '登录名',
        dataIndex: 'loginname',
      },
      {
        title: '所属单位',
        render: (_, record) => record.unit?.name || '-',
      },
      {
        title: '手机号',
        dataIndex: 'mobile',
      },
      {
        title: '状态',
        render: (_, record) =>
          record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
      },
    ],
    [],
  );

  const resultColumns = React.useMemo<ColumnsType<MsgSendResultRecord>>(
    () => [
      {
        title: '接收对象',
        dataIndex: 'receiver',
      },
      {
        title: '发送结果',
        dataIndex: 'success',
        render: (_, record) =>
          record.success ? <Tag color="success">成功</Tag> : <Tag color="error">失败</Tag>,
        width: 100,
      },
      {
        title: '请求号',
        dataIndex: 'requestId',
      },
      {
        title: '返回码',
        dataIndex: 'code',
        width: 120,
      },
      {
        title: '返回信息',
        dataIndex: 'message',
      },
      {
        title: '发送时间',
        dataIndex: 'sendAt',
        render: (_, record) =>
          record.sendAt ? dayjs(record.sendAt).format('YYYY-MM-DD HH:mm:ss') : '-',
        width: 180,
      },
    ],
    [],
  );

  return (
    <PageContainer title="发送消息" loading={loading}>
      <Card>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            ...defaultFormValues,
            bizType: bizTypes[0]?.value,
          }}
        >
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
            <Form.Item
              label="消息渠道"
              name="channelId"
              rules={[{ required: true, message: '请选择消息渠道' }]}
            >
              <Select
                placeholder="请选择消息渠道"
                options={channels.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
                allowClear
                onChange={handleChannelChange}
              />
            </Form.Item>
            <Form.Item label="消息模板" name="templateId">
              <Select
                placeholder="可选，选择后自动带入内容"
                options={filteredTemplates.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
                allowClear
                onChange={handleTemplateChange}
              />
            </Form.Item>
            <Form.Item label="业务类型" name="bizType">
              <Select
                placeholder="请选择业务类型"
                options={bizTypes.map((item) => ({
                  label: item.text,
                  value: item.value,
                }))}
                allowClear
              />
            </Form.Item>
            <Form.Item label="接收方式">
              <Radio.Group
                value={sendMode}
                options={[
                  { label: '系统用户', value: 'USER' },
                  { label: '直接地址', value: 'RECEIVER' },
                ]}
                onChange={(event) => setSendMode(event.target.value)}
              />
            </Form.Item>
          </div>

          <Form.Item label="消息标题" name="title">
            <Input placeholder="请输入消息标题" />
          </Form.Item>
          <Form.Item
            label="消息内容"
            name="content"
            rules={[{ required: true, message: '请输入消息内容' }]}
          >
            <Input.TextArea rows={8} placeholder="请输入消息内容" />
          </Form.Item>
          <Form.Item label="模板参数 JSON" name="paramsJson">
            <Input.TextArea rows={4} placeholder='{"code":"1234"}' />
          </Form.Item>

          {sendMode === 'USER' ? (
            <Form.Item label="接收用户">
              <Space style={{ marginBottom: 12 }}>
                <Button icon={<PlusOutlined />} onClick={openPicker}>
                  选择用户
                </Button>
                <Typography.Text type="secondary">
                  已选择 {selectedUsers.length} 个用户
                </Typography.Text>
              </Space>
              {selectedUsers.length ? (
                <Table
                  rowKey="id"
                  size="small"
                  pagination={false}
                  columns={userColumns}
                  dataSource={selectedUsers}
                />
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂未选择用户" />
              )}
            </Form.Item>
          ) : (
            <Form.Item
              label="接收地址"
              extra="多个接收地址可用换行、逗号或分号分隔"
            >
              <Input.TextArea
                rows={4}
                value={receiverText}
                onChange={(event) => setReceiverText(event.target.value)}
                placeholder="手机号 / 邮箱 / webhook 接收地址"
              />
            </Form.Item>
          )}

          <Space>
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={submitting}
              disabled={!access.hasPermission('msg.manage.send')}
              onClick={() => void submit()}
            >
              立即发送
            </Button>
            <Button icon={<ReloadOutlined />} onClick={resetForm}>
              重置
            </Button>
          </Space>
        </Form>
      </Card>

      <Modal
        title="选择接收用户"
        open={pickerOpen}
        width={960}
        onOk={confirmPicker}
        onCancel={() => {
          setPickerSelectedRowKeys([]);
          setPickerSelectedRows([]);
          setPickerOpen(false);
        }}
        destroyOnHidden
      >
        <Space orientation="vertical" style={{ display: 'flex' }} size={16}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '240px 1fr auto',
              gap: 12,
              alignItems: 'center',
            }}
          >
            <TreeSelect
              placeholder="按单位筛选"
              allowClear
              treeDefaultExpandAll
              style={{ width: '100%' }}
              treeData={toTreeData(unitTree)}
              value={pickerParams.unitId}
              onClear={() =>
                setPickerParams((prev) => ({
                  ...prev,
                  unitId: undefined,
                  current: 1,
                }))
              }
              onChange={(value) =>
                setPickerParams((prev) => ({
                  ...prev,
                  unitId: value,
                  current: 1,
                }))
              }
            />
            <Input.Search
              allowClear
              placeholder="请输入姓名/登录名"
              onSearch={(value) =>
                setPickerParams((prev) => ({
                  ...prev,
                  keywords: value,
                  current: 1,
                }))
              }
            />
            <Button onClick={() => loadUsers(pickerParams)}>刷新</Button>
          </div>
          <Table
            rowKey="id"
            loading={pickerLoading}
            columns={pickerColumns}
            dataSource={pickerRows}
            rowSelection={{
              selectedRowKeys: pickerSelectedRowKeys,
              onChange: (keys, rows) => {
                setPickerSelectedRowKeys(keys);
                setPickerSelectedRows(rows);
              },
            }}
            pagination={{
              current: pickerParams.current,
              pageSize: pickerParams.pageSize,
              total: pickerTotal,
              onChange: (page, pageSize) =>
                setPickerParams((prev) => ({
                  ...prev,
                  current: page,
                  pageSize,
                })),
            }}
          />
        </Space>
      </Modal>

      <Modal
        title="发送结果"
        open={resultOpen}
        width={900}
        footer={[
          <Button key="close" type="primary" onClick={() => setResultOpen(false)}>
            关闭
          </Button>,
        ]}
        onCancel={() => setResultOpen(false)}
      >
        <Table
          rowKey={(record) => `${record.receiver}-${record.requestId || record.code || 'result'}`}
          size="small"
          pagination={false}
          columns={resultColumns}
          dataSource={sendResults}
        />
      </Modal>
    </PageContainer>
  );
};

export default MsgSendPage;
