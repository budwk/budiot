import { EyeOutlined, MessageOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { Descriptions, Drawer, Form, Input, Modal, Tag } from 'antd';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  getVideoWorkOrderDetail,
  getVideoWorkOrderPage,
  replyVideoWorkOrder,
} from '@/services/budiot/video/workorder';
import type { VideoWorkOrderRecord } from '@/services/budiot/video/typing';

const WorkOrderPage: React.FC = () => {
  const actionRef = React.useRef<ActionType>(null);
  const [detail, setDetail] = React.useState<VideoWorkOrderRecord>();
  const [drawerOpen, setDrawerOpen] = React.useState(false);
  const [replyOpen, setReplyOpen] = React.useState(false);
  const [currentId, setCurrentId] = React.useState<string>();
  const [form] = Form.useForm<{ reply: string }>();

  const columns = React.useMemo<ProColumns<VideoWorkOrderRecord>[]>(
    () => [
      { title: '工单编号', dataIndex: 'workOrderNo' },
      { title: '任务内容', dataIndex: 'taskContent', search: false },
      { title: '处理人ID', dataIndex: 'assigneeId' },
      {
        title: '状态',
        dataIndex: 'handleStatus',
        render: (_, record) => <Tag>{record.handleStatus}</Tag>,
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
                  const response = await getVideoWorkOrderDetail(record.id);
                  setDetail(response.data);
                  setDrawerOpen(true);
                },
              },
              {
                key: 'reply',
                label: '回复',
                icon: <MessageOutlined />,
                onClick: () => {
                  setCurrentId(record.id);
                  form.resetFields();
                  setReplyOpen(true);
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
      <PlatformProTable<VideoWorkOrderRecord, Record<string, any>>
        actionRef={actionRef}
        persistenceKey="video-workorder-table"
        rowKey="id"
        columns={columns}
        request={async (params) => {
          const response = await getVideoWorkOrderPage({
            workOrderNo: params.workOrderNo,
            assigneeId: params.assigneeId,
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

      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} title="工单详情" width={640}>
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="工单编号">{detail?.workOrderNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="处理人ID">{detail?.assigneeId || '-'}</Descriptions.Item>
          <Descriptions.Item label="任务内容">{detail?.taskContent || '-'}</Descriptions.Item>
          <Descriptions.Item label="最后回复">{detail?.lastReply || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">{detail?.handleStatus || '-'}</Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Modal
        title="回复工单"
        open={replyOpen}
        onCancel={() => setReplyOpen(false)}
        onOk={async () => {
          const values = await form.validateFields();
          if (!currentId) return;
          await replyVideoWorkOrder(currentId, values.reply);
          setReplyOpen(false);
          actionRef.current?.reload();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="reply" label="回复内容" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default WorkOrderPage;
