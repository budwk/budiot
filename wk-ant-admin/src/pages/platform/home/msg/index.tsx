import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  InboxOutlined,
  MailOutlined,
} from '@ant-design/icons';
import { PageContainer, ProCard } from '@ant-design/pro-components';
import { history, useLocation, useModel } from '@umijs/max';
import { Alert, Badge, Button, Descriptions, Empty, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import * as React from 'react';
import {
  doReadAllHomeMsg,
  doReadMoreHomeMsg,
  doReadOneHomeMsg,
  getHomeMsgData,
  getHomeMsgInfo,
  getHomeMsgList,
} from '@/services/budiot/home/msg';
import type {
  HomeMsgDetail,
  HomeMsgListRecord,
  SysMsgTypeOption,
} from '@/services/budiot/typing';
import { emitHomeNoticeRefresh } from '@/utils/homeNotice';

const filterOptions = [
  {
    label: '全部消息',
    value: 'all',
    description: '查看全部站内通知',
    icon: <InboxOutlined />,
  },
  {
    label: '未读消息',
    value: 'unread',
    description: '优先处理待查看内容',
    icon: <MailOutlined />,
  },
  {
    label: '已读消息',
    value: 'read',
    description: '回看已确认的消息',
    icon: <CheckCircleOutlined />,
  },
];

const toPlainText = (value?: string) => {
  if (!value) {
    return '-';
  }
  if (typeof window === 'undefined') {
    return value;
  }
  const parser = new window.DOMParser();
  return parser.parseFromString(value, 'text/html').body.textContent || '-';
};

const MessageHomePage: React.FC = () => {
  const location = useLocation();
  const { initialState } = useModel('@@initialState');
  const searchParams = React.useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );
  const currentFilter = searchParams.get('status') || 'all';
  const currentId = searchParams.get('id');
  const currentType = searchParams.get('type') || '';
  const pageNo = Number(searchParams.get('pageNo') || 1);
  const pageSize = Number(searchParams.get('pageSize') || 10);
  const [types, setTypes] = React.useState<SysMsgTypeOption[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [detailLoading, setDetailLoading] = React.useState(false);
  const [list, setList] = React.useState<HomeMsgListRecord[]>([]);
  const [total, setTotal] = React.useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = React.useState<React.Key[]>([]);
  const [detail, setDetail] = React.useState<HomeMsgDetail>();

  const updateQuery = React.useCallback(
    (patch: Record<string, string | number | undefined>) => {
      const nextQuery = new URLSearchParams(location.search);
      Object.entries(patch).forEach(([key, value]) => {
        if (value === undefined || value === '') {
          nextQuery.delete(key);
          return;
        }
        nextQuery.set(key, String(value));
      });
      history.replace(`/platform/home/msg?${nextQuery.toString()}`);
    },
    [location.search],
  );

  const refreshList = React.useCallback(async () => {
    setLoading(true);
    try {
      const response = await getHomeMsgList({
        status: currentFilter,
        type: currentType,
        pageNo,
        pageSize,
        pageOrderName: 'sendAt',
        pageOrderBy: 'descending',
      });
      setList(response.data.list || []);
      setTotal(response.data.totalCount || 0);
    } finally {
      setLoading(false);
    }
  }, [currentFilter, currentType, pageNo, pageSize]);

  const refreshInitData = React.useCallback(async () => {
    const response = await getHomeMsgData();
    setTypes(response.data.types || []);
  }, []);

  const refreshDetail = React.useCallback(async () => {
    if (!currentId) {
      setDetail(undefined);
      return;
    }
    setDetailLoading(true);
    try {
      const response = await getHomeMsgInfo(currentId);
      setDetail(response.data);
      if (!initialState?.platformInfo?.AppDemoEnv) {
        await doReadOneHomeMsg(currentId);
        emitHomeNoticeRefresh();
      }
    } finally {
      setDetailLoading(false);
    }
  }, [currentId, initialState?.platformInfo?.AppDemoEnv]);

  React.useEffect(() => {
    void refreshInitData();
  }, [refreshInitData]);

  React.useEffect(() => {
    if (currentId) {
      void refreshDetail();
      return;
    }
    void refreshList();
  }, [currentId, refreshDetail, refreshList]);

  const handleReadMore = async () => {
    await doReadMoreHomeMsg(selectedRowKeys.join(','));
    setSelectedRowKeys([]);
    emitHomeNoticeRefresh();
    await refreshList();
  };

  const handleReadAll = async () => {
    await doReadAllHomeMsg();
    setSelectedRowKeys([]);
    emitHomeNoticeRefresh();
    await refreshList();
  };

  const columns = React.useMemo<ColumnsType<HomeMsgListRecord>>(
    () => [
      {
        title: '消息标题',
        dataIndex: 'title',
        render: (_, record) => (
          <Button
            type="link"
            style={{
              paddingInline: 0,
              fontWeight: record.status === 0 ? 600 : 400,
            }}
            onClick={() => updateQuery({ id: record.msgid || record.id })}
          >
            <Space size={8}>
              {record.status === 0 ? <Badge color="#ff4d4f" /> : null}
              <span>{record.title}</span>
            </Space>
          </Button>
        ),
      },
      {
        title: '状态',
        dataIndex: 'status',
        width: 100,
        render: (value) =>
          value === 0 ? <Tag color="error">未读</Tag> : <Tag>已读</Tag>,
      },
      {
        title: '发送时间',
        dataIndex: 'sendat',
        width: 200,
        render: (value) =>
          value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        title: '消息类型',
        dataIndex: 'type',
        width: 160,
        render: (value) =>
          types.find((item) => item.value === value)?.text || value || '-',
      },
    ],
    [types, updateQuery],
  );

  if (currentId) {
    return (
      <PageContainer
        title="消息详情"
        extra={[
          <Button
            key="back"
            icon={<ArrowLeftOutlined />}
            onClick={() => {
              const nextQuery = new URLSearchParams(location.search);
              nextQuery.delete('id');
              history.replace(`/platform/home/msg?${nextQuery.toString()}`);
            }}
          >
            返回消息中心
          </Button>,
        ]}
      >
        <ProCard loading={detailLoading}>
          {detail ? (
            <Descriptions column={1} bordered>
              <Descriptions.Item label="消息标题">{detail.title}</Descriptions.Item>
              <Descriptions.Item label="发送时间">
                {detail.sendAt
                  ? dayjs(detail.sendAt).format('YYYY-MM-DD HH:mm:ss')
                  : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="消息内容">
                <Typography.Paragraph
                  style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}
                >
                  {toPlainText(detail.note)}
                </Typography.Paragraph>
              </Descriptions.Item>
            </Descriptions>
          ) : (
            <Empty description="暂无详情内容" />
          )}
        </ProCard>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="消息中心">
      <ProCard>
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          {initialState?.platformInfo?.AppDemoEnv ? (
            <Alert
              type="info"
              showIcon
              title="演示环境不会真正写入消息已读状态。"
            />
          ) : null}
          <div className="wk-home-msg-filters">
            <div className="wk-home-msg-filter-section">
              <Typography.Text className="wk-home-msg-filter-label">
                消息状态
              </Typography.Text>
              <div className="wk-home-msg-status-grid">
                {filterOptions.map((item) => {
                  const active = currentFilter === item.value;
                  return (
                    <button
                      key={item.value}
                      type="button"
                      className={`wk-home-msg-status-card${active ? ' wk-home-msg-status-card-active' : ''}`}
                      onClick={() =>
                        updateQuery({
                          status: item.value,
                          pageNo: 1,
                        })
                      }
                    >
                      <span className="wk-home-msg-status-card-icon">{item.icon}</span>
                      <span className="wk-home-msg-status-card-body">
                        <span className="wk-home-msg-status-card-title">{item.label}</span>
                        <span className="wk-home-msg-status-card-desc">
                          {item.description}
                        </span>
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="wk-home-msg-filter-section">
              <div className="wk-home-msg-type-header">
                <Typography.Text className="wk-home-msg-filter-label">
                  消息类型
                </Typography.Text>
                {currentType ? (
                  <Button
                    type="link"
                    size="small"
                    onClick={() =>
                      updateQuery({
                        type: undefined,
                        pageNo: 1,
                      })
                    }
                  >
                    清空筛选
                  </Button>
                ) : null}
              </div>
              <Space wrap size={[8, 8]}>
                <Tag.CheckableTag
                  className="wk-home-msg-type-tag"
                  checked={currentType === ''}
                  onChange={() =>
                    updateQuery({
                      type: undefined,
                      pageNo: 1,
                    })
                  }
                >
                  全部类型
                </Tag.CheckableTag>
                {types.map((item) => (
                  <Tag.CheckableTag
                    key={item.value}
                    className="wk-home-msg-type-tag"
                    checked={currentType === item.value}
                    onChange={() =>
                      updateQuery({
                        type: item.value,
                        pageNo: 1,
                      })
                    }
                  >
                    {item.text}
                  </Tag.CheckableTag>
                ))}
              </Space>
            </div>
          </div>
          <Table<HomeMsgListRecord>
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={list}
             rowSelection={{
               selectedRowKeys,
               onChange: setSelectedRowKeys,
             }}
             rowClassName={(record) =>
               record.status === 0 ? 'wk-home-msg-row-unread' : ''
             }
             pagination={{
               current: pageNo,
               pageSize,
              total,
              showSizeChanger: true,
              onChange: (nextPage, nextSize) =>
                updateQuery({
                  pageNo: nextPage,
                  pageSize: nextSize,
                }),
            }}
            title={() => (
              <Space wrap>
                <Button
                  disabled={!selectedRowKeys.length}
                  onClick={() => void handleReadMore()}
                >
                  设置已读
                </Button>
                <Button onClick={() => void handleReadAll()}>全部已读</Button>
              </Space>
            )}
          />
          {!list.length && !loading ? (
            <Typography.Text type="secondary">暂无消息数据</Typography.Text>
          ) : null}
        </Space>
      </ProCard>
    </PageContainer>
  );
};

export default MessageHomePage;
