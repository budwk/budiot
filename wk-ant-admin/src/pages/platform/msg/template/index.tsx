import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  Button,
  Form,
  Input,
  Modal,
  Radio,
  Tag,
} from 'antd';
import { App } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createMsgTemplate,
  deleteMsgTemplate,
  getMsgTemplateDetail,
  getMsgTemplateMeta,
  getMsgTemplatePage,
  updateMsgTemplate,
} from '@/services/budiot/msg/template';
import type { MsgOption, MsgTemplateRecord } from '@/services/budiot/typing';

type MsgTemplateFormValues = {
  id?: string;
  channelId: string;
  channelType: string;
  providerType: string;
  bizType: string;
  name: string;
  templateCode?: string;
  paramsJson?: string;
  content?: string;
  disabled: boolean;
};

type MsgTemplateTableParams = {
  current?: number;
  pageSize?: number;
  name?: string;
  channelId?: string;
  bizType?: string;
};

const defaultFormValues: MsgTemplateFormValues = {
  channelId: '',
  channelType: '',
  providerType: '',
  bizType: '',
  name: '',
  templateCode: '',
  paramsJson: '',
  content: '',
  disabled: false,
};

const resolveOptionValue = (value?: string | MsgOption) =>
  typeof value === 'string' ? value : value?.value || '';

const resolveOptionText = (options: MsgOption[], value?: string | MsgOption) => {
  const currentValue = resolveOptionValue(value);
  return options.find((item) => item.value === currentValue)?.text || currentValue || '-';
};

const MsgTemplatePage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<MsgTemplateFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [metaLoading, setMetaLoading] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [channels, setChannels] = React.useState<
    Array<{ id: string; name: string; channelType?: string; providerType?: string }>
  >([]);
  const [bizTypes, setBizTypes] = React.useState<MsgOption[]>([]);

  const channelMap = React.useMemo(
    () => new Map(channels.map((item) => [item.id, item])),
    [channels],
  );

  React.useEffect(() => {
    const initialize = async () => {
      setMetaLoading(true);
      try {
        const response = await getMsgTemplateMeta();
        setChannels(response.data.channels || []);
        setBizTypes(response.data.bizTypes || []);
      } finally {
        setMetaLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const handleChannelChange = React.useCallback(
    (channelId?: string) => {
      const channel = channelMap.get(channelId || '');
      form.setFieldValue('channelType', channel?.channelType || '');
      form.setFieldValue('providerType', channel?.providerType || '');
    },
    [channelMap, form],
  );

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({
      ...defaultFormValues,
      bizType: bizTypes[0]?.value || 'NOTIFY',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: MsgTemplateRecord) => {
    const response = await getMsgTemplateDetail(record.id);
    const detail = response.data;
    setEditingId(record.id);
    form.setFieldsValue({
      id: detail.id,
      channelId: detail.channelId,
      channelType: resolveOptionValue(detail.channelType),
      providerType: resolveOptionValue(detail.providerType),
      bizType: resolveOptionValue(detail.bizType),
      name: detail.name,
      templateCode: detail.templateCode || '',
      paramsJson: detail.paramsJson || '',
      content: detail.content || '',
      disabled: detail.disabled,
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateMsgTemplate({ ...values, id: editingId });
      } else {
        await createMsgTemplate(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<MsgTemplateRecord>[]>(
    () => [
      {
        title: '模板名称',
        dataIndex: 'name',
      },
      {
        title: '渠道',
        dataIndex: 'channelId',
        valueType: 'select',
        fieldProps: {
          options: channels.map((item) => ({
            label: item.name,
            value: item.id,
          })),
          allowClear: true,
        },
        render: (_, record) => record.channel?.name || channelMap.get(record.channelId)?.name || '-',
        width: 140,
      },
      {
        title: '业务类型',
        dataIndex: 'bizType',
        valueType: 'select',
        fieldProps: {
          options: bizTypes.map((item) => ({
            label: item.text,
            value: item.value,
          })),
          allowClear: true,
        },
        render: (_, record) => resolveOptionText(bizTypes, record.bizType),
        width: 120,
      },
      {
        title: '模板编码',
        dataIndex: 'templateCode',
        search: false,
        width: 160,
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        search: false,
        width: 100,
        render: (_, record) =>
          record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        search: false,
        valueType: 'dateTime',
        width: 180,
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
                disabled: !access.hasPermission('msg.manage.template.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('msg.manage.template.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除模板',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteMsgTemplate(record.id);
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
    [access, bizTypes, channelMap, channels, modal],
  );

  return (
    <PageContainer title="消息模板">
      <PlatformProTable<MsgTemplateRecord, MsgTemplateTableParams>
        persistenceKey="platform-msg-template-table"
        actionRef={actionRef}
        rowKey="id"
        loading={metaLoading}
        headerTitle="模板列表"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('msg.manage.template.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getMsgTemplatePage({
            name: params.name || '',
            channelId: params.channelId || '',
            bizType: params.bizType || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
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
        title={editingId ? '修改模板' : '新增模板'}
        open={modalOpen}
        forceRender
        width={760}
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 16,
            }}
          >
            <Form.Item
              label="渠道"
              name="channelId"
              rules={[{ required: true, message: '请选择渠道' }]}
            >
              <Radio.Group
                options={channels.map((item) => ({
                  label: item.name,
                  value: item.id,
                }))}
                onChange={(event) => handleChannelChange(event.target.value)}
              />
            </Form.Item>
            <Form.Item
              label="业务类型"
              name="bizType"
              rules={[{ required: true, message: '请选择业务类型' }]}
            >
              <Radio.Group
                options={bizTypes.map((item) => ({
                  label: item.text,
                  value: item.value,
                }))}
              />
            </Form.Item>
            <Form.Item
              label="模板名称"
              name="name"
              rules={[{ required: true, message: '请输入模板名称' }]}
            >
              <Input placeholder="请输入模板名称" />
            </Form.Item>
            <Form.Item label="启用状态" name="disabled">
              <Radio.Group
                options={[
                  { label: '启用', value: false },
                  { label: '禁用', value: true },
                ]}
              />
            </Form.Item>
          </div>
          <Form.Item name="channelType" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="providerType" hidden>
            <Input />
          </Form.Item>
          <Form.Item label="模板编码" name="templateCode">
            <Input placeholder="请输入模板编码" />
          </Form.Item>
          <Form.Item label="默认参数JSON" name="paramsJson">
            <Input.TextArea rows={3} placeholder='{"code":"1234"}' />
          </Form.Item>
          <Form.Item label="模板内容" name="content">
            <Input.TextArea rows={6} placeholder="请输入模板内容" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default MsgTemplatePage;
