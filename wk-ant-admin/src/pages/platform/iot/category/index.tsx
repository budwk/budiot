import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SortAscendingOutlined,
} from '@ant-design/icons';
import type { ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { TreeDataNode } from 'antd';
import {
  App,
  Button,
  Form,
  Input,
  Modal,
  Radio,
  Switch,
  Tag,
  Tree,
  TreeSelect,
} from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createIotCategory,
  deleteIotCategory,
  getIotCategoryDetail,
  getIotCategoryList,
  getIotCategorySortTree,
  sortIotCategories,
  toggleIotCategoryDisabled,
  updateIotCategory,
} from '@/services/budiot/iot/category';
import type { IotCategoryRecord } from '@/services/budiot/iot/typing';
import { collectTreeKeys, buildTree } from '@/utils/tree';
import {
  flattenIotTreeIds,
  moveIotTreeNode,
  toIotTreeSelectData,
} from '../shared';

type CategoryFormValues = {
  id?: string;
  parentId?: string;
  name: string;
  code: string;
  disabled: boolean;
};

const defaultValues: CategoryFormValues = {
  parentId: '',
  name: '',
  code: '',
  disabled: false,
};

const CategoryPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const [form] = Form.useForm<CategoryFormValues>();
  const [loading, setLoading] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [sortOpen, setSortOpen] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [tableData, setTableData] = React.useState<IotCategoryRecord[]>([]);
  const [treeData, setTreeData] = React.useState<IotCategoryRecord[]>([]);
  const [expanded, setExpanded] = React.useState(true);
  const [sortTreeData, setSortTreeData] = React.useState<TreeDataNode[]>([]);

  const expandedRowKeys = React.useMemo(
    () => (expanded ? collectTreeKeys(tableData) : []),
    [expanded, tableData],
  );

  const loadData = React.useCallback(async () => {
    setLoading(true);
    try {
      const response = await getIotCategoryList();
      const tree = buildTree(response.data || []);
      setTableData(tree);
      setTreeData(tree);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    loadData().catch(() => undefined);
  }, [loadData]);

  const openCreate = (record?: IotCategoryRecord) => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({
      ...defaultValues,
      parentId: record?.id || '',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: IotCategoryRecord) => {
    const response = await getIotCategoryDetail(record.id);
    setEditingId(record.id);
    form.setFieldsValue({
      id: response.data.id,
      parentId: response.data.parentId || '',
      name: response.data.name,
      code: response.data.code,
      disabled: response.data.disabled,
    });
    setModalOpen(true);
  };

  const openSort = async () => {
    const response = await getIotCategorySortTree();
    const tree = buildTree(response.data || []);
    setSortTreeData(toIotTreeSelectData(tree));
    setSortOpen(true);
  };

  const submit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateIotCategory({ ...values, id: editingId });
      } else {
        await createIotCategory(values);
      }
      setModalOpen(false);
      await loadData();
    } finally {
      setSubmitting(false);
    }
  };

  const columns = React.useMemo<ProColumns<IotCategoryRecord>[]>(
    () => [
      {
        title: '分类名称',
        dataIndex: 'name',
      },
      {
        title: '分类编码',
        dataIndex: 'code',
        width: 180,
      },
      {
        title: '状态',
        dataIndex: 'disabled',
        width: 120,
        render: (_, record) => (
          <Switch
            checked={!record.disabled}
            checkedChildren="启用"
            unCheckedChildren="禁用"
            disabled={!access.hasPermission('iot.manage.category.update')}
            onChange={async (checked) => {
              await toggleIotCategoryDisabled(record.id, !checked);
              await loadData();
            }}
          />
        ),
      },
      {
        title: '位置',
        dataIndex: 'location',
        search: false,
        width: 100,
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
                key: 'create',
                label: '新增子类',
                icon: <PlusOutlined />,
                disabled: !access.hasPermission('iot.manage.category.create'),
                onClick: () => openCreate(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('iot.manage.category.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                danger: true,
                icon: <DeleteOutlined />,
                disabled: !access.hasPermission('iot.manage.category.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除分类',
                    content: `确定删除 ${record.name} 吗？`,
                    onOk: async () => {
                      await deleteIotCategory(record.id);
                      await loadData();
                    },
                  });
                },
              },
            ]}
          />
        ),
      },
    ],
    [access, loadData, modal],
  );

  return (
    <PageContainer title="设备分类">
      <PlatformProTable<IotCategoryRecord, { name?: string }>
        persistenceKey="platform-iot-category-table"
        rowKey="id"
        loading={loading}
        headerTitle="分类树"
        search={false}
        dataSource={tableData}
        columns={columns}
        expandable={{
          expandedRowKeys,
          onExpandedRowsChange: (keys) => setExpanded(keys.length > 0),
        }}
        toolBarRender={() => [
          <Button
            key="expand"
            onClick={() => setExpanded((prev) => !prev)}
          >
            {expanded ? '全部折叠' : '全部展开'}
          </Button>,
          <Button
            key="sort"
            icon={<SortAscendingOutlined />}
            disabled={!access.hasPermission('iot.manage.category.update')}
            onClick={() => void openSort()}
          >
            排序
          </Button>,
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('iot.manage.category.create')}
            onClick={() => openCreate()}
          >
            新增
          </Button>,
        ]}
      />

      <Modal
        title={editingId ? '修改分类' : '新增分类'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item label="上级分类" name="parentId">
            <TreeSelect allowClear treeDefaultExpandAll treeData={toIotTreeSelectData(treeData)} />
          </Form.Item>
          <Form.Item label="分类名称" name="name" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="分类编码" name="code" rules={[{ required: true, message: '请输入分类编码' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="状态" name="disabled">
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
        title="分类排序"
        open={sortOpen}
        onOk={async () => {
          await sortIotCategories(flattenIotTreeIds(sortTreeData).toString());
          setSortOpen(false);
          await loadData();
        }}
        onCancel={() => setSortOpen(false)}
        destroyOnHidden
      >
        <Tree
          draggable
          blockNode
          treeData={sortTreeData}
          allowDrop={({ dragNode, dropNode }) =>
            (dragNode as any)?.parentId === (dropNode as any)?.parentId
          }
          onDrop={(info) => {
            if (!info.dropToGap) {
              return;
            }
            setSortTreeData((prev) =>
              moveIotTreeNode(
                prev,
                info.dragNode.key,
                info.node.key,
                info.dropPosition < 0 ? 'before' : 'after',
              ),
            );
          }}
        />
      </Modal>
    </PageContainer>
  );
};

export default CategoryPage;
