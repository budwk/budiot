import {
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  LinkOutlined,
  LockOutlined,
  UnlockOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import type { UploadFile } from 'antd';
import {
  App,
  Button,
  Descriptions,
  Drawer,
  Image,
  Modal,
  Tag,
  Upload,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import {
  deleteManagedFile,
  downloadManagedFile,
  getManagedFileDetail,
  getManagedFilePage,
  previewManagedFile,
  updateManagedFilePublicFlag,
  uploadManagedFile,
} from '@/services/budiot/file';
import type { ManagedFileRecord } from '@/services/budiot/typing';

type FileTableParams = {
  current?: number;
  pageSize?: number;
  fileName?: string;
  category?: string;
  createdAtRange?: [dayjs.Dayjs, dayjs.Dayjs];
};

const categoryOptions = [
  { value: 'file', label: '文件' },
  { value: 'image', label: '图片' },
  { value: 'video', label: '视频' },
];

const storageTypeOptions = [
  { value: 'LOCAL', label: '本地' },
  { value: 'FTP', label: 'FTP' },
  { value: 'FDFS', label: 'FastDFS' },
  { value: 'MINIO', label: 'MinIO' },
];

const formatFileSize = (size?: number) => {
  const value = size || 0;
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`;
};

const inferCategory = (file: File) => {
  if (file.type.startsWith('image/')) return 'image';
  if (file.type.startsWith('video/')) return 'video';
  return 'file';
};

const formatPublicUrl = (value?: string) => {
  if (!value) {
    return '-';
  }
  return value.startsWith('http://') || value.startsWith('https://')
    ? value
    : `${window.location.origin}${value}`;
};

const formatUploader = (record: ManagedFileRecord) => {
  if (record.createdByUsername && record.createdByLoginname) {
    return `${record.createdByUsername}(${record.createdByLoginname})`;
  }
  return record.createdByLoginname || record.createdByUsername || '-';
};

const FileListPage = () => {
  const access = useAccess();
  const { modal, message } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const [uploading, setUploading] = React.useState(false);
  const [uploadModalOpen, setUploadModalOpen] = React.useState(false);
  const [uploadFileList, setUploadFileList] = React.useState<UploadFile[]>([]);
  const [detailRecord, setDetailRecord] = React.useState<ManagedFileRecord>();
  const [previewUrl, setPreviewUrl] = React.useState<string>();

  React.useEffect(
    () => () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    },
    [previewUrl],
  );

  const columns = React.useMemo<ProColumns<ManagedFileRecord>[]>(
    () => [
      {
        title: '文件名称',
        dataIndex: 'fileName',
      },
      {
        title: '分类',
        dataIndex: 'category',
        valueType: 'select',
        fieldProps: {
          allowClear: true,
          options: categoryOptions,
        },
        width: 110,
        render: (_, record) => (
          <Tag>{categoryOptions.find((item) => item.value === record.category)?.label || record.category || '-'}</Tag>
        ),
      },
      {
        title: '上传时间',
        dataIndex: 'createdAtRange',
        hideInTable: true,
        valueType: 'dateTimeRange',
        search: {
          transform: (value) => ({
            beginTime: value?.[0]?.valueOf?.(),
            endTime: value?.[1]?.valueOf?.(),
          }),
        },
      },
      {
        title: '存储方式',
        dataIndex: 'storageType',
        search: false,
        width: 120,
        render: (_, record) => (
          <Tag color="success">
            {storageTypeOptions.find((item) => item.value === record.storageType)?.label ||
              record.storageType ||
              '-'}
          </Tag>
        ),
      },
      {
        title: '是否公开',
        dataIndex: 'publicFlag',
        search: false,
        width: 110,
        render: (_, record) =>
          record.publicFlag ? <Tag color="success">是</Tag> : <Tag color="default">否</Tag>,
      },
      {
        title: '上传人',
        key: 'createdByUsername',
        search: false,
        width: 180,
        render: (_, record) => formatUploader(record),
      },
      {
        title: '内容类型',
        dataIndex: 'contentType',
        search: false,
      },
      {
        title: '文件大小',
        dataIndex: 'size',
        search: false,
        width: 120,
        render: (_, record) => formatFileSize(record.size),
      },
      {
        title: '上传时间',
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
                key: 'detail',
                label: '详情',
                icon: <EyeOutlined />,
                disabled: !access.hasPermission('sys.manage.file.view'),
                onClick: async () => {
                  const response = await getManagedFileDetail(record.id);
                  setDetailRecord(response.data);
                },
              },
              {
                key: 'preview',
                label: '预览',
                icon: <EyeOutlined />,
                hidden: !record.imageFlag,
                disabled: !access.hasPermission('sys.manage.file.view'),
                onClick: async () => {
                  if (previewUrl) {
                    URL.revokeObjectURL(previewUrl);
                  }
                  const blob = await previewManagedFile(record.id);
                  setPreviewUrl(URL.createObjectURL(blob));
                },
              },
              {
                key: 'public',
                label: record.publicFlag ? '设为私有' : '设为公开',
                icon: record.publicFlag ? <LockOutlined /> : <UnlockOutlined />,
                disabled: !access.hasPermission('sys.manage.file'),
                onClick: () => {
                  modal.confirm({
                    title: record.publicFlag ? '设为私有' : '设为公开',
                    content: `确定修改文件 ${record.fileName} 的公开状态吗？`,
                    onOk: async () => {
                      await updateManagedFilePublicFlag(record.id, !record.publicFlag);
                      actionRef.current?.reload();
                    },
                  });
                },
              },
              {
                key: 'copy',
                label: '复制链接',
                icon: <LinkOutlined />,
                hidden: !record.publicFlag,
                disabled: !access.hasPermission('sys.manage.file.view'),
                onClick: async () => {
                  const publicUrl = formatPublicUrl(record.accessPath);
                  await navigator.clipboard.writeText(publicUrl === '-' ? '' : publicUrl);
                  message.success('公开链接已复制');
                },
              },
              {
                key: 'download',
                label: '下载',
                icon: <DownloadOutlined />,
                disabled: !access.hasPermission('sys.manage.file.download'),
                onClick: () =>
                  downloadManagedFile(record.id, record.fileName || `file_${record.id}`),
              },
              {
                key: 'delete',
                label: '删除',
                icon: <DeleteOutlined />,
                danger: true,
                disabled: !access.hasPermission('sys.manage.file.delete'),
                onClick: () => {
                  modal.confirm({
                    title: '确认删除文件',
                    content: `确定删除文件 ${record.fileName} 吗？`,
                    onOk: async () => {
                      await deleteManagedFile(record.id);
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
    [access, message, modal, previewUrl],
  );

  return (
    <PageContainer title="文件管理">
      <PlatformProTable<ManagedFileRecord, FileTableParams>
        persistenceKey="platform-file-list-table"
        actionRef={actionRef}
        rowKey="id"
        headerTitle="文件列表"
        dateFormatter={false}
        columns={columns}
        toolBarRender={() => [
          <Button
            key="upload"
            type="primary"
            icon={<UploadOutlined />}
            disabled={!access.hasPermission('sys.manage.file.upload')}
            onClick={() => {
              setUploadFileList([]);
              setUploadModalOpen(true);
            }}
          >
            手动上传
          </Button>,
        ]}
        request={async (params) => {
          const response = await getManagedFilePage({
            fileName: params.fileName || '',
            category: params.category || '',
            beginTime: (params as Record<string, any>).beginTime,
            endTime: (params as Record<string, any>).endTime,
            pageNo: params.current || 1,
            pageSize: params.pageSize || 10,
            totalCount: 0,
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

      <Modal
        title="手动上传文件"
        open={uploadModalOpen}
        confirmLoading={uploading}
        onOk={async () => {
          const files = uploadFileList.flatMap((item) =>
            item.originFileObj ? [item.originFileObj] : [],
          );
          if (!files.length) {
            message.warning('请选择上传文件');
            return;
          }
          setUploading(true);
          try {
            for (const file of files) {
              const formData = new FormData();
              formData.append('Filedata', file);
              formData.append('category', inferCategory(file));
              await uploadManagedFile(formData);
            }
            setUploadModalOpen(false);
            actionRef.current?.reload();
            message.success('文件上传成功');
          } finally {
            setUploading(false);
          }
        }}
        onCancel={() => setUploadModalOpen(false)}
        destroyOnHidden
      >
        <Upload.Dragger
          multiple
          beforeUpload={() => false}
          fileList={uploadFileList}
          onChange={({ fileList }) => setUploadFileList(fileList)}
        >
          <p>拖拽文件到这里，或点击选择文件</p>
          <p style={{ color: '#999' }}>支持单个或批量上传，文件将统一纳入文件管理列表。</p>
        </Upload.Dragger>
      </Modal>

      <Drawer
        title="文件详情"
        open={Boolean(detailRecord)}
        size="large"
        onClose={() => setDetailRecord(undefined)}
      >
        {detailRecord ? (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="文件名称">{detailRecord.fileName}</Descriptions.Item>
            <Descriptions.Item label="文件分类">
              {categoryOptions.find((item) => item.value === detailRecord.category)?.label ||
                detailRecord.category ||
                '-'}
            </Descriptions.Item>
            <Descriptions.Item label="文件大小">{formatFileSize(detailRecord.size)}</Descriptions.Item>
            <Descriptions.Item label="内容类型">{detailRecord.contentType || '-'}</Descriptions.Item>
            <Descriptions.Item label="存储方式">
              {storageTypeOptions.find((item) => item.value === detailRecord.storageType)?.label ||
                detailRecord.storageType ||
                '-'}
            </Descriptions.Item>
            <Descriptions.Item label="是否公开">
              {detailRecord.publicFlag ? <Tag color="success">是</Tag> : <Tag>否</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="公开链接">
              {detailRecord.publicFlag ? formatPublicUrl(detailRecord.accessPath) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="上传时间">
              {detailRecord.createdAt
                ? dayjs(detailRecord.createdAt).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="上传人登录名">
              {detailRecord.createdByLoginname || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="上传人姓名">
              {detailRecord.createdByUsername || '-'}
            </Descriptions.Item>
          </Descriptions>
        ) : null}
      </Drawer>

      <Modal
        title="图片预览"
        open={Boolean(previewUrl)}
        footer={null}
        onCancel={() => {
          if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
          }
          setPreviewUrl(undefined);
        }}
        destroyOnHidden
      >
        {previewUrl ? <Image width="100%" src={previewUrl} /> : null}
      </Modal>
    </PageContainer>
  );
};

export default FileListPage;
