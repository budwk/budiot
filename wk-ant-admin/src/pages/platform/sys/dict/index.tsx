import {
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
import type { TreeDataNode } from 'antd';
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Switch,
  Tooltip,
  Tree,
  TreeSelect,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createDict,
  deleteDict,
  getDictDetail,
  getDictList,
  sortDicts,
  updateDict,
  updateDictDisabled,
} from '@/services/budiot/sys/dict';
import type { SysDictRecord } from '@/services/budiot/typing';
import { buildTree, collectTreeKeys } from '@/utils/tree';

type DictFormValues = {
  id?: string;
  parentId?: string;
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

type DictTableParams = {
  name?: string;
};

const defaultFormValues: DictFormValues = {
  parentId: '',
  name: '',
  code: '',
  disabled: false,
};

const toTreeSelectData = (items: SysDictRecord[]): TreeDataNode[] =>
  items.map((item) => ({
    key: item.id,
    value: item.id,
    title: item.name,
    children: item.children?.length ? toTreeSelectData(item.children) : undefined,
  }));

const toSortTreeData = (items: SysDictRecord[]): SortTreeNode[] =>
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

const DictPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [dictForm] = Form.useForm<DictFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [sortSubmitting, setSortSubmitting] = React.useState(false);
  const [expanded, setExpanded] = React.useState(true);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [sortOpen, setSortOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [tableData, setTableData] = React.useState<SysDictRecord[]>([]);
  const [treeOptions, setTreeOptions] = React.useState<TreeDataNode[]>([]);
  const [sortTreeData, setSortTreeData] = React.useState<SortTreeNode[]>([]);
  const [switchingIds, setSwitchingIds] = React.useState<string[]>([]);

  const expandedRowKeys = React.useMemo(
    () => (expanded ? collectTreeKeys(tableData) : []),
    [expanded, tableData],
  );

  const loadSortTree = async () => {
    const response = await getDictList({ name: '' });
    setSortTreeData(toSortTreeData(buildTree(response.data || [])));
  };

  const openCreate = (record?: SysDictRecord) => {
    setEditingId(undefined);
    dictForm.resetFields();
    dictForm.setFieldsValue({
      ...defaultFormValues,
      parentId: record?.id || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysDictRecord) => {
    const response = await getDictDetail(record.id);
    setEditingId(record.id);
    dictForm.setFieldsValue({
      ...response.data,
      parentId: response.data.parentId || '',
      disabled: response.data.disabled,
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await dictForm.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateDict({ ...values, id: editingId });
      } else {
        await createDict(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisabledChange = async (record: SysDictRecord, checked: boolean) => {
    setSwitchingIds((prev) => [...prev, record.id]);
    try {
      await updateDictDisabled({ id: record.id, disabled: !checked });
      actionRef.current?.reload();
    } finally {
      setSwitchingIds((prev) => prev.filter((item) => item !== record.id));
    }
  };

  const columns = React.useMemo<ProColumns<SysDictRecord>[]>(
    () => [
      {
        title: '字典名称',
        dataIndex: 'name',
      },
      {
        title: '字典编码',
        dataIndex: 'code',
        search: false,
        render: (_, record) =>
          record.code ? <Typography.Text code>{record.code}</Typography.Text> : '-',
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
            loading={switchingIds.includes(record.id)}
            disabled={!access.hasPermission('sys.config.dict.update')}
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
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'child',
                label: '子字典',
                icon: <PlusOutlined />,
                disabled: !access.hasPermission('sys.config.dict.create'),
                onClick: () => openCreate(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('sys.config.dict.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                hidden: record.path === '0001',
                disabled: !access.hasPermission('sys.config.dict.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除字典',
                    content: `此操作将删除 ${record.name} 及其下级，请谨慎操作。`,
                    onOk: async () => {
                      await deleteDict(record.id);
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
    <PageContainer title="字典管理">
      <PlatformProTable<SysDictRecord, DictTableParams>
        persistenceKey="platform-sys-dict-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="字典列表"
        pagination={false}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.config.dict.create')}
            onClick={() => openCreate()}
          >
            新增
          </Button>,
          <Button
            key="sort"
            icon={<SortAscendingOutlined />}
            disabled={!access.hasPermission('sys.config.dict.update')}
            onClick={async () => {
              await loadSortTree();
              setSortOpen(true);
            }}
          >
            排序
          </Button>,
          <Tooltip key="expand" title={expanded ? '折叠字典树' : '展开字典树'}>
            <Button
              icon={expanded ? <FolderOutlined /> : <FolderOpenOutlined />}
              onClick={() => setExpanded((prev) => !prev)}
            >
              {expanded ? '折叠' : '展开'}
            </Button>
          </Tooltip>,
        ]}
        request={async (params) => {
          const response = await getDictList({ name: params.name || '' });
          const tree = buildTree(response.data || []);
          setTableData(tree);
          setTreeOptions(toTreeSelectData(tree));
          return {
            data: tree,
            total: tree.length,
            success: true,
          };
        }}
        expandable={{
          expandedRowKeys,
          defaultExpandAllRows: true,
        }}
      />

      <Modal
        title={editingId ? '修改字典' : '新增字典'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={dictForm} layout="vertical">
          <Form.Item label="上级字典" name="parentId">
            <TreeSelect
              allowClear
              treeDefaultExpandAll
              placeholder="请选择上级字典"
              treeData={treeOptions}
              disabled={Boolean(editingId)}
            />
          </Form.Item>
          <Form.Item
            label="字典名称"
            name="name"
            rules={[{ required: true, message: '请输入字典名称' }]}
          >
            <Input maxLength={100} placeholder="请输入字典名称" />
          </Form.Item>
          <Form.Item
            label="字典编码"
            name="code"
            rules={[{ required: true, message: '请输入字典编码' }]}
          >
            <Input maxLength={32} placeholder="请输入字典编码" />
          </Form.Item>
          <Form.Item label="字典状态" name="disabled" valuePropName="checked">
            <Switch checkedChildren="禁用" unCheckedChildren="启用" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="字典排序"
        open={sortOpen}
        confirmLoading={sortSubmitting}
        onOk={async () => {
          setSortSubmitting(true);
          try {
            await sortDicts(flattenSortIds(sortTreeData).toString());
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

export default DictPage;
