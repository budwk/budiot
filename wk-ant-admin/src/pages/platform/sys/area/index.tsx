import {
  DeleteOutlined,
  EditOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  PlusOutlined,
  SortAscendingOutlined,
} from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { TableColumnsType } from 'antd';
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Tooltip,
  Tree,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import {
  createArea,
  deleteArea,
  getAreaDetail,
  getAreaList,
  sortAreas,
  updateArea,
  updateAreaDisabled,
} from '@/services/budiot/sys/area';
import type { SysAreaRecord } from '@/services/budiot/typing';
import { buildTree, collectTreeKeys } from '@/utils/tree';

type AreaFormValues = {
  id?: string;
  parentId?: string;
  parentName?: string;
  name: string;
  code: string;
  disabled: boolean;
};

type SortTreeNode = {
  key: string;
  title: string;
  parentId?: string;
  children?: SortTreeNode[];
};

const defaultFormValues: AreaFormValues = {
  parentId: '',
  parentName: '',
  name: '',
  code: '',
  disabled: false,
};

const toSortTreeData = (items: SysAreaRecord[]): SortTreeNode[] =>
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
  const clone = JSON.parse(JSON.stringify(tree)) as SortTreeNode[];
  let draggedNode: SortTreeNode | undefined;

  const traverse = (
    items: SortTreeNode[],
    key: React.Key,
    callback: (item: SortTreeNode, index: number, arr: SortTreeNode[]) => void,
  ) => {
    items.forEach((item, index, arr) => {
      if (item.key === key) {
        callback(item, index, arr);
        return;
      }
      if (item.children?.length) {
        traverse(item.children, key, callback);
      }
    });
  };

  traverse(clone, dragKey, (item, index, arr) => {
    draggedNode = item;
    arr.splice(index, 1);
  });

  if (!draggedNode) return clone;

  traverse(clone, dropKey, (_item, index, arr) => {
    if (!draggedNode) return;
    arr.splice(position === 'before' ? index : index + 1, 0, draggedNode);
  });

  return clone;
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

const AreaPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const [areaForm] = Form.useForm<AreaFormValues>();
  const [loading, setLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [sortSubmitting, setSortSubmitting] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [sortOpen, setSortOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [tableData, setTableData] = React.useState<SysAreaRecord[]>([]);
  const [sortTreeData, setSortTreeData] = React.useState<SortTreeNode[]>([]);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);
  const [expandedRowKeys, setExpandedRowKeys] = React.useState<React.Key[]>([]);

  const allExpandedRowKeys = React.useMemo(() => collectTreeKeys(tableData), [tableData]);
  const expanded = allExpandedRowKeys.length > 0 && expandedRowKeys.length === allExpandedRowKeys.length;

  const loadAreas = React.useCallback(async () => {
    setLoading(true);
    try {
      const response = await getAreaList();
      const tree = buildTree(response.data || []);
      setTableData(tree);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    loadAreas().catch(() => undefined);
  }, [loadAreas]);

  React.useEffect(() => {
    setExpandedRowKeys((prev) => {
      if (!prev.length) {
        return prev;
      }
      const nextKeySet = new Set(allExpandedRowKeys);
      return prev.filter((key) => nextKeySet.has(String(key)));
    });
  }, [allExpandedRowKeys]);

  const openCreate = (record?: SysAreaRecord) => {
    setEditingId(undefined);
    areaForm.resetFields();
    areaForm.setFieldsValue({
      ...defaultFormValues,
      parentId: record?.id || '',
      parentName: record?.name || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysAreaRecord) => {
    const response = await getAreaDetail(record.id);
    setEditingId(record.id);
    areaForm.setFieldsValue({
      ...response.data,
      parentName: response.data.parentName || '',
      disabled: response.data.disabled,
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await areaForm.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateArea({ ...values, id: editingId });
      } else {
        await createArea(values);
      }
      setModalOpen(false);
      await loadAreas();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisabledChange = async (record: SysAreaRecord, checked: boolean) => {
    setSwitchingIds((prev) => [...prev, record.id]);
    try {
      await updateAreaDisabled({ id: record.id, disabled: !checked });
      await loadAreas();
    } finally {
      setSwitchingIds((prev) => prev.filter((item) => item !== record.id));
    }
  };

  const columns = React.useMemo<TableColumnsType<SysAreaRecord>>(
    () => [
      {
        title: '区域名称',
        dataIndex: 'name',
        key: 'name',
      },
      {
        title: '区域编码',
        dataIndex: 'code',
        key: 'code',
        render: (value) => (value ? <Typography.Text code>{value}</Typography.Text> : '-'),
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        key: 'disabled',
        width: 120,
        align: 'center',
        render: (value: boolean, record) => (
          <Switch
            checked={!value}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            loading={switchingIds.includes(record.id)}
            disabled={!access.hasPermission('sys.config.area.update')}
            onChange={(checked) => handleDisabledChange(record, checked)}
          />
        ),
      },
      {
        title: '创建时间',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: 180,
        render: (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'),
      },
      {
        title: '操作',
        key: 'actions',
        width: 220,
        render: (_, record) => (
          <Space size="small">
            <Button
              type="link"
              icon={<PlusOutlined />}
              disabled={!access.hasPermission('sys.config.area.create')}
              onClick={() => openCreate(record)}
            >
              子区域
            </Button>
            <Button
              type="link"
              icon={<EditOutlined />}
              disabled={!access.hasPermission('sys.config.area.update')}
              onClick={() => openEdit(record)}
            >
              修改
            </Button>
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              disabled={!access.hasPermission('sys.config.area.delete')}
              onClick={() => {
                modal.confirm({
                  title: '确认删除区域',
                  content: `此操作将删除 ${record.name} 及其下级，请谨慎操作。`,
                  onOk: async () => {
                    await deleteArea(record.id);
                    await loadAreas();
                  },
                });
              }}
            >
              删除
            </Button>
          </Space>
        ),
      },
    ],
    [access, loadAreas, modal, switchingIds],
  );

  return (
    <PageContainer title="区域管理">
      <ProCard>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Space wrap>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              disabled={!access.hasPermission('sys.config.area.create')}
              onClick={() => openCreate()}
            >
              新增
            </Button>
            <Button
              icon={<SortAscendingOutlined />}
              disabled={!access.hasPermission('sys.config.area.update')}
              onClick={async () => {
                const response = await getAreaList();
                setSortTreeData(toSortTreeData(buildTree(response.data || [])));
                setSortOpen(true);
              }}
            >
              排序
            </Button>
            <Tooltip title={expanded ? '折叠区域树' : '展开区域树'}>
              <Button
                icon={expanded ? <FolderOutlined /> : <FolderOpenOutlined />}
                onClick={() =>
                  setExpandedRowKeys(expanded ? [] : allExpandedRowKeys)
                }
              >
                {expanded ? '折叠' : '展开'}
              </Button>
            </Tooltip>
          </Space>

          <Table<SysAreaRecord>
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={tableData}
            pagination={false}
            expandable={{
              expandedRowKeys,
              onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
            }}
          />
        </div>
      </ProCard>

      <Modal
        title={editingId ? '修改区域' : '新增区域'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={areaForm} layout="vertical">
          {!editingId ? (
            <Form.Item label="上级区域" name="parentName">
              <Input disabled placeholder="请输入上级区域" />
            </Form.Item>
          ) : null}
          <Form.Item
            label="区域名称"
            name="name"
            rules={[{ required: true, message: '请输入区域名称' }]}
          >
            <Input maxLength={100} placeholder="请输入区域名称" />
          </Form.Item>
          <Form.Item
            label="区域编码"
            name="code"
            rules={[{ required: true, message: '请输入区域编码' }]}
          >
            <Input maxLength={32} placeholder="请输入区域编码" />
          </Form.Item>
          <Form.Item label="区域状态" name="disabled" valuePropName="checked">
            <Switch checkedChildren="禁用" unCheckedChildren="启用" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="区域排序"
        open={sortOpen}
        confirmLoading={sortSubmitting}
        onOk={async () => {
          setSortSubmitting(true);
          try {
            await sortAreas(flattenSortIds(sortTreeData).toString());
            setSortOpen(false);
            await loadAreas();
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
              moveSortNode(
                prev,
                dragNode.key,
                dropNode.key,
                info.dropPosition < 0 ? 'before' : 'after',
              ),
            );
          }}
        />
      </Modal>
    </PageContainer>
  );
};

export default AreaPage;
