import {
  DeleteOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Descriptions,
  Drawer,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import { clearLogs, deleteLog, getLogMeta, getLogPage } from '@/services/budiot/sys/log';
import type {
  BudiotAppOption,
  SysLogRecord,
  SysLogTypeOption,
} from '@/services/budiot/typing';

type LogTableParams = {
  current?: number;
  pageSize?: number;
  appId?: string;
  type?: string;
  status?: string;
  loginname?: string;
  username?: string;
  tag?: string;
  msg?: string;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs];
};

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

const LogPage = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const latestFiltersRef = React.useRef<Record<string, unknown>>({
    appId: '',
    type: '',
    status: '',
    loginname: '',
    username: '',
    tag: '',
    msg: '',
    beginTime: '',
    endTime: '',
  });
  const latestSortRef = React.useRef({
    pageOrderName: 'createdAt',
    pageOrderBy: 'descending',
  });
  const [metaLoading, setMetaLoading] = React.useState(false);
  const [apps, setApps] = React.useState<BudiotAppOption[]>([]);
  const [types, setTypes] = React.useState<SysLogTypeOption[]>([]);
  const [detailRecord, setDetailRecord] = React.useState<SysLogRecord>();

  React.useEffect(() => {
    const initialize = async () => {
      setMetaLoading(true);
      try {
        const response = await getLogMeta();
        setApps(response.data.apps || []);
        setTypes(response.data.types || []);
      } finally {
        setMetaLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const columns = React.useMemo<ProColumns<SysLogRecord>[]>(
    () => [
      {
        title: '日志类型',
        dataIndex: 'type',
        valueType: 'select',
        initialValue: '',
        fieldProps: {
          loading: metaLoading,
          options: [
            { label: '全部日志', value: '' },
            ...types.map((item) => ({ label: item.text, value: item.value })),
          ],
          onChange: () => actionRef.current?.reload(),
        },
        width: 120,
        render: (_, record) => types.find((item) => item.value === record.type)?.text || record.type || '-',
      },
      {
        title: '应用',
        dataIndex: 'appId',
        hideInTable: true,
        valueType: 'select',
        fieldProps: {
          loading: metaLoading,
          allowClear: true,
          options: apps.map((item) => ({ label: item.name, value: item.id })),
          onChange: () => actionRef.current?.reload(),
        },
      },
      {
        title: '操作状态',
        dataIndex: 'status',
        hideInTable: true,
        valueType: 'select',
        initialValue: '',
        fieldProps: {
          options: [
            { label: '全部', value: '' },
            { label: '成功', value: 'success' },
            { label: '失败', value: 'exception' },
          ],
        },
      },
      {
        title: '用户姓名',
        dataIndex: 'username',
        hideInTable: true,
      },
      {
        title: '用户名',
        dataIndex: 'loginname',
        hideInTable: true,
      },
      {
        title: '功能模块',
        dataIndex: 'tag',
        width: 120,
        ellipsis: true,
      },
      {
        title: '日志内容',
        dataIndex: 'msg',
        ellipsis: true,
      },
      {
        title: '操作时间',
        dataIndex: 'dateRange',
        hideInTable: true,
        valueType: 'dateTimeRange',
        search: {
          transform: (value) => ({
            beginTime: value?.[0]?.valueOf?.() || '',
            endTime: value?.[1]?.valueOf?.() || '',
          }),
        },
      },
      {
        title: '请求路径',
        dataIndex: 'url',
        search: false,
        width: 220,
        ellipsis: true,
      },
      {
        title: '操作状态',
        dataIndex: 'exception',
        search: false,
        width: 100,
        align: 'center',
        render: (_, record) =>
          record.exception ? <Tag color="error">失败</Tag> : <Tag color="success">成功</Tag>,
      },
      {
        title: '操作人',
        dataIndex: 'loginname',
        search: false,
        width: 180,
        render: (_, record) =>
          record.loginname ? `${record.username || ''}(${record.loginname})` : '-',
      },
      {
        title: '操作时间',
        dataIndex: 'createdAt',
        search: false,
        width: 180,
        sorter: true,
        render: (_, record) =>
          record.createdAt ? dayjs(record.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        title: 'IP',
        dataIndex: 'ip',
        search: false,
        width: 140,
      },
      {
        title: '执行耗时',
        dataIndex: 'executeTime',
        search: false,
        width: 100,
        align: 'center',
        render: (_, record) =>
          typeof record.executeTime === 'number' ? `${record.executeTime} ms` : '-',
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 180,
        render: (_, record) => [
          <Button
            key="detail"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => setDetailRecord(record)}
          >
            详情
          </Button>,
          <Button
            key="delete"
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={!access.hasPermission('sys.manage.log.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认删除日志',
                content: '确定删除该日志记录吗？',
                onOk: async () => {
                  await deleteLog(record.id, record.createdAt);
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
    [access, apps, metaLoading, modal, types],
  );

  return (
    <PageContainer title="系统日志">
      <PlatformProTable<SysLogRecord, LogTableParams>
        persistenceKey="platform-sys-log-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="日志列表"
        dateFormatter={false}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="clear"
            danger
            disabled={!access.hasPermission('sys.manage.log.delete')}
            onClick={() => {
              modal.confirm({
                title: '确认清空日志',
                content: '确定清空当前筛选条件下的日志记录吗？未筛选时将清空全部日志。',
                onOk: async () => {
                  await clearLogs({
                    ...latestFiltersRef.current,
                    pageNo: 1,
                    pageSize: actionRef.current?.pageInfo?.pageSize || 10,
                    totalCount: 0,
                    ...latestSortRef.current,
                  });
                  actionRef.current?.reload();
                },
              });
            }}
          >
            清空日志
          </Button>,
        ]}
        request={async (params, sort) => {
          const sortParams = getSortParams(sort, 'createdAt');
          latestFiltersRef.current = {
            appId: params.appId || '',
            type: params.type || '',
            status: params.status || '',
            loginname: params.loginname || '',
            username: params.username || '',
            tag: params.tag || '',
            msg: params.msg || '',
            beginTime: (params as Record<string, any>).beginTime || '',
            endTime: (params as Record<string, any>).endTime || '',
          };
          latestSortRef.current = sortParams;
          const response = await getLogPage({
            ...latestFiltersRef.current,
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

      <Drawer
        title="日志详情"
        open={Boolean(detailRecord)}
        size="large"
        onClose={() => setDetailRecord(undefined)}
      >
        {detailRecord ? (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="操作人">
              {detailRecord.loginname
                ? `${detailRecord.username || ''}(${detailRecord.loginname})`
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="操作时间">
              {detailRecord.createdAt
                ? dayjs(detailRecord.createdAt).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="操作IP">{detailRecord.ip || '-'}</Descriptions.Item>
            <Descriptions.Item label="请求路径">{detailRecord.url || '-'}</Descriptions.Item>
            <Descriptions.Item label="执行方法">{detailRecord.method || '-'}</Descriptions.Item>
            <Descriptions.Item label="操作系统">{detailRecord.os || '-'}</Descriptions.Item>
            <Descriptions.Item label="浏览器">{detailRecord.browser || '-'}</Descriptions.Item>
            <Descriptions.Item label="请求参数">
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                {detailRecord.params || '-'}
              </Typography.Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="响应结果">
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                {detailRecord.result || '-'}
              </Typography.Paragraph>
            </Descriptions.Item>
            {detailRecord.exception ? (
              <Descriptions.Item label="异常信息">
                <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                  {detailRecord.exception}
                </Typography.Paragraph>
              </Descriptions.Item>
            ) : null}
          </Descriptions>
        ) : null}
      </Drawer>
    </PageContainer>
  );
};

export default LogPage;
