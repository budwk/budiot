import {
  BorderOutlined,
  CameraOutlined,
  DeleteOutlined,
  DeploymentUnitOutlined,
  SaveOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Empty,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Tag,
  Typography,
} from 'antd';
import * as React from 'react';
import { API_BASE_URL, APP_ID, DEFAULT_LANG } from '@/constants/app';
import { snapshotVideoCamera } from '@/services/budiot/video/media';
import { getVideoTaskDetail, saveVideoTaskRegionConfigs } from '@/services/budiot/video/task';
import type {
  VideoAlgoTaskMeta,
  VideoAlgoTaskRecord,
  VideoTaskRegionConfigRecord,
} from '@/services/budiot/video/typing';
import { getStoredToken } from '@/utils/session';

type Point = { x: number; y: number };
type DrawTool = 'RECTANGLE' | 'POLYGON';

type RegionConfigModalProps = {
  open: boolean;
  taskId?: string;
  meta: VideoAlgoTaskMeta;
  onClose: () => void;
  onSaved: () => void;
  onSnapshotPersisted?: (cameraId: string, snapshotImageUrl: string) => void;
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
    throw new Error('加载摄像头预览失败');
  }
  const blob = await response.blob();
  return window.URL.createObjectURL(blob);
};

const parsePolygonPoints = (pointsJson?: string): Point[] => {
  if (!pointsJson) {
    return [];
  }
  try {
    const data = JSON.parse(pointsJson);
    if (!Array.isArray(data)) {
      return [];
    }
    return data
      .map((item) => ({
        x: Number(item?.x),
        y: Number(item?.y),
      }))
      .filter((item) => Number.isFinite(item.x) && Number.isFinite(item.y));
  } catch {
    return [];
  }
};

const stringifyPolygonPoints = (points: Point[]) =>
  JSON.stringify(points.map((item) => ({ x: Math.round(item.x), y: Math.round(item.y) })));

const toSvgPoints = (points: Point[]) => points.map((item) => `${item.x},${item.y}`).join(' ');

const buildRectanglePoints = (start: Point, end: Point): Point[] => {
  const minX = Math.min(start.x, end.x);
  const maxX = Math.max(start.x, end.x);
  const minY = Math.min(start.y, end.y);
  const maxY = Math.max(start.y, end.y);
  return [
    { x: minX, y: minY },
    { x: maxX, y: minY },
    { x: maxX, y: maxY },
    { x: minX, y: maxY },
  ];
};

const isRectanglePoints = (points: Point[]) => {
  if (points.length !== 4) {
    return false;
  }
  const uniqueX = Array.from(new Set(points.map((item) => Math.round(item.x))));
  const uniqueY = Array.from(new Set(points.map((item) => Math.round(item.y))));
  return uniqueX.length === 2 && uniqueY.length === 2;
};

const getRegionShapeLabel = (pointsJson?: string) =>
  isRectanglePoints(parsePolygonPoints(pointsJson)) ? '矩形' : '多边形';

const revokeBlobUrl = (url?: string) => {
  if (url?.startsWith('blob:')) {
    window.URL.revokeObjectURL(url);
  }
};

const getPolygonLabelLayout = (points: Point[], label?: string) => {
  if (!points.length) {
    return undefined;
  }
  const minX = Math.min(...points.map((item) => item.x));
  const minY = Math.min(...points.map((item) => item.y));
  const width = Math.max(64, (label?.length || 0) * 14 + 20);
  const height = 24;
  const x = Math.max(4, minX);
  const y = Math.max(4, minY - height - 6);
  return {
    x,
    y,
    width,
    height,
    textX: x + 8,
    textY: y + 16,
  };
};

const getCenteredLabelLayout = (points: Point[], label?: string) => {
  if (!points.length || !label) {
    return undefined;
  }
  const minX = Math.min(...points.map((item) => item.x));
  const maxX = Math.max(...points.map((item) => item.x));
  const minY = Math.min(...points.map((item) => item.y));
  const maxY = Math.max(...points.map((item) => item.y));
  const width = Math.max(88, label.length * 14 + 24);
  const height = 28;
  const centerX = (minX + maxX) / 2;
  const centerY = (minY + maxY) / 2;
  return {
    x: centerX - width / 2,
    y: centerY - height / 2,
    width,
    height,
    textX: centerX,
    textY: centerY + 1,
  };
};

const renderCenteredModelLabel = (
  points: Point[],
  label?: string,
  color = '#1677ff',
) => {
  const labelLayout = getCenteredLabelLayout(points, label);
  return labelLayout ? (
    <g>
      <rect
        x={labelLayout.x}
        y={labelLayout.y}
        width={labelLayout.width}
        height={labelLayout.height}
        rx={8}
        fill={color}
        fillOpacity={0.92}
      />
      <text
        x={labelLayout.textX}
        y={labelLayout.textY}
        fill="#fff"
        fontSize="13"
        fontWeight="600"
        textAnchor="middle"
        dominantBaseline="middle"
      >
        {label}
      </text>
    </g>
  ) : null;
};

const normalizeRegionConfigs = (configs?: VideoTaskRegionConfigRecord[]) =>
  (configs || [])
    .filter((item): item is VideoTaskRegionConfigRecord => Boolean(item?.cameraId && item?.modelId))
    .map((item, index) => ({
      ...item,
      location: item.location ?? index,
      enabled: item.enabled !== false,
    }));

const getRelocatedRegionConfigs = (configs: VideoTaskRegionConfigRecord[]) => {
  const counters = new Map<string, number>();
  return configs.map((item) => {
    const nextLocation = counters.get(item.cameraId) || 0;
    counters.set(item.cameraId, nextLocation + 1);
    return {
      ...item,
      location: nextLocation,
    };
  });
};

const RegionConfigModal: React.FC<RegionConfigModalProps> = ({
  open,
  taskId,
  meta,
  onClose,
  onSaved,
  onSnapshotPersisted,
}) => {
  const { message } = App.useApp();
  const svgRef = React.useRef<SVGSVGElement>(null);
  const [loading, setLoading] = React.useState(false);
  const [saving, setSaving] = React.useState(false);
  const [snapshotLoading, setSnapshotLoading] = React.useState(false);
  const [regionTask, setRegionTask] = React.useState<VideoAlgoTaskRecord>();
  const [regionConfigs, setRegionConfigs] = React.useState<VideoTaskRegionConfigRecord[]>([]);
  const [cameraKeyword, setCameraKeyword] = React.useState('');
  const [activeCameraId, setActiveCameraId] = React.useState<string>();
  const [selectedRegionId, setSelectedRegionId] = React.useState<string>();
  const [drawingRegionId, setDrawingRegionId] = React.useState<string>();
  const [activeTool, setActiveTool] = React.useState<DrawTool>('RECTANGLE');
  const [selectedModelId, setSelectedModelId] = React.useState<string>();
  const [polygonDraft, setPolygonDraft] = React.useState<Point[]>([]);
  const [rectDraft, setRectDraft] = React.useState<Point[]>([]);
  const [rectStart, setRectStart] = React.useState<Point>();
  const [cameraPreviewUrls, setCameraPreviewUrls] = React.useState<Record<string, string>>({});
  const [previewSize, setPreviewSize] = React.useState({ width: 1280, height: 720 });

  const cancelDrawing = React.useCallback(() => {
    setDrawingRegionId(undefined);
    setPolygonDraft([]);
    setRectDraft([]);
    setRectStart(undefined);
  }, []);

  const resetState = React.useCallback(() => {
    setLoading(false);
    setSaving(false);
    setSnapshotLoading(false);
    setRegionTask(undefined);
    setRegionConfigs([]);
    setCameraKeyword('');
    setActiveCameraId(undefined);
    setSelectedRegionId(undefined);
    setDrawingRegionId(undefined);
    setActiveTool('RECTANGLE');
    setSelectedModelId(undefined);
    setPolygonDraft([]);
    setRectDraft([]);
    setRectStart(undefined);
    setPreviewSize({ width: 1280, height: 720 });
    setCameraPreviewUrls((prev) => {
      Object.values(prev).forEach((url) => revokeBlobUrl(url));
      return {};
    });
  }, []);

  React.useEffect(() => {
    if (!open) {
      resetState();
    }
  }, [open, resetState]);

  const replaceCameraPreview = React.useCallback((cameraId: string, nextUrl: string) => {
    setCameraPreviewUrls((prev) => {
      revokeBlobUrl(prev[cameraId]);
      return {
        ...prev,
        [cameraId]: nextUrl,
      };
    });
  }, []);

  const loadCameraPreview = React.useCallback(
    async (cameraId: string, sourceUrl?: string) => {
      if (!cameraId || !sourceUrl) {
        return;
      }
      try {
        const preview = await fetchProtectedBlobUrl(sourceUrl);
        replaceCameraPreview(cameraId, preview);
      } catch {
        replaceCameraPreview(cameraId, toApiUrl(sourceUrl));
      }
    },
    [replaceCameraPreview],
  );

  const selectCamera = React.useCallback(
    (
      cameraId: string | undefined,
      configs: VideoTaskRegionConfigRecord[],
      modelIds: string[] | undefined,
    ) => {
      setActiveCameraId(cameraId);
      cancelDrawing();
      if (!cameraId) {
        setSelectedRegionId(undefined);
        setSelectedModelId(modelIds?.[0]);
        return;
      }
      const firstRegion = configs.find((item) => item.cameraId === cameraId);
      setSelectedRegionId(firstRegion?.id);
      setSelectedModelId(firstRegion?.modelId || modelIds?.[0]);
    },
    [cancelDrawing],
  );

  React.useEffect(() => {
    if (!open || !taskId) {
      return;
    }
    let mounted = true;
    setLoading(true);
    getVideoTaskDetail(taskId)
      .then((response) => {
        if (!mounted) {
          return;
        }
        const detail = response.data;
        const nextRegionConfigs = normalizeRegionConfigs(detail.regionConfigs);
        const firstCameraId = detail.cameraIds?.[0];
        setRegionTask(detail);
        setRegionConfigs(nextRegionConfigs);
        setActiveTool('RECTANGLE');
        selectCamera(firstCameraId, nextRegionConfigs, detail.modelIds);
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });
    return () => {
      mounted = false;
    };
  }, [open, selectCamera, taskId]);

  const activeCamera = React.useMemo(
    () => meta.cameras.find((item) => item.id === activeCameraId),
    [activeCameraId, meta.cameras],
  );

  const taskCameras = React.useMemo(
    () => meta.cameras.filter((item) => regionTask?.cameraIds?.includes(item.id)),
    [meta.cameras, regionTask?.cameraIds],
  );

  const currentTaskModelOptions = React.useMemo(() => {
    if (!regionTask?.modelIds?.length) {
      return [];
    }
    return meta.models.filter((item) => regionTask.modelIds?.includes(item.id));
  }, [meta.models, regionTask?.modelIds]);

  const filteredTaskCameras = React.useMemo(() => {
    const keyword = cameraKeyword.trim().toLowerCase();
    if (!keyword) {
      return taskCameras;
    }
    return taskCameras.filter(
      (item) =>
        item.name.toLowerCase().includes(keyword) ||
        item.cameraCode.toLowerCase().includes(keyword),
    );
  }, [cameraKeyword, taskCameras]);

  const activeCameraRegions = React.useMemo(
    () =>
      regionConfigs
        .filter((item) => item.cameraId === activeCameraId)
        .sort((left, right) => (left.location || 0) - (right.location || 0)),
    [activeCameraId, regionConfigs],
  );

  const selectedRegion = React.useMemo(
    () => regionConfigs.find((item) => item.id === selectedRegionId),
    [regionConfigs, selectedRegionId],
  );

  const drawingModelName = React.useMemo(() => {
    const drawingRegion = regionConfigs.find((item) => item.id === drawingRegionId);
    const modelId = drawingRegion?.modelId || selectedModelId;
    if (!modelId) {
      return '';
    }
    return currentTaskModelOptions.find((item) => item.id === modelId)?.name || modelId;
  }, [currentTaskModelOptions, drawingRegionId, regionConfigs, selectedModelId]);

  const getRegionModelName = React.useCallback(
    (modelId?: string) => {
      if (!modelId) {
        return '';
      }
      return currentTaskModelOptions.find((item) => item.id === modelId)?.name || modelId;
    },
    [currentTaskModelOptions],
  );

  const currentPreviewUrl = activeCameraId ? cameraPreviewUrls[activeCameraId] : '';

  React.useEffect(() => {
    if (!open) {
      return;
    }
    taskCameras.forEach((camera) => {
      const previewUrl = camera.snapshotImageUrl || camera.snapshotUrl;
      if (previewUrl && !cameraPreviewUrls[camera.id]) {
        loadCameraPreview(camera.id, previewUrl).catch(() => undefined);
      }
    });
  }, [cameraPreviewUrls, loadCameraPreview, open, taskCameras]);

  const handleCaptureSnapshot = React.useCallback(
    async () => {
      if (!activeCameraId) {
        message.warning('请先选择摄像头');
        return;
      }
      setSnapshotLoading(true);
      try {
        const response = await snapshotVideoCamera(activeCameraId, true);
        const nextUrl =
          response.data?.fileUrl ||
          response.data?.thumbnailUrl ||
          activeCamera?.snapshotImageUrl ||
          activeCamera?.snapshotUrl;
        if (nextUrl) {
          onSnapshotPersisted?.(activeCameraId, nextUrl);
          await loadCameraPreview(activeCameraId, nextUrl);
        }
      } finally {
        setSnapshotLoading(false);
      }
    },
    [activeCamera?.snapshotImageUrl, activeCamera?.snapshotUrl, activeCameraId, loadCameraPreview, message, onSnapshotPersisted],
  );

  const svgPoint = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      const svg = svgRef.current;
      if (!svg) {
        return undefined;
      }
      const point = svg.createSVGPoint();
      point.x = event.clientX;
      point.y = event.clientY;
      const matrix = svg.getScreenCTM();
      if (!matrix) {
        return undefined;
      }
      const transformed = point.matrixTransform(matrix.inverse());
      return {
        x: Math.max(0, Math.min(previewSize.width, transformed.x)),
        y: Math.max(0, Math.min(previewSize.height, transformed.y)),
      };
    },
    [previewSize.height, previewSize.width],
  );

  const updateRegionConfig = React.useCallback((id: string, patch: Partial<VideoTaskRegionConfigRecord>) => {
    setRegionConfigs((prev) => prev.map((item) => (item.id === id ? { ...item, ...patch } : item)));
  }, []);

  const createRegionRecord = React.useCallback(
    (cameraId: string) => {
      const fallbackModelId = selectedModelId || currentTaskModelOptions[0]?.id;
      if (!fallbackModelId) {
        message.warning('请先在任务中关联已部署模型');
        return undefined;
      }
      const nextId = `draft-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
      const nextRegion: VideoTaskRegionConfigRecord = {
        id: nextId,
        cameraId,
        modelId: fallbackModelId,
        name: `区域 ${regionConfigs.filter((item) => item.cameraId === cameraId).length + 1}`,
        polygonPoints: '',
        enabled: true,
        location: regionConfigs.filter((item) => item.cameraId === cameraId).length,
      };
      setRegionConfigs((prev) => [...prev, nextRegion]);
      setSelectedRegionId(nextId);
      setSelectedModelId(fallbackModelId);
      return nextId;
    },
    [currentTaskModelOptions, message, regionConfigs, selectedModelId],
  );

  const ensureDrawingTarget = React.useCallback(() => {
    if (!activeCameraId) {
      message.warning('请先选择摄像头');
      return undefined;
    }
    return createRegionRecord(activeCameraId);
  }, [activeCameraId, createRegionRecord, message]);

  const finishPolygonDrawing = React.useCallback(() => {
    if (!drawingRegionId || polygonDraft.length < 3) {
      return;
    }
    updateRegionConfig(drawingRegionId, { polygonPoints: stringifyPolygonPoints(polygonDraft) });
    cancelDrawing();
  }, [cancelDrawing, drawingRegionId, polygonDraft, updateRegionConfig]);

  const clearCanvas = React.useCallback(() => {
    if (drawingRegionId || polygonDraft.length || rectDraft.length) {
      cancelDrawing();
      return;
    }
    if (selectedRegionId) {
      updateRegionConfig(selectedRegionId, { polygonPoints: '' });
    }
  }, [
    cancelDrawing,
    drawingRegionId,
    polygonDraft.length,
    rectDraft.length,
    selectedRegionId,
    updateRegionConfig,
  ]);

  const removeRegion = React.useCallback(
    (id: string) => {
      setRegionConfigs((prev) => getRelocatedRegionConfigs(prev.filter((item) => item.id !== id)));
      if (selectedRegionId === id) {
        const remainingRegions = activeCameraRegions.filter((item) => item.id !== id);
        setSelectedRegionId(remainingRegions[0]?.id);
      }
      if (drawingRegionId === id) {
        cancelDrawing();
      }
    },
    [activeCameraRegions, cancelDrawing, drawingRegionId, selectedRegionId],
  );

  const handleRegionCanvasClick = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      if (activeTool !== 'POLYGON') {
        return;
      }
      const targetId = drawingRegionId || ensureDrawingTarget();
      if (!targetId) {
        return;
      }
      const point = svgPoint(event);
      if (!point) {
        return;
      }
      setDrawingRegionId(targetId);
      setSelectedRegionId(targetId);
      setPolygonDraft((prev) => [...prev, point]);
    },
    [activeTool, drawingRegionId, ensureDrawingTarget, svgPoint],
  );

  const handleRegionCanvasMouseDown = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      if (activeTool !== 'RECTANGLE') {
        return;
      }
      const point = svgPoint(event);
      if (!point) {
        return;
      }
      const targetId = ensureDrawingTarget();
      if (!targetId) {
        return;
      }
      setDrawingRegionId(targetId);
      setSelectedRegionId(targetId);
      setRectStart(point);
      setRectDraft(buildRectanglePoints(point, point));
    },
    [activeTool, ensureDrawingTarget, svgPoint],
  );

  const handleRegionCanvasMouseMove = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      if (activeTool !== 'RECTANGLE' || !rectStart) {
        return;
      }
      const point = svgPoint(event);
      if (!point) {
        return;
      }
      setRectDraft(buildRectanglePoints(rectStart, point));
    },
    [activeTool, rectStart, svgPoint],
  );

  const handleRegionCanvasMouseUp = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      if (activeTool !== 'RECTANGLE' || !rectStart || !drawingRegionId) {
        return;
      }
      const point = svgPoint(event);
      if (!point) {
        cancelDrawing();
        return;
      }
      const points = buildRectanglePoints(rectStart, point);
      const width = Math.abs(points[1].x - points[0].x);
      const height = Math.abs(points[2].y - points[1].y);
      if (width < 8 || height < 8) {
        cancelDrawing();
        return;
      }
      updateRegionConfig(drawingRegionId, { polygonPoints: stringifyPolygonPoints(points) });
      cancelDrawing();
    },
    [activeTool, cancelDrawing, drawingRegionId, rectStart, svgPoint, updateRegionConfig],
  );

  const handleRegionCanvasContextMenu = React.useCallback(
    (event: React.MouseEvent<SVGSVGElement>) => {
      event.preventDefault();
      if (activeTool === 'POLYGON') {
        finishPolygonDrawing();
      }
    },
    [activeTool, finishPolygonDrawing],
  );

  React.useEffect(() => {
    if (!open) {
      return;
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Delete' && selectedRegionId) {
        event.preventDefault();
        removeRegion(selectedRegionId);
        return;
      }
      if (event.key === 'Escape') {
        event.preventDefault();
        cancelDrawing();
        return;
      }
      if (event.key.toLowerCase() === 'r') {
        event.preventDefault();
        cancelDrawing();
        setActiveTool('RECTANGLE');
        return;
      }
      if (event.key.toLowerCase() === 'p') {
        event.preventDefault();
        cancelDrawing();
        setActiveTool('POLYGON');
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [cancelDrawing, open, removeRegion, selectedRegionId]);

  const closeModal = React.useCallback(() => {
    onClose();
  }, [onClose]);

  const saveRegionConfig = React.useCallback(async () => {
    if (!regionTask?.id) {
      return;
    }
    if (drawingRegionId || polygonDraft.length || rectDraft.length) {
      message.warning('请先右击完成当前区域绘制后再保存');
      return;
    }
    setSaving(true);
    try {
      const payload = getRelocatedRegionConfigs(normalizeRegionConfigs(regionConfigs));
      await saveVideoTaskRegionConfigs(regionTask.id, payload as Record<string, unknown>[]);
      closeModal();
      onSaved();
    } finally {
      setSaving(false);
    }
  }, [
    closeModal,
    drawingRegionId,
    message,
    onSaved,
    polygonDraft.length,
    rectDraft.length,
    regionConfigs,
    regionTask?.id,
  ]);

  return (
    <Modal
      title="区域检测配置"
      open={open}
      onCancel={closeModal}
      keyboard={false}
      footer={null}
      width="96vw"
      style={{ top: 20 }}
      destroyOnClose
    >
      {loading ? null : regionTask ? (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '320px minmax(0, 1fr)',
            gap: 16,
            minHeight: '72vh',
          }}
        >
          <Card bodyStyle={{ padding: 0 }}>
            <div style={{ padding: 16, borderBottom: '1px solid #f0f0f0' }}>
              <Input
                placeholder="搜索摄像头"
                value={cameraKeyword}
                onChange={(event) => setCameraKeyword(event.target.value)}
                prefix={<SearchOutlined />}
              />
            </div>
            <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
              {filteredTaskCameras.length ? (
                filteredTaskCameras.map((camera) => {
                  const active = camera.id === activeCameraId;
                  const thumbUrl = cameraPreviewUrls[camera.id];
                  return (
                    <div
                      key={camera.id}
                      onClick={() => selectCamera(camera.id, regionConfigs, regionTask?.modelIds)}
                      style={{
                        border: `2px solid ${active ? '#1677ff' : '#e5e7eb'}`,
                        borderRadius: 14,
                        padding: 12,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 14,
                        background: active ? '#f0f7ff' : '#fff',
                      }}
                    >
                      <div
                        style={{
                          width: 104,
                          height: 72,
                          borderRadius: 10,
                          overflow: 'hidden',
                          background: '#f3f4f6',
                          flexShrink: 0,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        {thumbUrl ? (
                          <img
                            src={thumbUrl}
                            alt={camera.name}
                            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                          />
                        ) : (
                          <Typography.Text type="secondary">暂无预览</Typography.Text>
                        )}
                      </div>
                      <div style={{ minWidth: 0 }}>
                        <Typography.Title level={5} style={{ margin: 0 }}>
                          {camera.name}
                        </Typography.Title>
                        <Typography.Text type="secondary">{camera.cameraCode}</Typography.Text>
                      </div>
                    </div>
                  );
                })
              ) : (
                <Empty description="没有匹配的摄像头" />
              )}
            </div>
          </Card>

          <Card bodyStyle={{ padding: 16 }}>
            <Space wrap style={{ marginBottom: 14 }}>
                <Button
                  type="primary"
                  icon={<CameraOutlined />}
                  loading={snapshotLoading}
                  onClick={() => handleCaptureSnapshot()}
                >
                  抓拍图片
                </Button>
              <Button onClick={clearCanvas}>清空画布</Button>
              <Button
                type="primary"
                icon={<SaveOutlined />}
                loading={saving}
                onClick={saveRegionConfig}
              >
                保存区域
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                disabled={!selectedRegionId}
                onClick={() => selectedRegionId && removeRegion(selectedRegionId)}
              >
                Del 删除选中
              </Button>
            </Space>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 14,
                flexWrap: 'wrap',
                padding: '10px 14px',
                marginBottom: 16,
                borderRadius: 10,
                border: '1px solid #e5e7eb',
                background: '#fafafa',
              }}
            >
              {[
                ['Del', '删除选中'],
                ['R', '矩形'],
                ['P', '多边形'],
                ['Esc', '取消绘制'],
                ['右键', '封闭多边形'],
              ].map(([key, text]) => (
                <Space key={key} size={6}>
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      minWidth: 34,
                      height: 28,
                      paddingInline: 8,
                      borderRadius: 8,
                      background: '#fff',
                      border: '1px solid #d9d9d9',
                      color: '#1677ff',
                      fontWeight: 600,
                    }}
                  >
                    {key}
                  </span>
                  <Typography.Text type="secondary">{text}</Typography.Text>
                </Space>
              ))}
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '300px minmax(0, 1fr) 340px',
                gap: 16,
                minHeight: '58vh',
              }}
            >
              <Card bodyStyle={{ padding: 0 }}>
                <div style={{ padding: '14px 18px', borderBottom: '1px solid #f0f0f0' }}>
                  <Typography.Title level={5} style={{ margin: 0 }}>
                    绘制工具
                  </Typography.Title>
                </div>
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>
                  {[
                    { key: 'RECTANGLE', label: '矩形', icon: <BorderOutlined /> },
                    { key: 'POLYGON', label: '多边形', icon: <DeploymentUnitOutlined /> },
                  ].map((tool) => {
                    const active = activeTool === tool.key;
                    return (
                      <div
                        key={tool.key}
                        onClick={() => {
                          cancelDrawing();
                          setActiveTool(tool.key as DrawTool);
                        }}
                        style={{
                          height: 92,
                          borderRadius: 14,
                          border: `2px solid ${active ? '#1677ff' : '#e5e7eb'}`,
                          background: active ? '#f0f7ff' : '#fff',
                          cursor: 'pointer',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: 10,
                          color: active ? '#1677ff' : '#4b5563',
                          fontWeight: 600,
                        }}
                      >
                        <span style={{ fontSize: 24 }}>{tool.icon}</span>
                        <span>{tool.label}</span>
                      </div>
                    );
                  })}
                </div>
                <div
                  style={{
                    padding: '14px 18px',
                    borderTop: '1px solid #f0f0f0',
                    borderBottom: '1px solid #f0f0f0',
                  }}
                >
                  <Typography.Title level={5} style={{ margin: 0 }}>
                    算法模型
                  </Typography.Title>
                </div>
                <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
                    {currentTaskModelOptions.map((item) => {
                      const active = selectedModelId === item.id;
                      return (
                        <div
                          key={item.id}
                          onClick={() => {
                            setSelectedModelId(item.id);
                          }}
                          style={{
                            padding: '14px 16px',
                          borderRadius: 12,
                          border: `2px solid ${active ? '#1677ff' : '#e5e7eb'}`,
                          background: active ? '#f0f7ff' : '#fff',
                          cursor: 'pointer',
                          fontWeight: 600,
                        }}
                      >
                        {item.name}
                        {item.currentVersion ? ` (${item.currentVersion})` : ''}
                      </div>
                    );
                  })}
                  {!currentTaskModelOptions.length ? <Empty description="未关联可用模型" /> : null}
                </div>
              </Card>

              <Card bodyStyle={{ padding: 16 }}>
                <Typography.Title level={5} style={{ marginTop: 0, marginBottom: 12 }}>
                  {activeCamera?.name || '未选择摄像头'}
                </Typography.Title>
                <div
                  style={{
                    position: 'relative',
                    width: '100%',
                    overflow: 'hidden',
                    borderRadius: 12,
                    background: '#0f172a',
                    minHeight: 520,
                    aspectRatio: `${previewSize.width} / ${previewSize.height}`,
                  }}
                >
                  {currentPreviewUrl ? (
                    <img
                      src={currentPreviewUrl}
                      alt={activeCamera?.name || 'camera'}
                      style={{ width: '100%', height: '100%', objectFit: 'contain', display: 'block' }}
                      onLoad={(event) => {
                        const { naturalWidth, naturalHeight } = event.currentTarget;
                        if (naturalWidth > 0 && naturalHeight > 0) {
                          setPreviewSize({ width: naturalWidth, height: naturalHeight });
                        }
                      }}
                    />
                  ) : (
                    <div
                      style={{
                        width: '100%',
                        height: '100%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#94a3b8',
                        background: 'linear-gradient(135deg, #111827 0%, #1f2937 100%)',
                      }}
                    >
                      {snapshotLoading ? '抓拍中...' : '暂无抓拍图片'}
                    </div>
                  )}
                  <svg
                    ref={svgRef}
                    viewBox={`0 0 ${previewSize.width} ${previewSize.height}`}
                    preserveAspectRatio="xMidYMid meet"
                    style={{
                      position: 'absolute',
                      inset: 0,
                      width: '100%',
                      height: '100%',
                      cursor: 'crosshair',
                    }}
                    onClick={handleRegionCanvasClick}
                    onMouseDown={handleRegionCanvasMouseDown}
                    onMouseMove={handleRegionCanvasMouseMove}
                    onMouseUp={handleRegionCanvasMouseUp}
                    onContextMenu={handleRegionCanvasContextMenu}
                  >
                    {activeCameraRegions.map((region) => {
                      const points = parsePolygonPoints(region.polygonPoints);
                      if (!points.length) {
                        return null;
                      }
                      const active = region.id === selectedRegionId;
                      const labelLayout = getPolygonLabelLayout(points, region.name);
                      const modelName = getRegionModelName(region.modelId);
                      return (
                        <g
                          key={region.id}
                          onMouseDown={(event) => {
                            event.stopPropagation();
                            setSelectedRegionId(region.id);
                          }}
                        >
                          <polygon
                            points={toSvgPoints(points)}
                            fill={active ? '#ff4d4f55' : '#ff4d4f33'}
                            stroke="#ff4d4f"
                            strokeWidth={active ? 4 : 2}
                          />
                          {labelLayout ? (
                            <>
                              <rect
                                x={labelLayout.x}
                                y={labelLayout.y}
                                width={labelLayout.width}
                                height={labelLayout.height}
                                rx={6}
                                fill="#ff4d4f"
                              />
                              <text
                                x={labelLayout.textX}
                                y={labelLayout.textY}
                                fill="#fff"
                                fontSize="13"
                                fontWeight="600"
                              >
                                {region.name}
                              </text>
                            </>
                          ) : null}
                          {renderCenteredModelLabel(points, modelName, '#ff4d4f')}
                        </g>
                      );
                    })}
                    {rectDraft.length === 4 ? (
                      <>
                        <polygon
                          points={toSvgPoints(rectDraft)}
                          fill="#1677ff22"
                          stroke="#1677ff"
                          strokeWidth={2}
                        />
                        {renderCenteredModelLabel(rectDraft, drawingModelName)}
                      </>
                    ) : null}
                    {polygonDraft.length > 0 ? (
                      <>
                        <polyline
                          points={toSvgPoints(polygonDraft)}
                          fill="none"
                          stroke="#1677ff"
                          strokeWidth={3}
                        />
                        {polygonDraft.map((point, index) => (
                          <circle
                            key={`${point.x}-${point.y}-${index}`}
                            cx={point.x}
                            cy={point.y}
                            r={5}
                            fill="#1677ff"
                          />
                        ))}
                        {renderCenteredModelLabel(polygonDraft, drawingModelName)}
                      </>
                    ) : null}
                  </svg>
                </div>
              </Card>

              <div style={{ display: 'grid', gridTemplateRows: '1fr auto', gap: 16 }}>
                <Card title={`检测区域 (${activeCameraRegions.length})`} bodyStyle={{ padding: 16 }}>
                  {activeCameraRegions.length ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                      {activeCameraRegions.map((region, index) => {
                        const active = region.id === selectedRegionId;
                        return (
                          <div
                            key={region.id}
                            onClick={() => setSelectedRegionId(active ? undefined : region.id)}
                            style={{
                              border: `2px solid ${active ? '#1677ff' : '#e5e7eb'}`,
                              borderRadius: 14,
                              padding: 16,
                              background: active ? '#f0f7ff' : '#fff',
                              cursor: 'pointer',
                            }}
                          >
                            <div
                              style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'flex-start',
                                gap: 12,
                              }}
                            >
                              <div>
                                <Typography.Title level={5} style={{ margin: 0 }}>
                                  {region.name || `区域 ${index + 1}`}
                                </Typography.Title>
                                <Typography.Text type="secondary">
                                  {getRegionShapeLabel(region.polygonPoints)}
                                </Typography.Text>
                              </div>
                              <Button
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={(event) => {
                                  event.stopPropagation();
                                  removeRegion(region.id!);
                                }}
                              />
                            </div>
                            <div style={{ marginTop: 12 }}>
                              <Typography.Text type="secondary">绑定模型：</Typography.Text>
                              <Tag>
                                {currentTaskModelOptions.find((item) => item.id === region.modelId)
                                  ?.name || region.modelId}
                              </Tag>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <Empty description="当前摄像头暂无检测区域" />
                  )}
                </Card>

                <Card title="区域配置" bodyStyle={{ padding: 16 }}>
                  {selectedRegion ? (
                    <Space direction="vertical" size={14} style={{ width: '100%' }}>
                      <Input
                        value={selectedRegion.name}
                        placeholder="请输入区域名称"
                        onChange={(event) =>
                          updateRegionConfig(selectedRegion.id!, { name: event.target.value })
                        }
                      />
                      <Select
                        value={selectedRegion.modelId}
                        placeholder="请选择绑定模型"
                        options={currentTaskModelOptions.map((item) => ({
                          label: `${item.name}${item.currentVersion ? ` (${item.currentVersion})` : ''}`,
                          value: item.id,
                        }))}
                        onChange={(value) => {
                          updateRegionConfig(selectedRegion.id!, { modelId: value });
                        }}
                      />
                      <Switch
                        checked={selectedRegion.enabled}
                        checkedChildren="启用"
                        unCheckedChildren="停用"
                        onChange={(checked) =>
                          updateRegionConfig(selectedRegion.id!, { enabled: checked })
                        }
                      />
                    </Space>
                  ) : (
                    <Empty description="请选择一个检测区域" />
                  )}
                </Card>
              </div>
            </div>
          </Card>
        </div>
      ) : (
        <Empty description="未加载到任务信息" />
      )}
    </Modal>
  );
};

export default RegionConfigModal;
