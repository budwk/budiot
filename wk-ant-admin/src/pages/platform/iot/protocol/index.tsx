import {
  BugOutlined,
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
  Card,
  Form,
  Input,
  Modal,
  Radio,
  Space,
  Tag,
  Typography,
} from 'antd';
import * as React from 'react';
import HighlightedCodeEditor from '@/components/HighlightedCodeEditor';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createIotProtocol,
  debugIotProtocol,
  deleteIotProtocol,
  getIotProtocolDetail,
  getIotProtocolMeta,
  getIotProtocolPage,
  updateIotProtocol,
} from '@/services/budiot/iot/protocol';
import type { IotOption, IotProtocolRecord } from '@/services/budiot/iot/typing';
import {
  formatDateTime,
  resolveIotOptionText,
  resolveIotOptionValue,
  safeJsonStringify,
  toSelectOptions,
} from '../shared';

type ProtocolFormValues = {
  id?: string;
  name: string;
  code: string;
  scriptType: string;
  scriptContent: string;
  description?: string;
  disabled: boolean;
};

const defaultValues: ProtocolFormValues = {
  name: '',
  code: '',
  scriptType: 'JAVASCRIPT',
  scriptContent: '',
  description: '',
  disabled: false,
};

const getEditorLanguage = (scriptType?: string): 'javascript' | 'json' | 'text' => {
  const normalized = resolveIotOptionValue(scriptType).toUpperCase();
  if (normalized.includes('JSON')) {
    return 'json';
  }
  if (normalized.includes('JS') || normalized.includes('JAVA')) {
    return 'javascript';
  }
  return 'text';
};

const ProtocolPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<ProtocolFormValues>();
  const [scriptTypes, setScriptTypes] = React.useState<IotOption[]>([]);
  const [sampleScript, setSampleScript] = React.useState('');
  const [sampleInputJson, setSampleInputJson] = React.useState('{}');
  const [modalOpen, setModalOpen] = React.useState(false);
  const [debugOpen, setDebugOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [submitting, setSubmitting] = React.useState(false);
  const [debugSubmitting, setDebugSubmitting] = React.useState(false);
  const [debugInput, setDebugInput] = React.useState('{}');
  const [debugResult, setDebugResult] = React.useState<string>('');
  const currentScriptType = Form.useWatch('scriptType', form);

  React.useEffect(() => {
    const initialize = async () => {
      const response = await getIotProtocolMeta();
      setScriptTypes(response.data.scriptTypes || []);
      setSampleScript(response.data.sampleScript || '');
      setSampleInputJson(response.data.sampleInputJson || '{}');
      setDebugInput(response.data.sampleInputJson || '{}');
    };
    initialize().catch(() => undefined);
  }, []);

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    setDebugInput('');
    setDebugResult('');
    form.setFieldsValue({
      ...defaultValues,
      scriptType: scriptTypes[0]?.value || 'JAVASCRIPT',
      scriptContent: '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: IotProtocolRecord) => {
    const response = await getIotProtocolDetail(record.id);
    setEditingId(record.id);
    form.setFieldsValue({
      id: response.data.id,
      name: response.data.name,
      code: response.data.code,
      scriptType: resolveIotOptionValue(response.data.scriptType),
      scriptContent: response.data.scriptContent || '',
      description: response.data.description || '',
      disabled: response.data.disabled,
    });
    setDebugResult('');
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotProtocol({ ...values, id: editingId });
      } else {
        await createIotProtocol(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const runDebug = async () => {
    const values = await form.validateFields();
    setDebugSubmitting(true);
    try {
      const response = await debugIotProtocol({
        scriptType: values.scriptType,
        scriptContent: values.scriptContent,
        inputJson: debugInput,
      });
      setDebugResult(safeJsonStringify(response.data));
    } finally {
      setDebugSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<IotProtocolRecord>[]>(
    () => [
      { title: '协议名称', dataIndex: 'name' },
      { title: '协议编码', dataIndex: 'code', width: 160 },
      {
        title: '脚本类型',
        dataIndex: 'scriptType',
        width: 140,
        valueType: 'select',
        fieldProps: { options: toSelectOptions(scriptTypes), allowClear: true },
        render: (_, record) => resolveIotOptionText(scriptTypes, record.scriptType),
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        width: 100,
        valueType: 'select',
        fieldProps: {
          options: [
            { label: '启用', value: false },
            { label: '禁用', value: true },
          ],
          allowClear: true,
        },
        render: (_, record) =>
          record.disabled ? <Tag color="error">禁用</Tag> : <Tag color="success">启用</Tag>,
      },
      {
        title: '版本',
        dataIndex: 'scriptVersion',
        search: false,
        width: 100,
      },
      {
        title: '更新时间',
        dataIndex: 'updatedAt',
        search: false,
        width: 180,
        render: (_, record) => formatDateTime(record.updatedAt),
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
                key: 'debug',
                label: '调试',
                icon: <BugOutlined />,
                disabled: !access.hasPermission('iot.manage.protocol'),
                onClick: async () => {
                          await openEdit(record);
                          setDebugInput('');
                          setDebugResult('');
                          setDebugOpen(true);
                        },
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('iot.manage.protocol.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                disabled: !access.hasPermission('iot.manage.protocol.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除协议',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteIotProtocol(record.id);
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
    [access, modal, scriptTypes],
  );

  return (
    <PageContainer title="协议管理">
      <PlatformProTable<IotProtocolRecord, { name?: string; code?: string; scriptType?: string; disabled?: boolean; current?: number; pageSize?: number }>
        persistenceKey="platform-iot-protocol-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="协议脚本"
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.protocol.create')}
            onClick={openCreate}
          >
            新增
          </Button>,
        ]}
        request={async (params) => {
          const response = await getIotProtocolPage({
            name: params.name || '',
            code: params.code || '',
            scriptType: params.scriptType || '',
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
        title={editingId ? '修改协议' : '新增协议'}
        open={modalOpen}
        forceRender
        width={1280}
        mask={{ closable: false }}
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
            <Form.Item label="协议名称" name="name" rules={[{ required: true, message: '请输入协议名称' }]}>
              <Input />
            </Form.Item>
            <Form.Item label="协议编码" name="code" rules={[{ required: true, message: '请输入协议编码' }]}>
              <Input />
            </Form.Item>
          </div>
          <Form.Item label="脚本类型" name="scriptType" rules={[{ required: true, message: '请选择脚本类型' }]}>
            <Radio.Group options={toSelectOptions(scriptTypes)} />
          </Form.Item>
          <Space style={{ marginBottom: 12 }}>
            <Button onClick={() => form.setFieldValue('scriptContent', sampleScript)}>填入示例脚本</Button>
            <Button
              icon={<BugOutlined />}
              onClick={() => {
                setDebugResult('');
                setDebugOpen(true);
              }}
            >
              打开调试
            </Button>
          </Space>
          <Form.Item label="脚本内容" name="scriptContent" rules={[{ required: true, message: '请输入脚本内容' }]}>
            <HighlightedCodeEditor language={getEditorLanguage(currentScriptType)} minHeight={420} />
          </Form.Item>
          <Form.Item label="状态" name="disabled">
            <Radio.Group
              options={[
                { label: '启用', value: false },
                { label: '禁用', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item label="说明" name="description">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="协议调试"
        open={debugOpen}
        width={1120}
        mask={{ closable: false }}
        confirmLoading={debugSubmitting}
        onOk={() => void runDebug()}
        onCancel={() => setDebugOpen(false)}
        destroyOnHidden
      >
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <Card
            size="small"
            title="输入 JSON"
            extra={<Button onClick={() => setDebugInput(sampleInputJson)}>填入示例输入</Button>}
          >
            <HighlightedCodeEditor
              language="json"
              minHeight={220}
              value={debugInput}
              onChange={setDebugInput}
            />
          </Card>
          <Card size="small" title="调试结果">
            {debugResult ? (
              <HighlightedCodeEditor language="json" minHeight={320} value={debugResult} readOnly />
            ) : (
              <Typography.Text type="secondary">执行调试后显示解析结果。</Typography.Text>
            )}
          </Card>
        </Space>
      </Modal>
    </PageContainer>
  );
};

export default ProtocolPage;
