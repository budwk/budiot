import { DeleteOutlined, UploadOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Alert, App, Button, Card, Image, Input, List, Space, Tag, Typography, Upload } from 'antd';
import * as React from 'react';
import { previewManagedFile } from '@/services/budiot/file';
import { uploadFile } from '@/utils/file';

type VideoPlaceholderOptions = {
  title: string;
  description: string;
  highlights: string[];
};

export const buildVideoPlaceholderPage = ({
  title,
  description,
  highlights,
}: VideoPlaceholderOptions): React.FC => {
  const VideoPlaceholderPage: React.FC = () => (
    <PageContainer
      header={{
        title,
      }}
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Alert
          showIcon
          type="info"
          message="wk-video 第一阶段骨架已接入，当前页面用于承接后续功能开发。"
          description={description}
        />
        <Card title="本阶段已规划能力">
          <List
            dataSource={highlights}
            renderItem={(item) => (
              <List.Item>
                <Tag color="processing">规划中</Tag>
                <span>{item}</span>
              </List.Item>
            )}
          />
        </Card>
      </Space>
    </PageContainer>
  );

  return VideoPlaceholderPage;
};

type VideoCoverUploadFieldProps = {
  value?: string;
  onChange?: (value?: string) => void;
  placeholder?: string;
  uploadParams?: Record<string, string | number | boolean | undefined>;
};

type UploadResultData = {
  id?: string;
  url?: string;
};

export const VideoCoverUploadField: React.FC<VideoCoverUploadFieldProps> = ({
  value,
  onChange,
  placeholder = '请上传封面',
  uploadParams,
}) => {
  const { message } = App.useApp();
  const [uploading, setUploading] = React.useState(false);
  const [previewUrl, setPreviewUrl] = React.useState<string>();
  const previewUrlRef = React.useRef<string | undefined>(undefined);

  const replacePreviewUrl = React.useCallback((next?: string) => {
    if (previewUrlRef.current?.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrlRef.current);
    }
    previewUrlRef.current = next;
    setPreviewUrl(next);
  }, []);

  React.useEffect(
    () => () => {
      if (previewUrlRef.current?.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrlRef.current);
      }
    },
    [],
  );

  React.useEffect(() => {
    let cancelled = false;
    if (!value) {
      replacePreviewUrl(undefined);
      return undefined;
    }
    previewManagedFile(value)
      .then((blob) => {
        const next = URL.createObjectURL(blob);
        if (cancelled) {
          URL.revokeObjectURL(next);
          return;
        }
        replacePreviewUrl(next);
      })
      .catch(() => {
        if (!cancelled) {
          replacePreviewUrl(undefined);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [replacePreviewUrl, value]);

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Upload
        accept=".png,.jpg,.jpeg,.gif,.bmp,.svg,.webp"
        showUploadList={false}
        beforeUpload={async (file) => {
          setUploading(true);
          try {
            const formData = new FormData();
            formData.append('Filedata', file);
            const response = await uploadFile(formData, { type: 'image', params: uploadParams });
            const data = (response.data || {}) as UploadResultData;
            if (!data.id) {
              throw new Error('封面上传未返回文件ID');
            }
            onChange?.(data.id);
            replacePreviewUrl(data.url);
            message.success('封面已上传');
          } finally {
            setUploading(false);
          }
          return false;
        }}
      >
        {previewUrl ? (
          <div
            style={{
              position: 'relative',
              width: 180,
              height: 120,
              borderRadius: 8,
              overflow: 'hidden',
              cursor: 'pointer',
            }}
          >
            <Image
              preview={false}
              src={previewUrl}
              alt="cover"
              style={{ width: 180, height: 120, objectFit: 'cover' }}
            />
            <div
              style={{
                position: 'absolute',
                inset: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'rgba(0, 0, 0, 0.35)',
                opacity: 0,
                transition: 'opacity 0.2s ease',
              }}
              className="video-cover-upload-overlay"
            >
              <Button
                danger
                type="primary"
                shape="circle"
                icon={<DeleteOutlined />}
                onClick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  onChange?.(undefined);
                  replacePreviewUrl(undefined);
                }}
              />
            </div>
          </div>
        ) : (
          <Button icon={<UploadOutlined />} loading={uploading}>
            {placeholder}
          </Button>
        )}
      </Upload>
      <style>{`
        .video-cover-upload-overlay:hover,
        .video-cover-upload-overlay:focus-within,
        .video-cover-upload-overlay:active {
          opacity: 1 !important;
        }
      `}</style>
      {!previewUrl ? <Typography.Text type="secondary">未设置封面</Typography.Text> : null}
    </Space>
  );
};

export const useManagedFilePreviewUrl = (fileId?: string) => {
  const previewUrlRef = React.useRef<string | undefined>(undefined);
  const [previewUrl, setPreviewUrl] = React.useState<string>();

  React.useEffect(
    () => () => {
      if (previewUrlRef.current?.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrlRef.current);
      }
    },
    [],
  );

  React.useEffect(() => {
    let cancelled = false;

    const replaceUrl = (next?: string) => {
      if (previewUrlRef.current?.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrlRef.current);
      }
      previewUrlRef.current = next;
      setPreviewUrl(next);
    };

    if (!fileId) {
      replaceUrl(undefined);
      return undefined;
    }

    previewManagedFile(fileId)
      .then((blob) => {
        const next = URL.createObjectURL(blob);
        if (cancelled) {
          URL.revokeObjectURL(next);
          return;
        }
        replaceUrl(next);
      })
      .catch(() => {
        if (!cancelled) {
          replaceUrl(undefined);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [fileId]);

  return previewUrl;
};

type ManagedFilePreviewProps = {
  fileId?: string;
  alt?: string;
  style?: React.CSSProperties;
  preview?: boolean;
};

type VideoPackageUploadFieldProps = {
  value?: { fileId?: string; fileName?: string };
  onChange?: (value?: { fileId?: string; fileName?: string }) => void;
  placeholder?: string;
};

export const VideoPackageUploadField: React.FC<VideoPackageUploadFieldProps> = ({
  value,
  onChange,
  placeholder = '请上传导出训练文件',
}) => {
  const { message } = App.useApp();
  const [uploading, setUploading] = React.useState(false);

  return (
    <Space direction="vertical" size={8} style={{ width: '100%' }}>
      <Upload
        accept=".zip,.onnx"
        showUploadList={false}
        beforeUpload={async (file) => {
          setUploading(true);
          try {
            const formData = new FormData();
            formData.append('Filedata', file);
            const response = await uploadFile(formData, { type: 'file' });
            const data = (response.data || {}) as UploadResultData;
            if (!data.id) {
              throw new Error('训练文件上传未返回文件ID');
            }
            onChange?.({
              fileId: data.id,
              fileName: file.name,
            });
            message.success('训练文件已上传');
          } finally {
            setUploading(false);
          }
          return false;
        }}
      >
        <Button icon={<UploadOutlined />} loading={uploading}>
          {placeholder}
        </Button>
      </Upload>
      {value?.fileName ? (
        <Space size={8}>
          <Typography.Text>{value.fileName}</Typography.Text>
          <Button
            type="link"
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={() => onChange?.(undefined)}
          >
            移除
          </Button>
        </Space>
      ) : (
        <Typography.Text type="secondary">支持 .onnx 或 OpenVINO 导出 zip</Typography.Text>
      )}
    </Space>
  );
};

export const ManagedFileImagePreview: React.FC<ManagedFilePreviewProps> = ({ fileId, alt, style, preview = true }) => {
  const previewUrl = useManagedFilePreviewUrl(fileId);

  if (!fileId || !previewUrl) {
    return <Typography.Text type="secondary">暂无预览</Typography.Text>;
  }

  return <Image src={previewUrl} alt={alt} style={style} preview={preview} />;
};

export const ManagedFileVideoPreview: React.FC<ManagedFilePreviewProps> = ({ fileId, style }) => {
  const previewUrl = useManagedFilePreviewUrl(fileId);

  if (!fileId || !previewUrl) {
    return <Typography.Text type="secondary">暂无预览</Typography.Text>;
  }

  return <video src={previewUrl} controls style={style} />;
};
