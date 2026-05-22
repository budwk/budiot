import { DeleteOutlined, EyeOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { App, Button, Descriptions, Drawer, Form, Image, Input, Modal, Select, Space, Tag, Typography } from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  getVideoAlarmDetail,
  getVideoAlarmPage,
  handleVideoAlarm,
} from '@/services/budiot/video/alarm';
import { getVideoMediaFileDetail } from '@/services/budiot/video/media-file';
import type { VideoAlarmRecord, VideoMediaFileRecord } from '@/services/budiot/video/typing';

const normalizeAssetUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`;
};

const AlarmPage: React.FC = () => {
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [detail, setDetail] = React.useState<VideoAlarmRecord>();
  const [imageDetail, setImageDetail] = React.useState<VideoMediaFileRecord>();
  const [recordDetail, setRecordDetail] = React.useState<VideoMediaFileRecord>();
  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const [handleOpen, setHandleOpen] = React.useState(false);
  const [currentId, setCurrentId] = React.useState<string>();
  const [form] = Form.useForm();

  const columns = React.useMemo<ProColumns<VideoAlarmRecord>[]>(
    () => [
      { title: '事件类型', dataIndex: 'eventType' },
      { title: '事件内容', dataIndex: 'eventContent', search: false },
      {
        title: '处理状态',
        dataIndex: 'handleStatus',
        render: (_, record) => <Tag>{record.handleStatus}</Tag>,
      },
      {
        title: '告警时间',
        dataIndex: 'alarmTime',
        search: false,
        render: (_, record) =>
          record.alarmTime ? dayjs(record.alarmTime).format('YYYY-MM-DD HH:mm:ss') : '-',
      },
      {
        title: '操作',
        valueType: 'option',
        render: (_, record) => (
          <TableRowActions
            actions={[
              {
                key: 'detail',
                label: '详情',
                icon: <EyeOutlined />,
                onClick: async () => {
                  const response = await getVideoAlarmDetail(record.id);
                  setDetail(response.data);
                  const [imageResponse, recordResponse] = await Promise.all([
                    response.data?.imageFileId
                      ? getVideoMediaFileDetail(response.data.imageFileId).catch(() => undefined)
                      : Promise.resolve(undefined),
                    response.data?.recordFileId
                      ? getVideoMediaFileDetail(response.data.recordFileId).catch(() => undefined)
                      : Promise.resolve(undefined),
                  ]);
                  setImageDetail(imageResponse?.data);
                  setRecordDetail(recordResponse?.data);
                  setDrawerOpen(true);
                },
              },
              {
                key: 'handle',
                label: '处理',
                icon: <DeleteOutlined />,
                onClick: () => {
                  setCurrentId(record.id);
                  form.setFieldsValue({ handleStatus: 'IGNORED', assigneeId: '', taskContent: '', reply: '' });
                  setHandleOpen(true);
                },
              },
            ]}
          />
        ),
      },
    ],
    [form],
  );

  return (
    <PageContainer>
      <PlatformProTable<VideoAlarmRecord, Record<string, any>>
        actionRef={actionRef}
        persistenceKey="video-alarm-table"
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const response = await getVideoAlarmPage({
            eventType: params.eventType,
            handleStatus: params.handleStatus,
            pageNo: params.current,
            pageSize: params.pageSize,
          });
          return {
            data: response.data?.list || [],
            total: response.data?.totalCount || 0,
            success: true,
          };
        }}
      />

      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} title="告警详情" width={640}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="事件类型">{detail?.eventType || '-'}</Descriptions.Item>
          <Descriptions.Item label="事件内容">{detail?.eventContent || '-'}</Descriptions.Item>
          <Descriptions.Item label="告警时间">
            {detail?.alarmTime ? dayjs(detail.alarmTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="图片文件">
            {imageDetail ? (
              <Space direction="vertical" size={8}>
                {imageDetail.fileUrl ? (
                  <Image
                    src={normalizeAssetUrl(imageDetail.thumbnailUrl || imageDetail.fileUrl)}
                    alt={imageDetail.fileName}
                    style={{ width: 220, borderRadius: 8 }}
                  />
                ) : null}
                <Typography.Text>{imageDetail.fileName}</Typography.Text>
                {imageDetail.fileUrl ? (
                  <a href={normalizeAssetUrl(imageDetail.fileUrl)} target="_blank" rel="noreferrer">
                    查看大图
                  </a>
                ) : (
                  <Typography.Text type="secondary">文件地址未生成</Typography.Text>
                )}
              </Space>
            ) : (
              detail?.imageFileId || '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="录像文件">
            {recordDetail ? (
              <Space direction="vertical" size={8}>
                <Typography.Text>{recordDetail.fileName}</Typography.Text>
                {recordDetail.fileUrl ? (
                  <a href={normalizeAssetUrl(recordDetail.fileUrl)} target="_blank" rel="noreferrer">
                    播放/下载录像
                  </a>
                ) : (
                  <Typography.Text type="secondary">文件地址未生成</Typography.Text>
                )}
              </Space>
            ) : (
              detail?.recordFileId || '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="处理状态">{detail?.handleStatus || '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Modal
        title="处理告警"
        open={handleOpen}
        onCancel={() => setHandleOpen(false)}
        onOk={async () => {
          const values = await form.validateFields();
          if (!currentId) return;
          await handleVideoAlarm(currentId, values);
          setHandleOpen(false);
          actionRef.current?.reload();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="handleStatus" label="处理方式" rules={[{ required: true }]}>
            <Select
              options={[
                { label: '忽略', value: 'IGNORED' },
                { label: '转工单', value: 'TRANSFERRED' },
              ]}
            />
          </Form.Item>
          <Form.Item name="assigneeId" label="处理人ID">
            <Input />
          </Form.Item>
          <Form.Item name="taskContent" label="工单内容">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="reply" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default AlarmPage;
