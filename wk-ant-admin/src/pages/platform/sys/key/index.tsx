import {
  DeleteOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { App, Button, Form, Input, Modal, Space, Switch, Tag } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import { createKey, deleteKey, getKeyPage, updateKeyDisabled } from '@/services/budiot/sys/key';
import type { SysKeyRecord } from '@/services/budiot/typing';

type KeyFormValues = {
  name: string;
};

const KeyPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<KeyFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [visibleIds, setVisibleIds] = React.useState<string[]>([]);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);

  const columns = React.useMemo<ProColumns<SysKeyRecord>[]>(
    () => [
      {
        title: '密钥名称',
        dataIndex: 'name',
        width: 180,
      },
      {
        title: 'APP ID',
        dataIndex: 'appid',
        width: 180,
      },
      {
        title: 'APP KEY',
        dataIndex: 'appkey',
        search: false,
        render: (_, record) => {
          const visible = visibleIds.includes(record.appid);
          return (
            <Space size="small">
              <span style={visible ? { background: '#f6ffed', padding: '0 8px' } : undefined}>
                {visible ? record.appkey : '••••••••••••••••'}
              </span>
              <Button
                type="link"
                size="small"
                icon={visible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                onClick={() =>
                  setVisibleIds((prev) =>
                    visible ? prev.filter((item) => item !== record.appid) : [...prev, record.appid],
                  )
                }
              >
                {visible ? '隐藏' : '显示'}
              </Button>
            </Space>
          );
        },
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
            loading={switchingIds.includes(record.appid)}
            disabled={!access.hasPermission('sys.config.key.update')}
            onChange={async (checked) => {
              setSwitchingIds((prev) => [...prev, record.appid]);
              try {
                await updateKeyDisabled({ appid: record.appid, disabled: !checked });
                actionRef.current?.reload();
              } finally {
                setSwitchingIds((prev) => prev.filter((item) => item !== record.appid));
              }
            }}
          />
        ),
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 120,
        render: (_, record) => [
          <Button
            key="delete"
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={!access.hasPermission('sys.config.key.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认删除密钥',
                content: `确定删除 ${record.name} 吗？`,
                onOk: async () => {
                  await deleteKey(record.appid);
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
    [access, modal, switchingIds, visibleIds],
  );

  return (
    <PageContainer title="密钥管理">
      <PlatformProTable<SysKeyRecord, { current?: number; pageSize?: number }>
        persistenceKey="platform-sys-key-table"
        actionRef={actionRef}
        rowKey="appid"
        search={false}
        headerTitle="密钥列表"
        toolbar={{
          subTitle: <Tag color="processing">默认隐藏 APP KEY，点击后可临时显示</Tag>,
        }}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.config.key.create')}
            onClick={() => {
              form.resetFields();
              setModalOpen(true);
            }}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getKeyPage({
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
        title="新增密钥"
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={async () => {
          const values = await form.validateFields();
          setSubmitting(true);
          try {
            await createKey(values);
            setModalOpen(false);
            actionRef.current?.reload();
          } finally {
            setSubmitting(false);
          }
        }}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="密钥名称"
            name="name"
            rules={[{ required: true, message: '请输入密钥名称' }]}
          >
            <Input placeholder="请输入密钥名称" />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default KeyPage;
