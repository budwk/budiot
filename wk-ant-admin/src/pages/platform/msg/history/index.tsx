import { EyeOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import {
  Descriptions,
  Drawer,
  Input,
  Space,
  Tag,
  Typography,
} from 'antd';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  getMsgHistoryDetail,
  getMsgHistoryMeta,
  getMsgHistoryPage,
} from '@/services/budiot/msg/history';
import type { MsgHistoryRecord, MsgOption } from '@/services/budiot/typing';

type MsgHistoryTableParams = {
  current?: number;
  pageSize?: number;
  channelId?: string;
  status?: string;
  receiver?: string;
  title?: string;
  dateRange?: [Dayjs, Dayjs];
};

const resolveOptionValue = (value?: string | MsgOption) =>
  typeof value === 'string' ? value : value?.value || '';

const resolveOptionText = (options: MsgOption[], value?: string | MsgOption) => {
  const currentValue = resolveOptionValue(value);
  return options.find((item) => item.value === currentValue)?.text || currentValue || '-';
};

const statusColorMap: Record<string, string> = {
  SUCCESS: 'success',
  FAIL: 'error',
  WAITING: 'processing',
  PENDING: 'default',
};

const MsgHistoryPage: React.FC = () => {
  const actionRef = React.useRef<ActionType>(null);
  const [loading, setLoading] = React.useState(false);
  const [channels, setChannels] = React.useState<Array<{ id: string; name: string }>>([]);
  const [channelTypes, setChannelTypes] = React.useState<MsgOption[]>([]);
  const [providerTypes, setProviderTypes] = React.useState<MsgOption[]>([]);
  const [statuses, setStatuses] = React.useState<MsgOption[]>([]);
  const [detailOpen, setDetailOpen] = React.useState(false);
  const [detailLoading, setDetailLoading] = React.useState(false);
  const [detailRecord, setDetailRecord] = React.useState<MsgHistoryRecord>();

  React.useEffect(() => {
    const initialize = async () => {
      setLoading(true);
      try {
        const response = await getMsgHistoryMeta();
        setChannels(response.data.channels || []);
        setChannelTypes(response.data.channelTypes || []);
        setProviderTypes(response.data.providerTypes || []);
        setStatuses(response.data.statuses || []);
      } finally {
        setLoading(false);
      }
    };

    initialize().catch(() => undefined);
  }, []);

  const openDetail = async (record: MsgHistoryRecord) => {
    if (!record.createdAt) {
      return;
    }
    setDetailOpen(true);
    setDetailLoading(true);
    try {
      const response = await getMsgHistoryDetail(record.id, record.createdAt);
      setDetailRecord(response.data);
    } finally {
      setDetailLoading(false);
    }
  };

  const columns = React.useMemo<ProColumns<MsgHistoryRecord>[]>(
    () => [
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
        render: (_, record) => record.channel?.name || record.channelName || '-',
        width: 140,
      },
      {
        title: '状态',
        dataIndex: 'status',
        valueType: 'select',
        fieldProps: {
          options: statuses.map((item) => ({
            label: item.text,
            value: item.value,
          })),
          allowClear: true,
        },
        render: (_, record) => {
          const value = resolveOptionValue(record.status);
          return (
            <Tag color={statusColorMap[value] || 'default'}>
              {resolveOptionText(statuses, record.status)}
            </Tag>
          );
        },
        width: 110,
      },
      {
        title: '接收对象',
        dataIndex: 'receiver',
        width: 180,
      },
      {
        title: '标题',
        dataIndex: 'title',
        ellipsis: true,
      },
      {
        title: '渠道类型',
        dataIndex: 'channelType',
        search: false,
        render: (_, record) => resolveOptionText(channelTypes, record.channelType),
        width: 120,
      },
      {
        title: '提供商',
        dataIndex: 'providerType',
        search: false,
        render: (_, record) => resolveOptionText(providerTypes, record.providerType),
        width: 140,
      },
      {
        title: '发送时间',
        dataIndex: 'sendAt',
        search: false,
        valueType: 'dateTime',
        width: 180,
      },
      {
        title: '时间范围',
        dataIndex: 'dateRange',
        valueType: 'dateTimeRange',
        hideInTable: true,
        search: {
          transform: (value) => ({
            beginTime: value?.[0]?.valueOf?.(),
            endTime: value?.[1]?.valueOf?.(),
          }),
        },
      },
      {
        title: '操作',
        key: 'option',
        valueType: 'option',
        width: 120,
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'detail',
                label: '详情',
                icon: <EyeOutlined />,
                onClick: () => openDetail(record),
              },
            ]}
          />
        ),
      },
    ],
    [channelTypes, channels, providerTypes, statuses],
  );

  return (
    <PageContainer title="发送历史">
      <PlatformProTable<MsgHistoryRecord, MsgHistoryTableParams>
        persistenceKey="platform-msg-history-table"
        actionRef={actionRef}
        rowKey={(record) => `${record.id}-${record.createdAt || 0}`}
        loading={loading}
        headerTitle="发送记录"
        columns={columns}
        request={async (params) => {
          const response = await getMsgHistoryPage({
            channelId: params.channelId || '',
            status: params.status || '',
            receiver: params.receiver || '',
            title: params.title || '',
            beginTime: (params as Record<string, unknown>).beginTime,
            endTime: (params as Record<string, unknown>).endTime,
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            pageOrderName: 'createdAt',
            pageOrderBy: 'descending',
          });
          return {
            data: response.data.list || [],
            total: response.data.totalCount || 0,
            success: true,
          };
        }}
      />

      <Drawer
        title="发送详情"
        open={detailOpen}
        size="large"
        onClose={() => setDetailOpen(false)}
      >
        {detailLoading ? (
          <Typography.Text type="secondary">加载中...</Typography.Text>
        ) : detailRecord ? (
          <Space orientation="vertical" style={{ width: '100%' }} size={16}>
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="渠道">
                {detailRecord.channel?.name || detailRecord.channelName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={statusColorMap[resolveOptionValue(detailRecord.status)] || 'default'}>
                  {resolveOptionText(statuses, detailRecord.status)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="渠道类型">
                {resolveOptionText(channelTypes, detailRecord.channelType)}
              </Descriptions.Item>
              <Descriptions.Item label="提供商">
                {resolveOptionText(providerTypes, detailRecord.providerType)}
              </Descriptions.Item>
              <Descriptions.Item label="接收对象">
                {detailRecord.receiver || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="接收人">
                {detailRecord.receiverName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="标题" span={2}>
                {detailRecord.title || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="发送时间">
                {detailRecord.sendAt
                  ? dayjs(detailRecord.sendAt).format('YYYY-MM-DD HH:mm:ss')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="成功时间">
                {detailRecord.successAt
                  ? dayjs(detailRecord.successAt).format('YYYY-MM-DD HH:mm:ss')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="提供商返回码">
                {detailRecord.providerCode || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="提供商请求号">
                {detailRecord.providerRequestId || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="返回信息" span={2}>
                {detailRecord.providerMsg || '-'}
              </Descriptions.Item>
            </Descriptions>
            <div>
              <Typography.Title level={5}>消息内容</Typography.Title>
              <Input.TextArea value={detailRecord.content || ''} rows={8} readOnly />
            </div>
            <div>
              <Typography.Title level={5}>模板参数</Typography.Title>
              <Input.TextArea value={detailRecord.paramsJson || ''} rows={6} readOnly />
            </div>
          </Space>
        ) : null}
      </Drawer>
    </PageContainer>
  );
};

export default MsgHistoryPage;
