import { PauseCircleOutlined, PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProCard } from '@ant-design/pro-components';
import { Button, Empty, Image, List, Pagination, Select, Space, Spin, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import { getVideoInspectionPage } from '@/services/budiot/video/inspection';
import type {
  VideoCameraRecord,
  VideoInspectionRecord,
} from '@/services/budiot/video/typing';

const refreshIntervalOptions = [
  { label: '手动刷新', value: 0 },
  { label: '15 秒', value: 15 },
  { label: '30 秒', value: 30 },
  { label: '60 秒', value: 60 },
];

const normalizeAssetUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`;
};

const InspectionPage: React.FC = () => {
  const [loading, setLoading] = React.useState(false);
  const [records, setRecords] = React.useState<VideoInspectionRecord[]>([]);
  const [intervalSeconds, setIntervalSeconds] = React.useState<number>(30);
  const [paused, setPaused] = React.useState(false);
  const [pageNo, setPageNo] = React.useState(1);
  const [pageSize, setPageSize] = React.useState(12);
  const [total, setTotal] = React.useState(0);

  const loadInspection = React.useCallback(
    async (nextPageNo = pageNo, nextPageSize = pageSize) => {
      setLoading(true);
      try {
        const response = await getVideoInspectionPage({
          pageNo: nextPageNo,
          pageSize: nextPageSize,
          pageOrderName: 'name',
          pageOrderBy: 'ascending',
        });
        setRecords(response.data?.list || []);
        setTotal(response.data?.totalCount || 0);
      } finally {
        setLoading(false);
      }
    },
    [pageNo, pageSize],
  );

  const refreshAll = React.useCallback(async () => {
    await loadInspection(pageNo, pageSize);
  }, [loadInspection, pageNo, pageSize]);

  React.useEffect(() => {
    void loadInspection(pageNo, pageSize);
  }, [loadInspection, pageNo, pageSize]);

  React.useEffect(() => {
    if (intervalSeconds <= 0 || paused) {
      return undefined;
    }
    const timer = window.setInterval(() => {
      void refreshAll();
    }, intervalSeconds * 1000);
    return () => window.clearInterval(timer);
  }, [intervalSeconds, paused, refreshAll]);

  const handleToggleInspect = React.useCallback(() => {
    if (paused) {
      setPaused(false);
      if (intervalSeconds <= 0) {
        setIntervalSeconds(30);
      }
      return;
    }
    setPaused(true);
  }, [intervalSeconds, paused]);

  return (
    <>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <ProCard>
          <Space wrap size={[12, 12]}>
            <Select
              style={{ width: 160 }}
              value={intervalSeconds}
              onChange={setIntervalSeconds}
              options={refreshIntervalOptions}
            />
            <Button
              icon={paused || intervalSeconds <= 0 ? <PlayCircleOutlined /> : <PauseCircleOutlined />}
              onClick={handleToggleInspect}
            >
              {paused || intervalSeconds <= 0 ? '开始巡检' : '暂停巡检'}
            </Button>
            <Button icon={<ReloadOutlined />} onClick={() => void refreshAll()}>
              刷新
            </Button>
          </Space>
        </ProCard>

        <Spin spinning={loading}>
          {records.length ? (
            <>
              <Image.PreviewGroup>
                <List
                  grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 4, xxl: 4 }}
                  dataSource={records}
                  renderItem={(item) => (
                    <List.Item>
                      {(() => {
                        const camera = item.camera as VideoCameraRecord;
                        const snapshot = item.snapshot;
                        const snapshotUrl = normalizeAssetUrl(snapshot?.thumbnailUrl || snapshot?.fileUrl);
                        const address = camera.address || camera.host || camera.name || camera.cameraCode;
                        const snapshotTime = snapshot?.bizTime ? dayjs(snapshot.bizTime).format('YYYY-MM-DD HH:mm:ss') : '暂无抓拍';
                        return (
                      <div
                        style={{
                          border: '1px solid #f0f0f0',
                          borderRadius: 8,
                          overflow: 'hidden',
                          background: '#fff',
                        }}
                      >
                        <div style={{ position: 'relative', width: '100%', height: 220, background: '#f5f5f5' }}>
                          {snapshotUrl ? (
                            <Image src={snapshotUrl} alt={camera.name} style={{ width: '100%', height: 220, objectFit: 'cover' }} />
                          ) : (
                            <div
                              style={{
                                width: '100%',
                                height: 220,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexDirection: 'column',
                                gap: 10,
                                background: 'linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%)',
                                color: '#8c8c8c',
                              }}
                            >
                              <div
                                style={{
                                  width: 84,
                                  height: 84,
                                  borderRadius: '50%',
                                  background: '#dfe7f1',
                                  display: 'flex',
                                  alignItems: 'center',
                                  justifyContent: 'center',
                                  fontSize: 28,
                                  fontWeight: 600,
                                  color: '#9aa8b6',
                                }}
                              >
                                图
                              </div>
                              <Typography.Text type="secondary">暂无抓拍</Typography.Text>
                            </div>
                          )}
                          <div
                            style={{
                              position: 'absolute',
                              left: 10,
                              right: 10,
                              bottom: 10,
                              padding: '6px 8px',
                              borderRadius: 6,
                              background: 'rgba(0, 0, 0, 0.55)',
                              backdropFilter: 'blur(4px)',
                            }}
                          >
                            <Typography.Text
                              ellipsis={{ tooltip: address }}
                              style={{ display: 'block', color: '#fff', fontSize: 12, lineHeight: 1.4 }}
                            >
                              {address}
                            </Typography.Text>
                            <Typography.Text style={{ display: 'block', color: 'rgba(255,255,255,0.88)', fontSize: 12, lineHeight: 1.4 }}>
                              {snapshotTime}
                            </Typography.Text>
                          </div>
                        </div>
                        <div style={{ padding: 12 }}>
                          <Space wrap size={[8, 8]}>
                            <Tag color={camera.online ? 'success' : 'default'}>{camera.online ? '在线' : '离线'}</Tag>
                            <Tag>{camera.name}</Tag>
                          </Space>
                        </div>
                      </div>
                        );
                      })()}
                    </List.Item>
                  )}
                />
              </Image.PreviewGroup>

              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Pagination
                  current={pageNo}
                  pageSize={pageSize}
                  total={total}
                  showSizeChanger
                  showTotal={(value) => `共 ${value} 路摄像头`}
                  onChange={(nextPageNo, nextPageSize) => {
                    setPageNo(nextPageNo);
                    setPageSize(nextPageSize);
                  }}
                />
              </div>
            </>
          ) : (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无巡检数据" />
          )}
        </Spin>
      </Space>
    </>
  );
};

export default InspectionPage;
