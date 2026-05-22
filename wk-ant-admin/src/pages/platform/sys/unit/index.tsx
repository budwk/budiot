import {
  ApartmentOutlined,
  BankOutlined,
  BuildOutlined,
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
  Radio,
  Select,
  Space,
  Tag,
  Tree,
  TreeSelect,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  createUnit,
  deleteUnit,
  getUnitDetail,
  getUnitList,
  searchUnitUsers,
  sortUnits,
  updateUnit,
} from '@/services/budiot/sys/unit';
import type {
  SysUnitDetail,
  SysUnitRecord,
  SysUserOption,
} from '@/services/budiot/typing';
import { buildTree, collectTreeKeys } from '@/utils/tree';

type LeaderKind = 'leaders' | 'highers' | 'assigners';

type UnitFormValues = {
  id?: string;
  parentId: string;
  name: string;
  aliasName?: string;
  type: 'GROUP' | 'COMPANY' | 'UNIT';
  unitcode?: string;
  address?: string;
  telephone?: string;
  email?: string;
  website?: string;
  leaderName?: string;
  leaderMobile?: string;
  note?: string;
  disabled: boolean;
  parentType?: SysUnitRecord['type'] | '';
  leader?: string;
  higher?: string;
  assigner?: string;
};

type UnitTableParams = {
  name?: string;
  leaderName?: string;
};

const defaultFormValues: UnitFormValues = {
  parentId: '',
  name: '',
  aliasName: '',
  type: 'UNIT',
  unitcode: '',
  address: '',
  telephone: '',
  email: '',
  website: '',
  leaderName: '',
  leaderMobile: '',
  note: '',
  disabled: false,
  parentType: '',
  leader: '',
  higher: '',
  assigner: '',
};

const toTreeSelectData = (items: SysUnitRecord[]): TreeDataNode[] =>
  items.map((item) => ({
    key: item.id,
    value: item.id,
    title: item.name,
    type: item.type,
    parentId: item.parentId,
    children: item.children ? toTreeSelectData(item.children) : undefined,
  }));

const moveNode = (
  tree: TreeDataNode[],
  dragKey: React.Key,
  dropKey: React.Key,
  position: 'before' | 'after',
) => {
  const clone = JSON.parse(JSON.stringify(tree)) as TreeDataNode[];
  let draggedNode: TreeDataNode | undefined;

  const traverse = (
    items: TreeDataNode[],
    key: React.Key,
    callback: (item: TreeDataNode, index: number, arr: TreeDataNode[]) => void,
  ) => {
    items.forEach((item, index, arr) => {
      if (item.key === key) {
        callback(item, index, arr);
        return;
      }
      if (item.children) {
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

const flattenUnitIds = (items: TreeDataNode[], acc: string[] = []) => {
  items.forEach((item) => {
    acc.push(String(item.key));
    if (item.children?.length) {
      flattenUnitIds(item.children, acc);
    }
  });
  return acc;
};

const resolveUnitTypeValue = (type?: SysUnitRecord['type'] | string) =>
  typeof type === 'string' ? type : type?.value || '';

const resolveUnitTypeText = (type?: SysUnitRecord['type'] | string) => {
  const value = resolveUnitTypeValue(type);
  if (value === 'GROUP') {
    return '总公司';
  }
  if (value === 'COMPANY') {
    return '公司';
  }
  if (value === 'UNIT') {
    return '部门';
  }
  return typeof type === 'object' ? type?.text || '-' : '-';
};

const renderUnitTypeIcon = (type?: SysUnitRecord['type'] | string) => {
  const value = resolveUnitTypeValue(type);
  if (value === 'GROUP') {
    return <BankOutlined style={{ color: '#9254de' }} />;
  }
  if (value === 'COMPANY') {
    return <BuildOutlined style={{ color: '#1677ff' }} />;
  }
  return <ApartmentOutlined style={{ color: '#13a8a8' }} />;
};

const UnitPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [unitForm] = Form.useForm<UnitFormValues>();
  const [submitting, setSubmitting] = React.useState(false);
  const [sortOpen, setSortOpen] = React.useState(false);
  const [modalOpen, setModalOpen] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string>();
  const [tableData, setTableData] = React.useState<SysUnitRecord[]>([]);
  const [sortTreeData, setSortTreeData] = React.useState<TreeDataNode[]>([]);
  const [flatUnitList, setFlatUnitList] = React.useState<SysUnitRecord[]>([]);
  const [expandedRowKeys, setExpandedRowKeys] = React.useState<React.Key[]>([]);
  const [leaderOptions, setLeaderOptions] = React.useState<Record<LeaderKind, SysUserOption[]>>({
    leaders: [],
    highers: [],
    assigners: [],
  });
  const [selectedLeaderIds, setSelectedLeaderIds] = React.useState<string[]>([]);
  const [selectedHigherIds, setSelectedHigherIds] = React.useState<string[]>([]);
  const [selectedAssignerIds, setSelectedAssignerIds] = React.useState<string[]>([]);

  const currentParentId = Form.useWatch('parentId', unitForm);
  const currentType = Form.useWatch('type', unitForm);
  const currentParentRecord = React.useMemo(
    () => flatUnitList.find((item) => item.id === currentParentId),
    [currentParentId, flatUnitList],
  );
  const currentParentTypeValue = resolveUnitTypeValue(currentParentRecord?.type);
  const allExpandedRowKeys = React.useMemo(() => collectTreeKeys(tableData), [tableData]);
  const initializedExpansionRef = React.useRef(false);

  React.useEffect(() => {
    setExpandedRowKeys((prev) => {
      if (!initializedExpansionRef.current) {
        initializedExpansionRef.current = true;
        return allExpandedRowKeys;
      }
      const nextKeySet = new Set(allExpandedRowKeys.map((item) => String(item)));
      return prev.filter((key) => nextKeySet.has(String(key)));
    });
  }, [allExpandedRowKeys]);

  React.useEffect(() => {
    if (!modalOpen) {
      return;
    }
    if (!currentParentId) {
      if (currentType !== 'GROUP') {
        unitForm.setFieldValue('type', 'GROUP');
      }
      return;
    }
    if (currentParentTypeValue === 'GROUP') {
      if (currentType === 'GROUP') {
        unitForm.setFieldValue('type', 'COMPANY');
      }
      return;
    }
    if (currentType !== 'UNIT') {
      unitForm.setFieldValue('type', 'UNIT');
    }
  }, [currentParentId, currentParentTypeValue, currentType, modalOpen, unitForm]);

  const expanded = allExpandedRowKeys.length > 0 && expandedRowKeys.length === allExpandedRowKeys.length;

  const resetRemoteUsers = () => {
    setLeaderOptions({ leaders: [], highers: [], assigners: [] });
    setSelectedLeaderIds([]);
    setSelectedHigherIds([]);
    setSelectedAssignerIds([]);
  };

  const openCreate = (record?: SysUnitRecord) => {
    setEditingId(undefined);
    resetRemoteUsers();
    unitForm.resetFields();
    unitForm.setFieldsValue({
      ...defaultFormValues,
      parentId: record?.id || '',
      parentType: record?.type || '',
      type: record ? (record.type?.value === 'GROUP' ? 'COMPANY' : 'UNIT') : 'GROUP',
    });
    setModalOpen(true);
  };

  const openEdit = async (record: SysUnitRecord) => {
    const response = await getUnitDetail(record.id);
    const { unit, unitUserList = [] } = response.data;
    setEditingId(record.id);
    resetRemoteUsers();

    const leaders = unitUserList
      .filter((item) => item.leaderType?.value === 'LEADER')
      .map((item) => ({ value: item.userId, label: item.user.username }));
    const highers = unitUserList
      .filter((item) => item.leaderType?.value === 'HIGHER')
      .map((item) => ({ value: item.userId, label: item.user.username }));
    const assigners = unitUserList
      .filter((item) => item.leaderType?.value === 'ASSIGNER')
      .map((item) => ({ value: item.userId, label: item.user.username }));

    setLeaderOptions({ leaders, highers, assigners });
    setSelectedLeaderIds(leaders.map((item) => item.value));
    setSelectedHigherIds(highers.map((item) => item.value));
    setSelectedAssignerIds(assigners.map((item) => item.value));

    unitForm.setFieldsValue({
      ...(unit as SysUnitDetail),
      type:
        typeof unit.type === 'string'
          ? (unit.type as UnitFormValues['type'])
          : ((unit.type?.value as UnitFormValues['type']) || 'UNIT'),
      parentType: flatUnitList.find((item) => item.id === unit.parentId)?.type || '',
    });
    setModalOpen(true);
  };

  const submit = async () => {
    const values = await unitForm.validateFields();
    setSubmitting(true);
    try {
      const payload = {
        ...values,
        id: editingId,
        leader: selectedLeaderIds.toString(),
        higher: selectedHigherIds.toString(),
        assigner: selectedAssignerIds.toString(),
      };
      if (editingId) {
        await updateUnit(payload);
      } else {
        await createUnit(payload);
      }
      setModalOpen(false);
      actionRef.current?.reload();
    } finally {
      setSubmitting(false);
    }
  };

  const loadUserOptions = async (query: string, kind: LeaderKind) => {
    const unitId = kind === 'leaders' ? editingId || '' : currentParentId || '';
    if (!unitId) return;
    const response = await searchUnitUsers(query, unitId);
    setLeaderOptions((prev) => ({
      ...prev,
      [kind]: response.data.list || [],
    }));
  };

  const columns = React.useMemo<ProColumns<SysUnitRecord>[]>(
    () => [
      {
        title: '单位名称',
        dataIndex: 'name',
        render: (_, record) => (
          <Space>
            {renderUnitTypeIcon(record.type)}
            <span>{record.name}</span>
          </Space>
        ),
      },
      {
        title: '部门负责人',
        dataIndex: 'leaderName',
        hideInTable: true,
      },
      {
        title: '单位类型',
        dataIndex: 'type',
        search: false,
        render: (_, record) => resolveUnitTypeText(record.type),
      },
      {
        title: '部门负责人',
        dataIndex: 'leaderName',
        search: false,
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
                label: '子单位',
                icon: <PlusOutlined />,
                disabled: !access.hasPermission('sys.manage.unit.create'),
                onClick: () => openCreate(record),
              },
              {
                key: 'edit',
                label: '修改',
                icon: <EditOutlined />,
                disabled: !access.hasPermission('sys.manage.unit.update'),
                onClick: () => openEdit(record),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                hidden: record.path === '0001',
                disabled: !access.hasPermission('sys.manage.unit.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除单位',
                    content: '此操作将删除单位、下级单位以及关联角色，请谨慎操作。',
                    onOk: async () => {
                      await deleteUnit(record.id);
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
    [access, flatUnitList, modal],
  );

  return (
    <PageContainer title="单位管理">
      <PlatformProTable<SysUnitRecord, UnitTableParams>
        persistenceKey="platform-sys-unit-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="单位列表"
        pagination={false}
        scroll={{ x: 1200 }}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="create"
            type="primary"
            icon={<PlusOutlined />}
            disabled={!access.hasPermission('sys.manage.unit.create')}
            onClick={() => openCreate()}
          >
            新增
          </Button>,
          <Button
            key="sort"
            icon={<SortAscendingOutlined />}
            disabled={!access.hasPermission('sys.manage.unit.update')}
            onClick={() => setSortOpen(true)}
          >
            排序
          </Button>,
            <Button
              key="expand"
              icon={expanded ? <FolderOutlined /> : <FolderOpenOutlined />}
              onClick={() => setExpandedRowKeys(expanded ? [] : allExpandedRowKeys)}
            >
              {expanded ? '折叠' : '展开'}
            </Button>,
        ]}
        request={async (params) => {
          const response = await getUnitList({
            name: params.name,
            leaderName: params.leaderName,
          });
          const list = response.data || [];
          const tree = buildTree(list);
          setFlatUnitList(list);
          setTableData(tree);
          setSortTreeData(toTreeSelectData(tree));
          return {
            data: tree,
            total: tree.length,
            success: true,
          };
        }}
        expandable={{
          expandedRowKeys,
          onExpandedRowsChange: (keys) => setExpandedRowKeys([...keys]),
        }}
      />

      <Modal
        title={editingId ? '修改单位' : '新增单位'}
        open={modalOpen}
        forceRender
        confirmLoading={submitting}
        onOk={submit}
        onCancel={() => setModalOpen(false)}
        width={900}
        destroyOnHidden
      >
        <Form form={unitForm} layout="vertical" initialValues={defaultFormValues}>
          {!editingId ? (
            <Form.Item
              name="parentId"
              label="上级单位"
              rules={[
                {
                  validator: async (_, value) => {
                    if (currentType === 'GROUP' || value) {
                      return;
                    }
                    throw new Error('请选择上级单位');
                  },
                },
              ]}
            >
              <TreeSelect
                treeData={toTreeSelectData(tableData)}
                placeholder="选择上级单位"
                treeDefaultExpandAll
                allowClear
              />
            </Form.Item>
          ) : null}
          <Form.Item
            name="name"
            label="单位名称"
            rules={[{ required: true, message: '请输入单位名称' }]}
          >
            <Input placeholder="请输入单位名称" />
          </Form.Item>
          <Form.Item name="type" label="单位类型">
            <Radio.Group>
              {!currentParentId ? <Radio value="GROUP">总公司</Radio> : null}
              {currentParentTypeValue === 'GROUP' ? <Radio value="COMPANY">公司</Radio> : null}
              {(currentParentId && currentParentTypeValue !== 'GROUP') || currentParentTypeValue === 'GROUP' ? (
                <Radio value="UNIT">部门</Radio>
              ) : null}
            </Radio.Group>
          </Form.Item>
          <Form.Item name="aliasName" label="单位别名">
            <Input placeholder="单位别名" />
          </Form.Item>
          <Form.Item name="unitcode" label="单位编码">
            <Input placeholder="单位编码" />
          </Form.Item>
          {currentType !== 'UNIT' ? (
            <>
              <Form.Item name="address" label="单位地址">
                <Input placeholder="单位详细地址" />
              </Form.Item>
              <Form.Item name="telephone" label="固定电话">
                <Input placeholder="单位固定电话" />
              </Form.Item>
              <Form.Item name="email" label="电子邮箱">
                <Input placeholder="单位电子邮箱" />
              </Form.Item>
              <Form.Item name="website" label="单位网站">
                <Input placeholder="单位网站" />
              </Form.Item>
            </>
          ) : (
            <>
              <Form.Item name="leaderName" label="部门负责人">
                <Input placeholder="负责人姓名" />
              </Form.Item>
              <Form.Item name="leaderMobile" label="负责人电话">
                <Input placeholder="负责人电话" />
              </Form.Item>
            </>
          )}
          {editingId ? (
            <>
              <Form.Item label="单位领导">
                <Select
                  mode="multiple"
                  showSearch
                  filterOption={false}
                  value={selectedLeaderIds}
                  options={leaderOptions.leaders}
                  onSearch={(value) => loadUserOptions(value, 'leaders')}
                  onChange={(value) => setSelectedLeaderIds(value)}
                  placeholder="输入姓名或用户名进行查询"
                />
              </Form.Item>
              {currentType === 'UNIT' ? (
                <>
                  <Form.Item label="上级主管领导">
                    <Select
                      mode="multiple"
                      showSearch
                      filterOption={false}
                      value={selectedHigherIds}
                      options={leaderOptions.highers}
                      onSearch={(value) => loadUserOptions(value, 'highers')}
                      onChange={(value) => setSelectedHigherIds(value)}
                      placeholder="输入姓名或用户名进行查询"
                    />
                  </Form.Item>
                  <Form.Item label="上级分管领导">
                    <Select
                      mode="multiple"
                      showSearch
                      filterOption={false}
                      value={selectedAssignerIds}
                      options={leaderOptions.assigners}
                      onSearch={(value) => loadUserOptions(value, 'assigners')}
                      onChange={(value) => setSelectedAssignerIds(value)}
                      placeholder="输入姓名或用户名进行查询"
                    />
                  </Form.Item>
                </>
              ) : null}
            </>
          ) : null}
          <Form.Item name="disabled" label="单位状态">
            <Radio.Group
              options={[
                { label: '启用', value: false },
                { label: '禁用', value: true },
              ]}
            />
          </Form.Item>
          <Form.Item name="note" label="备注">
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="单位排序"
        open={sortOpen}
        onOk={async () => {
          await sortUnits(flattenUnitIds(sortTreeData).toString());
          setSortOpen(false);
          actionRef.current?.reload();
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
            if (!info.dropToGap) return;
            setSortTreeData((prev) =>
              moveNode(
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

export default UnitPage;
