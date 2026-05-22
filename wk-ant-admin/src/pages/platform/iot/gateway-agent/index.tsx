import { PageContainer, StatisticCard } from '@ant-design/pro-components';
import { Tag, Table } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import {
  getIotGatewayAgentPage,
  getIotGatewayAgentSummary,
} from '@/services/budiot/iot/gateway-agent';
import type {
  IotGatewayAgentGateway,
  IotGatewayAgentRecord,
  IotGatewayAgentSummary,
} from '@/services/budiot/iot/typing';
import {
  formatDateTime,
  runtimeStatusColorMap,
} from '../shared';

const GatewayAgentPage: React.FC = () => {
  const [summary, setSummary] = React.useState<IotGatewayAgentSummary>({
    agentCount: 0,
    claimedGatewayCount: 0,
    unclaimedGatewayCount: 0,
    totalGatewayCount: 0,
  });
  const [summaryLoading, setSummaryLoading] = React.useState(false);

  React.useEffect(() => {
    const initialize = async () => {
      setSummaryLoading(true);
      try {
        const response = await getIotGatewayAgentSummary();
        setSummary(response.data);
      } finally {
        setSummaryLoading(false);
      }
    };
    initialize().catch(() => undefined);
  }, []);

  return (
    <PageContainer title="网关代理" loading={summaryLoading}>
      <StatisticCard.Group direction="row">
        <StatisticCard statistic={{ title: '代理节点', value: summary.agentCount }} />
        <StatisticCard statistic={{ title: '已认领网关', value: summary.claimedGatewayCount }} />
        <StatisticCard statistic={{ title: '未认领网关', value: summary.unclaimedGatewayCount }} />
        <StatisticCard statistic={{ title: '网关总数', value: summary.totalGatewayCount }} />
      </StatisticCard.Group>

      <PlatformProTable<IotGatewayAgentRecord, { current?: number; pageSize?: number; keyword?: string; status?: string }>
        persistenceKey="platform-iot-gateway-agent-table"
        rowKey="agentId"
        headerTitle="代理列表"
        pagination={false}
        columns={[
          { title: 'Agent ID', dataIndex: 'agentId', width: 220 },
          { title: '主机', dataIndex: 'host', width: 180 },
          {
            title: '状态',
            dataIndex: 'status',
            width: 120,
            render: (_, record) => (
              <Tag color={runtimeStatusColorMap[String(record.status || '')] || 'default'}>
                {record.status || '-'}
              </Tag>
            ),
          },
          {
            title: '最后心跳',
            dataIndex: 'lastSeenAt',
            width: 180,
            render: (_, record) => formatDateTime(record.lastSeenAt),
          },
          {
            title: '已认领网关数',
            dataIndex: 'claimedGatewayCount',
            search: false,
            width: 140,
          },
        ]}
        expandable={{
          expandedRowRender: (record) => (
            <Table<IotGatewayAgentGateway>
              rowKey={(item) => item.gatewayId || item.name}
              size="small"
              pagination={false}
              dataSource={record.gateways || []}
              columns={[
                { title: '网关名称', dataIndex: 'name' },
                { title: '节点ID', dataIndex: 'nodeId', width: 180 },
                {
                  title: '状态',
                  dataIndex: 'status',
                  width: 120,
                  render: (_, item) => (
                    <Tag color={runtimeStatusColorMap[String(item.status || '')] || 'default'}>
                      {item.status || '-'}
                    </Tag>
                  ),
                },
                {
                  title: '连接信息',
                  render: (_, item) =>
                    [item.host, item.port, item.path].filter(Boolean).join(' ') ||
                    [item.remoteHost, item.remotePort].filter(Boolean).join(':') ||
                    '-',
                },
                {
                  title: '最后心跳',
                  width: 180,
                  render: (_, item) => formatDateTime(item.lastSeenAt),
                },
                {
                  title: '认领状态',
                  width: 100,
                  render: (_, item) =>
                    item.claimed ? <Tag color="success">已认领</Tag> : <Tag>未认领</Tag>,
                },
              ]}
            />
          ),
        }}
        request={async (params) => {
          const response = await getIotGatewayAgentPage({
            keyword: params.keyword || '',
            status: params.status || '',
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
          });
          const list = Array.isArray(response.data) ? response.data : response.data.list || [];
          const total = Array.isArray(response.data)
            ? response.data.length
            : response.data.totalCount || 0;
          return {
            data: list,
            total,
            success: true,
          };
        }}
      />
    </PageContainer>
  );
};

export default GatewayAgentPage;
