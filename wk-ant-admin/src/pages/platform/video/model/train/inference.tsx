import { ReloadOutlined } from '@ant-design/icons';
import { App, Button, Card, Empty, Select, Space, Typography } from 'antd';
import * as React from 'react';
import { API_BASE_URL, APP_ID, DEFAULT_LANG } from '@/constants/app';
import { getVideoCameraPage } from '@/services/budiot/video/camera';
import { getVideoModelPage } from '@/services/budiot/video/model';
import {
  createVideoInferenceTask,
  getVideoDeploymentPage,
  getVideoInferencePage,
  getVideoInferenceResult,
} from '@/services/budiot/video/train';
import type {
  VideoCameraRecord,
  VideoModelDeploymentRecord,
  VideoModelInferenceTaskRecord,
  VideoModelRecord,
} from '@/services/budiot/video/typing';
import { uploadFile } from '@/utils/file';
import { getStoredToken } from '@/utils/session';

type InferenceSourceMode = 'IMAGE_UPLOAD' | 'VIDEO_UPLOAD' | 'CAMERA';

type UploadedInferenceSource = {
  fileId?: string;
  fileName?: string;
  sourceUri?: string;
  previewUrl?: string;
  mediaType?: 'image' | 'video';
};

type InferenceBBoxDetection = {
  class?: number;
  class_name?: string;
  confidence?: number;
  bbox?: [number, number, number, number];
};

const toApiUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`;
};

const resolveInferenceArtifactUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (url.startsWith('/artifacts/')) {
    return `/api/platform/video/model/train/inference/artifact?path=${encodeURIComponent(url)}`;
  }
  return toApiUrl(url);
};

const toAbsoluteSourceUrl = (url?: string) => {
  const normalized = toApiUrl(url);
  if (!normalized) {
    return '';
  }
  if (/^https?:\/\//i.test(normalized)) {
    return normalized;
  }
  if (typeof window === 'undefined') {
    return normalized;
  }
  return new URL(normalized, window.location.origin).toString();
};

const fetchProtectedBlobUrl = async (url?: string) => {
  const normalized = toApiUrl(url);
  if (!normalized) {
    return '';
  }
  const requestUrl =
    /^https?:\/\//i.test(normalized) || normalized.startsWith(API_BASE_URL)
      ? normalized
      : `${API_BASE_URL}${normalized}`;
  const response = await fetch(requestUrl, {
    method: 'GET',
    headers: {
      Authorization: getStoredToken() || '',
      lang: DEFAULT_LANG,
      appId: APP_ID,
    },
  });
  if (!response.ok) {
    throw new Error('加载预览资源失败');
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
};

const InferenceWorkbench: React.FC = () => {
  const { message } = App.useApp();
  const inferenceImageInputRef = React.useRef<HTMLInputElement>(null);
  const inferenceVideoInputRef = React.useRef<HTMLInputElement>(null);
  const resultImageRef = React.useRef<HTMLImageElement>(null);
  const inferenceDeploymentRequestRef = React.useRef(0);
  const [modelOptions, setModelOptions] = React.useState<VideoModelRecord[]>([]);
  const [selectedModelId, setSelectedModelId] = React.useState<string>();
  const [selectedDeploymentId, setSelectedDeploymentId] = React.useState<string>();
  const [deploymentData, setDeploymentData] = React.useState<VideoModelDeploymentRecord[]>([]);
  const [historyRecords, setHistoryRecords] = React.useState<VideoModelInferenceTaskRecord[]>([]);
  const [historyId, setHistoryId] = React.useState<string>();
  const [sourceMode, setSourceMode] = React.useState<InferenceSourceMode>('IMAGE_UPLOAD');
  const [cameraOptions, setCameraOptions] = React.useState<VideoCameraRecord[]>([]);
  const [cameraId, setCameraId] = React.useState<string>();
  const [uploadedSource, setUploadedSource] = React.useState<UploadedInferenceSource>();
  const [inferenceSubmitting, setInferenceSubmitting] = React.useState(false);
  const [showInferenceOriginal, setShowInferenceOriginal] = React.useState(true);
  const [resultPayload, setResultPayload] = React.useState<Record<string, any>>();
  const [resultSummary, setResultSummary] = React.useState('');
  const [sourcePreviewUrl, setSourcePreviewUrl] = React.useState('');
  const [resultPreviewUrl, setResultPreviewUrl] = React.useState('');
  const [resultImageSize, setResultImageSize] = React.useState<{
    width: number;
    height: number;
    naturalWidth: number;
    naturalHeight: number;
  }>();

  const resolveModelDisplayLabel = React.useCallback(
    (modelId?: string, modelName?: string) => {
      const model = modelOptions.find((item) => item.id === modelId);
      const displayName = model?.name || modelName || modelId || '-';
      return model?.currentVersion ? `${displayName} - (版本号: ${model.currentVersion})` : displayName;
    },
    [modelOptions],
  );

  const runningDeployments = React.useMemo(
    () =>
      deploymentData.filter(
        (item) => item.status === 'RUNNING' && (!selectedModelId || item.modelId === selectedModelId),
      ),
    [deploymentData, selectedModelId],
  );

  const selectedDeployment = React.useMemo(
    () => deploymentData.find((item) => item.id === selectedDeploymentId),
    [deploymentData, selectedDeploymentId],
  );

  const selectedCamera = React.useMemo(
    () => cameraOptions.find((item) => item.id === cameraId),
    [cameraId, cameraOptions],
  );

  const inferenceResultImageUrl = resolveInferenceArtifactUrl(resultPayload?.result_image_url);
  const inferenceResultVideoUrl = resolveInferenceArtifactUrl(resultPayload?.output_video_url);
  const inferenceResultJsonUrl = resolveInferenceArtifactUrl(resultPayload?.json_url);
  const showInferenceServiceSelector = sourceMode === 'IMAGE_UPLOAD';
  const inferenceDetections = React.useMemo<InferenceBBoxDetection[]>(
    () =>
      Array.isArray(resultPayload?.detections)
        ? (resultPayload?.detections.filter((item: any) => Array.isArray(item?.bbox)) as InferenceBBoxDetection[])
        : [],
    [resultPayload?.detections],
  );
  const resultOverlayImageUrl =
    uploadedSource?.mediaType === 'image'
      ? uploadedSource?.previewUrl || sourcePreviewUrl || resultPreviewUrl
      : resultPreviewUrl;

  const syncResultImageSize = React.useCallback(() => {
    const image = resultImageRef.current;
    if (!image) {
      setResultImageSize(undefined);
      return;
    }
    if (!image.naturalWidth || !image.naturalHeight || !image.clientWidth || !image.clientHeight) {
      return;
    }
    setResultImageSize({
      width: image.clientWidth,
      height: image.clientHeight,
      naturalWidth: image.naturalWidth,
      naturalHeight: image.naturalHeight,
    });
  }, []);

  React.useEffect(() => {
    syncResultImageSize();
  }, [resultOverlayImageUrl, showInferenceOriginal, syncResultImageSize]);

  React.useEffect(() => {
    const image = resultImageRef.current;
    if (!image || typeof ResizeObserver === 'undefined') {
      return undefined;
    }
    const observer = new ResizeObserver(() => {
      syncResultImageSize();
    });
    observer.observe(image);
    return () => {
      observer.disconnect();
    };
  }, [resultOverlayImageUrl, syncResultImageSize]);

  React.useEffect(() => {
    let disposed = false;
    let objectUrl = '';
    if (!uploadedSource?.sourceUri || uploadedSource.previewUrl) {
      setSourcePreviewUrl('');
      return undefined;
    }
    fetchProtectedBlobUrl(uploadedSource.sourceUri)
      .then((nextUrl) => {
        if (disposed) {
          if (nextUrl.startsWith('blob:')) {
            URL.revokeObjectURL(nextUrl);
          }
          return;
        }
        objectUrl = nextUrl;
        setSourcePreviewUrl(nextUrl);
      })
      .catch(() => {
        if (!disposed) {
          setSourcePreviewUrl('');
        }
      });
    return () => {
      disposed = true;
      if (objectUrl.startsWith('blob:')) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [uploadedSource?.previewUrl, uploadedSource?.sourceUri]);

  React.useEffect(() => {
    let disposed = false;
    let objectUrl = '';
    const mediaUrl = inferenceResultImageUrl || inferenceResultVideoUrl;
    if (!mediaUrl) {
      setResultPreviewUrl('');
      return undefined;
    }
    fetchProtectedBlobUrl(mediaUrl)
      .then((nextUrl) => {
        if (disposed) {
          if (nextUrl.startsWith('blob:')) {
            URL.revokeObjectURL(nextUrl);
          }
          return;
        }
        objectUrl = nextUrl;
        setResultPreviewUrl(nextUrl);
      })
      .catch(() => {
        if (!disposed) {
          setResultPreviewUrl('');
        }
      });
    return () => {
      disposed = true;
      if (objectUrl.startsWith('blob:')) {
        URL.revokeObjectURL(objectUrl);
      }
    };
  }, [inferenceResultImageUrl, inferenceResultVideoUrl]);

  const loadInferenceMeta = React.useCallback(async () => {
    const modelRes = await getVideoModelPage({
      pageNo: 1,
      pageSize: 1000,
    });
    setModelOptions(modelRes.data?.list || []);
  }, []);

  const loadInferenceDeployments = React.useCallback(async (modelId?: string) => {
    const requestId = ++inferenceDeploymentRequestRef.current;
    const deploymentRes = await getVideoDeploymentPage({
      modelId,
      pageNo: 1,
      pageSize: 1000,
    });
    if (requestId !== inferenceDeploymentRequestRef.current) {
      return;
    }
    setDeploymentData(deploymentRes.data?.list || []);
  }, []);

  const loadInferenceHistory = React.useCallback(async (modelId?: string) => {
    const inferenceRes = await getVideoInferencePage({
      modelId,
      pageNo: 1,
      pageSize: 100,
      pageOrderName: 'createdAt',
      pageOrderBy: 'desc',
    });
    setHistoryRecords(inferenceRes.data?.list || []);
  }, []);

  const loadInferenceCameras = React.useCallback(async () => {
    const cameraRes = await getVideoCameraPage({ pageNo: 1, pageSize: 200 });
    setCameraOptions(cameraRes.data?.list || []);
  }, []);

  React.useEffect(() => {
    loadInferenceMeta().catch(() => undefined);
  }, [loadInferenceMeta]);

  React.useEffect(() => {
    if (sourceMode !== 'CAMERA') {
      return;
    }
    loadInferenceCameras().catch(() => undefined);
  }, [loadInferenceCameras, sourceMode]);

  React.useEffect(() => {
    if (sourceMode !== 'IMAGE_UPLOAD') {
      setSelectedDeploymentId(undefined);
      return;
    }
    if (!runningDeployments.length) {
      setSelectedDeploymentId(undefined);
      return;
    }
    const matched = runningDeployments.find((item) => item.id === selectedDeploymentId);
    if (!matched) {
      setSelectedDeploymentId(runningDeployments[0].id);
    }
  }, [runningDeployments, selectedDeploymentId, sourceMode]);

  const handleRefreshModelAndServices = React.useCallback(async () => {
    await loadInferenceMeta();
    if (selectedModelId) {
      await loadInferenceDeployments(selectedModelId);
    } else {
      inferenceDeploymentRequestRef.current += 1;
      setDeploymentData([]);
      setSelectedDeploymentId(undefined);
    }
  }, [loadInferenceDeployments, loadInferenceMeta, selectedModelId]);

  const handleModelChange = React.useCallback(
    async (value?: string) => {
      setSelectedModelId(value);
      setSelectedDeploymentId(undefined);
      setHistoryId(undefined);
      setResultPayload(undefined);
      setResultSummary('');
      if (!value) {
        inferenceDeploymentRequestRef.current += 1;
        setDeploymentData([]);
        return;
      }
      await loadInferenceDeployments(value);
      await loadInferenceHistory(value);
    },
    [loadInferenceDeployments],
  );

  const handleRefreshServices = React.useCallback(async () => {
    if (!selectedModelId) {
      return;
    }
    await loadInferenceDeployments(selectedModelId);
  }, [loadInferenceDeployments, selectedModelId]);

  const handleDeploymentChange = React.useCallback(
    (value?: string) => {
      const deployment = deploymentData.find((item) => item.id === value);
      setSelectedDeploymentId(value);
      if (deployment) {
        setSelectedModelId(deployment.modelId);
      }
    },
    [deploymentData],
  );

  const applyInferenceRecord = React.useCallback(
    async (record: VideoModelInferenceTaskRecord) => {
      const resultRes = await getVideoInferenceResult(record.id);
      let parsed: Record<string, any> = {};
      try {
        parsed = resultRes.data?.result ? JSON.parse(resultRes.data.result) : {};
      } catch {
        parsed = {};
      }
      setSelectedModelId(record.modelId);
      setSourceMode(record.taskType === 'VIDEO' ? 'VIDEO_UPLOAD' : 'IMAGE_UPLOAD');
      setResultPayload(parsed);
      setResultSummary(record.resultSummary || '');
      await loadInferenceDeployments(record.modelId);
      setUploadedSource({
        sourceUri: record.sourceUri,
        mediaType: record.taskType === 'VIDEO' ? 'video' : 'image',
      });
    },
    [loadInferenceDeployments],
  );

  const handleHistoryChange = React.useCallback(
    (value?: string) => {
      setHistoryId(value);
      const record = historyRecords.find((item) => item.id === value);
      if (record) {
        applyInferenceRecord(record).catch(() => undefined);
      }
    },
    [applyInferenceRecord, historyRecords],
  );

  const handleSourceModeChange = React.useCallback((value: InferenceSourceMode) => {
    setSourceMode(value);
    setUploadedSource(undefined);
    setCameraId(undefined);
  }, []);

  const handleUploadSource = React.useCallback(
    async (file: File, mediaType: 'image' | 'video') => {
      const previewUrl = URL.createObjectURL(file);
      const formData = new FormData();
      formData.append('Filedata', file);
      const response = await uploadFile(formData, { type: mediaType });
      const payload = (response.data || {}) as { id?: string; url?: string };
      if (!payload.url) {
        throw new Error('上传文件未返回访问地址');
      }
      setUploadedSource({
        fileId: payload.id,
        fileName: file.name,
        sourceUri: toAbsoluteSourceUrl(payload.url),
        previewUrl,
        mediaType,
      });
      setHistoryId(undefined);
      setShowInferenceOriginal(true);
      setResultPayload(undefined);
      setResultSummary('');
    },
    [],
  );

  const handleStartInference = React.useCallback(async () => {
    const modelId = selectedDeploymentId ? selectedDeployment?.modelId : selectedModelId;
    if (!modelId) {
      message.warning('请选择模型');
      return;
    }
    if (sourceMode === 'IMAGE_UPLOAD' && !selectedDeploymentId) {
      message.warning('请选择模型服务');
      return;
    }
    let sourceUri = uploadedSource?.sourceUri || '';
    let taskType = sourceMode === 'VIDEO_UPLOAD' ? 'VIDEO' : 'IMAGE';
    let version = sourceMode === 'IMAGE_UPLOAD' ? selectedDeployment?.version : undefined;
    if (sourceMode === 'CAMERA') {
      if (!selectedCamera) {
        message.warning('请选择摄像头');
        return;
      }
      sourceUri = toAbsoluteSourceUrl(selectedCamera.snapshotUrl || selectedCamera.playUrl || '');
      taskType = selectedCamera.snapshotUrl ? 'IMAGE' : 'VIDEO';
    }
    if (!sourceUri) {
      message.warning('请先选择输入源');
      return;
    }
    if (!version) {
      version =
        modelOptions.find((item) => item.id === modelId)?.currentVersion ||
        historyRecords.find((item) => item.modelId === modelId)?.version ||
        undefined;
    }
    setInferenceSubmitting(true);
    try {
      const res = await createVideoInferenceTask({
        taskName: `推理-${Date.now()}`,
        modelId,
        version,
        taskType,
        sourceUri,
      });
      let parsed: Record<string, any> = {};
      try {
        parsed = res.data.resultJson ? JSON.parse(res.data.resultJson) : {};
      } catch {
        parsed = {};
      }
      setResultPayload(parsed);
      setResultSummary(res.data.resultSummary || '');
      setHistoryId(res.data.id);
      await loadInferenceHistory();
    } finally {
      setInferenceSubmitting(false);
    }
  }, [
    historyRecords,
    loadInferenceHistory,
    message,
    modelOptions,
    selectedCamera,
    selectedDeployment?.modelId,
    selectedDeployment?.version,
    selectedDeploymentId,
    selectedModelId,
    sourceMode,
    uploadedSource?.sourceUri,
  ]);

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'stretch' }}>
      <Card style={{ width: 350, flexShrink: 0 }} styles={{ body: { padding: 0 } }}>
        <Space direction="vertical" size={0} style={{ width: '100%' }}>
          <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
              <Typography.Text strong>模型选择</Typography.Text>
              <Button type="text" size="small" icon={<ReloadOutlined />} onClick={() => void handleRefreshModelAndServices()} />
            </div>
            <Select
              style={{ width: '100%', marginTop: 12 }}
              placeholder="请选择模型"
              value={selectedModelId}
              options={modelOptions.map((item) => ({
                label: resolveModelDisplayLabel(item.id, item.name),
                value: item.id,
              }))}
              onChange={(value) => {
                void handleModelChange(value);
              }}
            />
          </div>
          <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
              <Typography.Text strong>模型服务</Typography.Text>
              <Button type="text" size="small" icon={<ReloadOutlined />} onClick={() => void handleRefreshServices()} />
            </div>
            <Select
              style={{ width: '100%', marginTop: 12 }}
              placeholder={showInferenceServiceSelector ? '请选择模型服务' : '仅图片上传时可选择模型服务'}
              disabled={!showInferenceServiceSelector}
              value={selectedDeploymentId}
              options={runningDeployments.map((item) => ({
                label: `${item.instanceName} (端口:${item.servicePort || '-'})`,
                value: item.id,
              }))}
              onChange={handleDeploymentChange}
            />
            {showInferenceServiceSelector && !runningDeployments.length ? (
              <Typography.Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                {selectedModelId ? '当前模型暂无运行中的模型服务' : '请先选择模型'}
              </Typography.Text>
            ) : null}
          </div>
          <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
              <Typography.Text strong>历史推理记录</Typography.Text>
              <Button type="text" size="small" icon={<ReloadOutlined />} onClick={() => void loadInferenceHistory()} />
            </div>
            <Select
              allowClear
              style={{ width: '100%', marginTop: 12 }}
              placeholder="请选择历史记录"
              value={historyId}
              options={historyRecords.map((item) => ({
                label: `${item.taskName} (${item.modelName || item.modelId})`,
                value: item.id,
              }))}
              onChange={handleHistoryChange}
            />
          </div>
          <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
            <Typography.Text strong>输入源选择</Typography.Text>
            <Select
              style={{ width: '100%', marginTop: 12 }}
              value={sourceMode}
              options={[
                { label: '图片上传', value: 'IMAGE_UPLOAD' },
                { label: '视频上传', value: 'VIDEO_UPLOAD' },
                { label: '摄像头', value: 'CAMERA' },
              ]}
              onChange={(value) => handleSourceModeChange(value as InferenceSourceMode)}
            />
            {sourceMode === 'IMAGE_UPLOAD' ? (
              <div style={{ marginTop: 12 }}>
                <input
                  ref={inferenceImageInputRef}
                  type="file"
                  accept="image/*"
                  style={{ display: 'none' }}
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.currentTarget.value = '';
                    if (!file) {
                      return;
                    }
                    handleUploadSource(file, 'image')
                      .then(() => message.success('图片已上传'))
                      .catch((error) => message.error(error instanceof Error ? error.message : '图片上传失败'));
                  }}
                />
                <Button block onClick={() => inferenceImageInputRef.current?.click()}>
                  选择图片文件
                </Button>
              </div>
            ) : null}
            {sourceMode === 'VIDEO_UPLOAD' ? (
              <div style={{ marginTop: 12 }}>
                <input
                  ref={inferenceVideoInputRef}
                  type="file"
                  accept="video/*"
                  style={{ display: 'none' }}
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.currentTarget.value = '';
                    if (!file) {
                      return;
                    }
                    handleUploadSource(file, 'video')
                      .then(() => message.success('视频已上传'))
                      .catch((error) => message.error(error instanceof Error ? error.message : '视频上传失败'));
                  }}
                />
                <Button block onClick={() => inferenceVideoInputRef.current?.click()}>
                  选择视频文件
                </Button>
              </div>
            ) : null}
            {sourceMode === 'CAMERA' ? (
              <Select
                style={{ width: '100%', marginTop: 12 }}
                placeholder="请选择摄像头"
                value={cameraId}
                options={cameraOptions.map((item) => ({ label: item.name, value: item.id }))}
                onChange={setCameraId}
              />
            ) : null}
          </div>
          <div style={{ padding: 16 }}>
            <Typography.Text strong>控制操作</Typography.Text>
            <Space direction="vertical" size={12} style={{ width: '100%', marginTop: 12 }}>
              <Button type="primary" block loading={inferenceSubmitting} onClick={() => void handleStartInference()}>
                开始检测
              </Button>
              <Button block onClick={() => setShowInferenceOriginal((value) => !value)}>
                {showInferenceOriginal ? '关闭原始对照' : '显示原始对照'}
              </Button>
            </Space>
          </div>
        </Space>
      </Card>
      <div style={{ display: 'grid', gridTemplateColumns: showInferenceOriginal ? '1fr 1fr' : '1fr', gap: 16, flex: 1 }}>
        {showInferenceOriginal ? (
          <Card title="原始输入源" styles={{ body: { minHeight: 520 } }}>
            {sourceMode === 'VIDEO_UPLOAD' && (uploadedSource?.previewUrl || uploadedSource?.sourceUri) ? (
              <video src={uploadedSource?.previewUrl || sourcePreviewUrl} controls style={{ width: '100%', maxHeight: 460 }} />
            ) : sourceMode === 'IMAGE_UPLOAD' && (uploadedSource?.previewUrl || uploadedSource?.sourceUri) ? (
              <img
                src={uploadedSource?.previewUrl || sourcePreviewUrl}
                alt={uploadedSource?.fileName || '输入源'}
                style={{ width: '100%', maxHeight: 460, objectFit: 'contain' }}
              />
            ) : sourceMode === 'CAMERA' && selectedCamera?.snapshotUrl ? (
              <img
                src={toApiUrl(selectedCamera.snapshotUrl)}
                alt={selectedCamera.name}
                style={{ width: '100%', maxHeight: 460, objectFit: 'contain' }}
              />
            ) : sourceMode === 'CAMERA' && selectedCamera?.playUrl ? (
              <video src={selectedCamera.playUrl} controls style={{ width: '100%', maxHeight: 460 }} />
            ) : (
              <Empty description="等待输入源" style={{ marginTop: 120 }} />
            )}
          </Card>
        ) : null}
          <Card title="检测结果" styles={{ body: { minHeight: 520 } }}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              {resultOverlayImageUrl ? (
                <div style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
                  <div style={{ position: 'relative', display: 'inline-block', lineHeight: 0, maxWidth: '100%' }}>
                  <img
                    ref={resultImageRef}
                    src={resultOverlayImageUrl}
                    alt="检测结果"
                    style={{ display: 'block', maxWidth: '100%', maxHeight: 460, width: 'auto', height: 'auto' }}
                    onLoad={syncResultImageSize}
                  />
                  {resultImageSize && inferenceDetections.length
                    ? inferenceDetections.map((item, index) => {
                        const [left, top, right, bottom] = item.bbox || [0, 0, 0, 0];
                        const scaleX = resultImageSize.width / Math.max(resultImageSize.naturalWidth, 1);
                        const scaleY = resultImageSize.height / Math.max(resultImageSize.naturalHeight, 1);
                        return (
                          <div
                            key={`${item.class_name || item.class || 'det'}-${index}`}
                            style={{
                              position: 'absolute',
                              left: left * scaleX,
                              top: top * scaleY,
                              width: Math.max((right - left) * scaleX, 2),
                              height: Math.max((bottom - top) * scaleY, 2),
                              border: '2px solid #ff4d4f',
                              boxSizing: 'border-box',
                              pointerEvents: 'none',
                            }}
                          >
                            <span
                              style={{
                                position: 'absolute',
                                left: 0,
                                top: -24,
                                padding: '2px 6px',
                                background: '#ff4d4f',
                                color: '#fff',
                                fontSize: 12,
                                lineHeight: '16px',
                                borderRadius: 4,
                                whiteSpace: 'nowrap',
                              }}
                            >
                              {`${item.class_name || item.class || '目标'} ${typeof item.confidence === 'number' ? item.confidence.toFixed(2) : ''}`}
                            </span>
                          </div>
                        );
                      })
                    : null}
                  </div>
                </div>
              ) : resultPreviewUrl && sourceMode !== 'IMAGE_UPLOAD' ? (
                <video src={resultPreviewUrl} controls style={{ width: '100%', maxHeight: 460 }} />
              ) : (
                <Empty description="检测结果将显示在这里" style={{ marginTop: 120 }} />
              )}
              {resultSummary ? <Typography.Text type="secondary">{resultSummary}</Typography.Text> : null}
              {typeof resultPayload?.detection_count === 'number' ? (
                <Typography.Text>检测数量：{resultPayload.detection_count}</Typography.Text>
            ) : null}
            {Array.isArray(resultPayload?.detections) && resultPayload.detections.length ? (
              <pre
                style={{
                  margin: 0,
                  padding: 12,
                  borderRadius: 8,
                  background: '#0f172a',
                  color: '#e2e8f0',
                  whiteSpace: 'pre-wrap',
                  overflow: 'auto',
                }}
              >
                {JSON.stringify(resultPayload.detections, null, 2)}
              </pre>
            ) : null}
            {!inferenceResultImageUrl && !inferenceResultVideoUrl && resultPayload?.response ? (
              <Typography.Paragraph style={{ marginBottom: 0 }}>{String(resultPayload.response)}</Typography.Paragraph>
            ) : null}
            {inferenceResultJsonUrl ? (
              <Typography.Link href={inferenceResultJsonUrl} target="_blank">
                查看结果JSON
              </Typography.Link>
            ) : null}
          </Space>
        </Card>
      </div>
    </div>
  );
};

export default InferenceWorkbench;
