import {
  AlertOutlined,
  ApartmentOutlined,
  CameraOutlined,
  ClockCircleOutlined,
  FullscreenOutlined,
  PartitionOutlined,
  ReloadOutlined,
  RollbackOutlined,
  SearchOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons';
import { Badge, Button, Empty, Image, Input, List, Modal, Spin, Tag, Tree, Typography } from 'antd';
import type { DataNode } from 'antd/es/tree';
import dayjs from 'dayjs';
import * as React from 'react';
import { VideoPlayer, VideoPlayerOverlay, type VideoPlayerOverlayDetection } from '@/components';
import { getVideoMediaEndpoint } from '@/services/budiot/video/media';
import { getVideoMonitorDashboardData, getVideoMonitorDashboardRuntime } from '@/services/budiot/video/monitor-dashboard';
import type {
  VideoAlarmRecord,
  VideoCameraRecord,
  VideoCatalogRecord,
  VideoMediaEndpoint,
  VideoModuleOverview,
  VideoMonitorRealtimeCameraState,
} from '@/services/budiot/video/typing';

const SPLIT_OPTIONS = [1, 4, 6, 9, 16];
const DETECTION_COLORS = ['#ff4d4f', '#fa8c16', '#52c41a', '#1d9bf0', '#722ed1', '#eb2f96'];
const DETECTION_STALE_MS = 6000;

type CameraTreeLeaf = DataNode & { cameraId?: string };

const screenStyle: React.CSSProperties = {
  minHeight: 'calc(100vh - 48px)',
  margin: -24,
  padding: 16,
  background:
    'radial-gradient(circle at top, rgba(44, 106, 202, 0.35) 0%, rgba(9, 23, 56, 0.98) 28%, #07162f 100%)',
  color: '#fff',
};

const panelStyle: React.CSSProperties = {
  borderRadius: 14,
  border: '1px solid rgba(68, 138, 255, 0.25)',
  background: 'linear-gradient(180deg, rgba(15, 39, 84, 0.9) 0%, rgba(8, 22, 53, 0.9) 100%)',
  boxShadow: 'inset 0 0 24px rgba(41, 121, 255, 0.08)',
  overflow: 'hidden',
};

const headerTitleStyle: React.CSSProperties = {
  color: '#e8f3ff',
  fontSize: 16,
  fontWeight: 700,
};

const buildGridTemplate = (splitCount: number) => {
  switch (splitCount) {
    case 1:
      return { columns: '1fr', rows: '1fr' };
    case 4:
      return { columns: 'repeat(2, minmax(0, 1fr))', rows: 'repeat(2, minmax(0, 1fr))' };
    case 6:
      return { columns: '1.35fr 1.35fr 1fr', rows: 'repeat(3, minmax(0, 1fr))' };
    case 9:
      return { columns: 'repeat(3, minmax(0, 1fr))', rows: 'repeat(3, minmax(0, 1fr))' };
    case 16:
      return { columns: 'repeat(4, minmax(0, 1fr))', rows: 'repeat(4, minmax(0, 1fr))' };
    default:
      return { columns: 'repeat(2, minmax(0, 1fr))', rows: 'repeat(2, minmax(0, 1fr))' };
  }
};

const getSlotLayoutStyle = (splitCount: number, index: number): React.CSSProperties | undefined => {
  if (splitCount !== 6) {
    return undefined;
  }
  const layouts: React.CSSProperties[] = [
    { gridColumn: '1 / 3', gridRow: '1 / 3' },
    { gridColumn: '1 / 2', gridRow: '3 / 4' },
    { gridColumn: '2 / 3', gridRow: '3 / 4' },
    { gridColumn: '3 / 4', gridRow: '1 / 2' },
    { gridColumn: '3 / 4', gridRow: '2 / 3' },
    { gridColumn: '3 / 4', gridRow: '3 / 4' },
  ];
  return layouts[index];
};

const sortCameras = (list: VideoCameraRecord[]) =>
  [...list].sort((left, right) => {
    if ((left.online ? 1 : 0) !== (right.online ? 1 : 0)) {
      return (right.online ? 1 : 0) - (left.online ? 1 : 0);
    }
    return left.name.localeCompare(right.name, 'zh-CN');
  });

const normalizeAssignments = (
  currentAssignments: Array<string | undefined>,
  splitCount: number,
  cameras: VideoCameraRecord[],
) => {
  const availableIds = cameras.map((item) => item.id);
  const nextAssignments = currentAssignments
    .slice(0, splitCount)
    .map((item) => (item && availableIds.includes(item) ? item : undefined));
  const usedIds = new Set(nextAssignments.filter(Boolean));
  for (let index = 0; index < splitCount; index += 1) {
    if (nextAssignments[index]) {
      continue;
    }
    const fallback = availableIds.find((id) => !usedIds.has(id));
    if (!fallback) {
      break;
    }
    nextAssignments[index] = fallback;
    usedIds.add(fallback);
  }
  while (nextAssignments.length < splitCount) {
    nextAssignments.push(undefined);
  }
  return nextAssignments;
};

const buildRealtimeDetections = (
  state: VideoMonitorRealtimeCameraState | undefined,
  now: number,
): VideoPlayerOverlayDetection[] => {
  if (!state?.detections?.length || !state.lastInferenceAt || now - state.lastInferenceAt > DETECTION_STALE_MS) {
    return [];
  }
  return state.detections
    .filter((item) => Array.isArray(item.bbox) && item.bbox.length >= 4)
    .map((item, index) => ({
      id: `${item.taskId}-${item.className}-${index}`,
      taskId: item.taskId,
      taskName: item.taskName,
      modelName: item.modelName,
      className: item.className,
      confidence: item.confidence,
      bbox: item.bbox,
      color: DETECTION_COLORS[index % DETECTION_COLORS.length],
    }));
};

const renderCameraLeafTitle = (camera: VideoCameraRecord) => (
  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, paddingLeft: 6 }}>
    <span style={{ color: '#cfe3ff', display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <VideoCameraOutlined style={{ color: camera.online ? '#69b1ff' : 'rgba(255,255,255,0.45)', fontSize: 15 }} />
      <span>{camera.name}</span>
    </span>
    <Badge status={camera.online ? 'success' : 'default'} />
  </div>
);

const normalizeAssetUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`;
};

const createCameraTreeData = (
  catalogs: VideoCatalogRecord[],
  cameras: VideoCameraRecord[],
  keyword: string,
): CameraTreeLeaf[] => {
  const normalizedKeyword = keyword.trim().toLowerCase();
  const cameraMap = new Map<string, VideoCameraRecord[]>();
  const uncategorized: VideoCameraRecord[] = [];
  cameras.forEach((camera) => {
    if (camera.catalogId) {
      const current = cameraMap.get(camera.catalogId) || [];
      current.push(camera);
      cameraMap.set(camera.catalogId, current);
    } else {
      uncategorized.push(camera);
    }
  });

  const buildCatalogNodes = (nodes: VideoCatalogRecord[]): CameraTreeLeaf[] =>
    nodes
      .map((catalog) => {
        const childCatalogs = buildCatalogNodes(catalog.children || []);
        const catalogCameras = (cameraMap.get(catalog.id) || [])
          .filter((camera) =>
            normalizedKeyword
              ? [camera.name, camera.cameraCode, camera.catalogName].filter(Boolean).join(' ').toLowerCase().includes(normalizedKeyword)
              : true,
          )
          .map<CameraTreeLeaf>((camera) => ({
            key: `camera-${camera.id}`,
            title: renderCameraLeafTitle(camera),
            isLeaf: true,
            cameraId: camera.id,
          }));
        const children = [...childCatalogs, ...catalogCameras];
        const catalogMatches = normalizedKeyword
          ? catalog.name.toLowerCase().includes(normalizedKeyword)
          : true;
        if (!catalogMatches && children.length === 0) {
          return undefined;
        }
        return {
          key: `catalog-${catalog.id}`,
          title: (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
              <span style={{ color: '#e8f3ff' }}>{catalog.name}</span>
              <Typography.Text style={{ color: 'rgba(255,255,255,0.45)', fontSize: 12 }}>
                {children.filter((item) => item.cameraId).length}
              </Typography.Text>
            </div>
          ),
          children,
        } as CameraTreeLeaf;
      })
      .filter((item): item is CameraTreeLeaf => Boolean(item));

  const result = buildCatalogNodes(catalogs);
  const uncategorizedNodes = uncategorized
    .filter((camera) =>
      normalizedKeyword
        ? [camera.name, camera.cameraCode].filter(Boolean).join(' ').toLowerCase().includes(normalizedKeyword)
        : true,
    )
    .map<CameraTreeLeaf>((camera) => ({
      key: `camera-${camera.id}`,
      title: renderCameraLeafTitle(camera),
      isLeaf: true,
      cameraId: camera.id,
    }));
  if (uncategorizedNodes.length) {
    result.push({
      key: 'catalog-uncategorized',
      title: <span style={{ color: '#e8f3ff' }}>未分组设备</span>,
      children: uncategorizedNodes,
    });
  }
  return result;
};

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = React.useState(false);
  const [overview, setOverview] = React.useState<VideoModuleOverview>();
  const [catalogTree, setCatalogTree] = React.useState<VideoCatalogRecord[]>([]);
  const [cameras, setCameras] = React.useState<VideoCameraRecord[]>([]);
  const [recentAlarms, setRecentAlarms] = React.useState<VideoAlarmRecord[]>([]);
  const [splitCount, setSplitCount] = React.useState(4);
  const [activeSlotIndex, setActiveSlotIndex] = React.useState(0);
  const [slotAssignments, setSlotAssignments] = React.useState<Array<string | undefined>>([]);
  const [keyword, setKeyword] = React.useState('');
  const [currentTime, setCurrentTime] = React.useState(dayjs());
  const [cameraEndpoints, setCameraEndpoints] = React.useState<Record<string, VideoMediaEndpoint>>({});
  const [endpointErrors, setEndpointErrors] = React.useState<Record<string, string>>({});
  const [realtimeCameraStates, setRealtimeCameraStates] = React.useState<Record<string, VideoMonitorRealtimeCameraState>>({});
  const [runtimeResolved, setRuntimeResolved] = React.useState<Record<string, boolean>>({});
  const [overlayStreamLocks, setOverlayStreamLocks] = React.useState<Record<string, string>>({});
  const [isFullscreen, setIsFullscreen] = React.useState(false);
  const [playingAlarm, setPlayingAlarm] = React.useState<VideoAlarmRecord>();
  const endpointLoadingRef = React.useRef<Set<string>>(new Set());

  const loadDashboard = React.useCallback(async () => {
    setLoading(true);
    try {
      const response = await getVideoMonitorDashboardData();
      const data = response.data;
      const nextCameras = sortCameras(data?.cameras || []);
      setOverview(data?.overview);
      setCatalogTree(data?.catalogTree || []);
      setCameras(nextCameras);
      setRecentAlarms(data?.recentAlarms || []);
      setSlotAssignments((current) => normalizeAssignments(current, splitCount, nextCameras));
    } finally {
      setLoading(false);
    }
  }, [splitCount]);

  React.useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  React.useEffect(() => {
    const timer = window.setInterval(() => setCurrentTime(dayjs()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  React.useEffect(() => {
    const syncFullscreenState = () => {
      setIsFullscreen(Boolean(document.fullscreenElement));
    };
    const requestFullscreen = async () => {
      if (document.fullscreenElement) {
        syncFullscreenState();
        return;
      }
      try {
        await document.documentElement.requestFullscreen();
      } catch {
        syncFullscreenState();
      }
    };
    void requestFullscreen();
    document.addEventListener('fullscreenchange', syncFullscreenState);
    return () => {
      document.removeEventListener('fullscreenchange', syncFullscreenState);
    };
  }, []);

  React.useEffect(() => {
    const timer = window.setInterval(() => {
      void loadDashboard();
    }, 30000);
    return () => window.clearInterval(timer);
  }, [loadDashboard]);

  React.useEffect(() => {
    setSlotAssignments((current) => normalizeAssignments(current, splitCount, cameras));
    setActiveSlotIndex((current) => Math.min(current, Math.max(splitCount - 1, 0)));
  }, [cameras, splitCount]);

  const cameraTreeData = React.useMemo(
    () => createCameraTreeData(catalogTree, cameras, keyword),
    [cameras, catalogTree, keyword],
  );
  const cameraMap = React.useMemo(
    () => new Map(cameras.map((item) => [item.id, item])),
    [cameras],
  );

  const visibleCameraIds = React.useMemo(
    () => Array.from(new Set(slotAssignments.filter((item): item is string => Boolean(item)))),
    [slotAssignments],
  );
  const runtimePollingCameraIds = React.useMemo(
    () => visibleCameraIds.filter((cameraId) => !overlayStreamLocks[cameraId]),
    [overlayStreamLocks, visibleCameraIds],
  );

  React.useEffect(() => {
    if (!visibleCameraIds.length) {
      setRealtimeCameraStates({});
      return undefined;
    }
    if (!runtimePollingCameraIds.length) {
      return undefined;
    }
    let cancelled = false;
    const loadRuntime = async () => {
      try {
        const response = await getVideoMonitorDashboardRuntime(runtimePollingCameraIds);
        if (!cancelled) {
          const nextStates = response.data?.cameraStates || {};
          setRuntimeResolved((current) => {
            const next = { ...current };
            runtimePollingCameraIds.forEach((cameraId) => {
              next[cameraId] = true;
            });
            return next;
          });
          setRealtimeCameraStates((current) => ({
            ...current,
            ...nextStates,
          }));
          setOverlayStreamLocks((current) => {
            const next = { ...current };
            runtimePollingCameraIds.forEach((cameraId) => {
              const state = nextStates[cameraId];
              if (!state?.overlayPlayUrl || state.overlayStatus !== 'RUNNING') {
                return;
              }
              next[cameraId] = state.overlayPlayUrl;
            });
            return next;
          });
        }
      } catch {
        if (!cancelled) {
          setRealtimeCameraStates((current) => current);
        }
      }
    };
    void loadRuntime();
    const timer = window.setInterval(() => {
      void loadRuntime();
    }, 2000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [runtimePollingCameraIds, visibleCameraIds.length]);

  React.useEffect(() => {
    setOverlayStreamLocks((current) => {
      const next: Record<string, string> = {};
      visibleCameraIds.forEach((cameraId) => {
        if (current[cameraId]) {
          next[cameraId] = current[cameraId];
        }
      });
      return next;
    });
    setRealtimeCameraStates((current) => {
      const next: Record<string, VideoMonitorRealtimeCameraState> = {};
      visibleCameraIds.forEach((cameraId) => {
        if (current[cameraId]) {
          next[cameraId] = current[cameraId];
        }
      });
      return next;
    });
    setRuntimeResolved((current) => {
      const next: Record<string, boolean> = {};
      visibleCameraIds.forEach((cameraId) => {
        if (current[cameraId]) {
          next[cameraId] = current[cameraId];
        }
      });
      return next;
    });
  }, [visibleCameraIds]);

  React.useEffect(() => {
    visibleCameraIds.forEach((cameraId) => {
      if (cameraEndpoints[cameraId] || endpointLoadingRef.current.has(cameraId)) {
        return;
      }
      endpointLoadingRef.current.add(cameraId);
      getVideoMediaEndpoint(cameraId)
        .then((response) => {
          setCameraEndpoints((current) => ({
            ...current,
            [cameraId]: response.data,
          }));
          setEndpointErrors((current) => {
            const next = { ...current };
            delete next[cameraId];
            return next;
          });
        })
        .catch((error) => {
          setEndpointErrors((current) => ({
            ...current,
            [cameraId]: (error as Error).message || '播放地址加载失败',
          }));
        })
        .finally(() => {
          endpointLoadingRef.current.delete(cameraId);
        });
    });
  }, [cameraEndpoints, visibleCameraIds]);

  const assignCameraToActiveSlot = React.useCallback(
    (cameraId: string) => {
      setSlotAssignments((current) => {
        const next = normalizeAssignments(current, splitCount, cameras);
        const duplicateIndex = next.findIndex((item, index) => index !== activeSlotIndex && item === cameraId);
        const currentValue = next[activeSlotIndex];
        next[activeSlotIndex] = cameraId;
        if (duplicateIndex > -1) {
          next[duplicateIndex] = currentValue && currentValue !== cameraId ? currentValue : undefined;
        }
        return next;
      });
    },
    [activeSlotIndex, cameras, splitCount],
  );

  const gridTemplate = React.useMemo(() => buildGridTemplate(splitCount), [splitCount]);
  const resolvedScreenStyle = React.useMemo<React.CSSProperties>(
    () =>
      isFullscreen
        ? {
            ...screenStyle,
            position: 'fixed',
            inset: 0,
            zIndex: 1000,
            minHeight: '100vh',
            margin: 0,
            padding: 16,
          }
        : screenStyle,
    [isFullscreen],
  );

  const handleExitOrEnterFullscreen = React.useCallback(async () => {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
      return;
    }
    try {
      await document.documentElement.requestFullscreen();
    } catch {
      // ignore browser restrictions and keep current page usable
    }
  }, []);

  return (
    <div style={resolvedScreenStyle}>
      <Spin spinning={loading}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: isFullscreen ? 'calc(100vh - 32px)' : 'calc(100vh - 80px)' }}>
          <div
            style={{
              ...panelStyle,
              padding: '14px 18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <ClockCircleOutlined style={{ color: '#76b8ff' }} />
              <Typography.Text style={{ color: '#d9ebff', fontSize: 18, fontWeight: 600 }}>
                {currentTime.format('YYYY年MM月DD日 dddd HH:mm:ss')}
              </Typography.Text>
            </div>
            <Typography.Text style={{ color: '#f0f7ff', fontSize: 28, fontWeight: 700, letterSpacing: 2 }}>
              视频监控平台
            </Typography.Text>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Button icon={<ReloadOutlined />} onClick={() => void loadDashboard()}>
                刷新
              </Button>
              {isFullscreen ? (
                <Button type="primary" icon={<RollbackOutlined />} onClick={() => void handleExitOrEnterFullscreen()}>
                  返回
                </Button>
              ) : (
                <Button type="primary" icon={<FullscreenOutlined />} onClick={() => void handleExitOrEnterFullscreen()}>
                  全屏
                </Button>
              )}
            </div>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '260px minmax(0, 1fr) 280px',
              gap: 16,
              flex: 1,
              minHeight: 0,
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: 0 }}>
              <div style={{ ...panelStyle, padding: 14 }}>
                <div style={{ ...headerTitleStyle, marginBottom: 12 }}>全局总览</div>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                    gap: 10,
                  }}
                >
                  {[
                    { label: '告警数量', value: overview?.alarmCount || 0, icon: <AlertOutlined />, color: '#ff7875' },
                    { label: '摄像头数量', value: overview?.cameraCount || 0, icon: <CameraOutlined />, color: '#69b1ff' },
                    { label: '算法数量', value: overview?.taskCount || 0, icon: <PartitionOutlined />, color: '#73d13d' },
                    { label: '模型数量', value: overview?.modelCount || 0, icon: <ApartmentOutlined />, color: '#ffd666' },
                  ].map((item) => (
                    <div
                      key={item.label}
                      style={{
                        borderRadius: 12,
                        border: '1px solid rgba(102, 163, 255, 0.18)',
                        background: 'rgba(14, 34, 78, 0.78)',
                        padding: '16px 12px',
                        textAlign: 'center',
                      }}
                    >
                      <div
                        style={{
                          width: 42,
                          height: 42,
                          margin: '0 auto 10px',
                          borderRadius: '50%',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          background: `${item.color}22`,
                          color: item.color,
                          fontSize: 18,
                        }}
                      >
                        {item.icon}
                      </div>
                      <div style={{ color: 'rgba(255,255,255,0.72)', fontSize: 12 }}>{item.label}</div>
                      <div style={{ color: '#fff', fontSize: 28, fontWeight: 700, lineHeight: 1.2 }}>{item.value}</div>
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ ...panelStyle, display: 'flex', flexDirection: 'column', minHeight: 0, flex: 1 }}>
                <div
                  style={{
                    padding: '14px 14px 10px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    borderBottom: '1px solid rgba(102, 163, 255, 0.14)',
                  }}
                >
                  <div style={headerTitleStyle}>设备目录</div>
                  <Tag color="processing" style={{ marginInlineEnd: 0 }}>
                    {cameras.length} 个设备
                  </Tag>
                </div>
                <div style={{ padding: 14 }}>
                  <Input
                    allowClear
                    className="monitor-tree-search"
                    value={keyword}
                    prefix={<SearchOutlined />}
                    placeholder="搜索设备 / 目录"
                    onChange={(event) => setKeyword(event.target.value)}
                    style={{
                      background: 'rgba(7, 23, 55, 0.96)',
                      color: '#d9ebff',
                      borderColor: 'rgba(82, 147, 255, 0.28)',
                    }}
                  />
                </div>
                <div style={{ flex: 1, minHeight: 0, overflow: 'auto', padding: '0 8px 12px 8px' }}>
                  {cameraTreeData.length ? (
                    <>
                      <Tree
                        className="monitor-device-tree"
                        defaultExpandAll
                        selectedKeys={slotAssignments[activeSlotIndex] ? [`camera-${slotAssignments[activeSlotIndex]}`] : []}
                        treeData={cameraTreeData}
                        onSelect={(_, info) => {
                          const leaf = info.node as CameraTreeLeaf;
                          if (leaf.cameraId) {
                            assignCameraToActiveSlot(leaf.cameraId);
                          }
                        }}
                        style={{
                          background: 'transparent',
                          color: '#d9ebff',
                        }}
                      />
                      <style>{`
                        .monitor-tree-search.ant-input-affix-wrapper {
                          background: rgba(7, 23, 55, 0.96) !important;
                          border-color: rgba(82, 147, 255, 0.28) !important;
                          color: #d9ebff !important;
                        }
                        .monitor-tree-search.ant-input-affix-wrapper .ant-input {
                          background: transparent !important;
                          color: #d9ebff !important;
                        }
                        .monitor-tree-search.ant-input-affix-wrapper .ant-input::placeholder,
                        .monitor-tree-search.ant-input-affix-wrapper .ant-input-prefix,
                        .monitor-tree-search.ant-input-affix-wrapper .ant-input-suffix {
                          color: rgba(217, 235, 255, 0.72) !important;
                        }
                        .monitor-tree-search.ant-input-affix-wrapper-focused,
                        .monitor-tree-search.ant-input-affix-wrapper:hover {
                          border-color: rgba(82, 147, 255, 0.52) !important;
                          box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12) !important;
                        }
                        .ant-tree {
                          background: transparent !important;
                          color: #d9ebff !important;
                        }
                        .monitor-device-tree .ant-tree-treenode {
                          padding: 2px 0 !important;
                        }
                        .monitor-device-tree .ant-tree-indent-unit {
                          width: 18px !important;
                        }
                        .ant-tree .ant-tree-node-content-wrapper,
                        .ant-tree .ant-tree-title,
                        .ant-tree .ant-tree-switcher {
                          color: #d9ebff !important;
                        }
                        .ant-tree .ant-tree-node-content-wrapper:hover {
                          background: rgba(56, 143, 255, 0.18) !important;
                        }
                        .ant-tree .ant-tree-node-selected,
                        .ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
                          background: rgba(56, 143, 255, 0.26) !important;
                        }
                      `}</style>
                    </>
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无设备" />
                  )}
                </div>
              </div>
            </div>

            <div style={{ ...panelStyle, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
              <div
                style={{
                  padding: '14px 16px 12px',
                  borderBottom: '1px solid rgba(102, 163, 255, 0.14)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 16,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                  <div style={headerTitleStyle}>实时监控</div>
                  <Typography.Text style={{ color: 'rgba(255,255,255,0.72)' }}>
                    {currentTime.format('YYYY-MM-DD HH:mm:ss')}
                  </Typography.Text>
                  <Typography.Text style={{ color: 'rgba(255,255,255,0.72)' }}>
                    {slotAssignments[activeSlotIndex] ? `已选 ${cameraMap.get(slotAssignments[activeSlotIndex] || '')?.name || ''}` : '未选择设备'}
                  </Typography.Text>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  {SPLIT_OPTIONS.map((item) => (
                    <Button
                      key={item}
                      type={item === splitCount ? 'primary' : 'default'}
                      size="small"
                      onClick={() => setSplitCount(item)}
                    >
                      {item}分屏
                    </Button>
                  ))}
                </div>
              </div>

              <div style={{ flex: 1, minHeight: 0, padding: 12, display: 'flex', flexDirection: 'column', gap: 12 }}>
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: gridTemplate.columns,
                    gridTemplateRows: gridTemplate.rows,
                    gap: 10,
                    flex: 1,
                    minHeight: 0,
                  }}
                >
                  {Array.from({ length: splitCount }).map((_, index) => {
                    const cameraId = slotAssignments[index];
                    const camera = cameraId ? cameraMap.get(cameraId) : undefined;
                    const endpoint = cameraId ? cameraEndpoints[cameraId] : undefined;
                    const realtimeState = cameraId ? realtimeCameraStates[cameraId] : undefined;
                    const hasRuntimeResolved = cameraId
                      ? Boolean(overlayStreamLocks[cameraId] || runtimeResolved[cameraId])
                      : false;
                    const overlayStreamSrc = cameraId ? overlayStreamLocks[cameraId] : undefined;
                    const useOverlayStream = Boolean(overlayStreamSrc);
                    const fallbackSrc = hasRuntimeResolved ? endpoint?.playUrl || camera?.playUrl || camera?.streamUrl : undefined;
                    const playerSrc = (useOverlayStream ? overlayStreamSrc : undefined) || fallbackSrc;
                    const detectionBoxes: VideoPlayerOverlayDetection[] = useOverlayStream
                      ? []
                      : buildRealtimeDetections(realtimeState, currentTime.valueOf());
                    return (
                      <div key={`slot-${index}`} style={{ minHeight: 0, ...getSlotLayoutStyle(splitCount, index) }}>
                        <VideoPlayerOverlay
                          src={playerSrc}
                          title={camera?.name || `窗口 ${index + 1}`}
                          subtitle={camera ? [camera.cameraCode, camera.catalogName].filter(Boolean).join(' / ') : undefined}
                          detections={detectionBoxes}
                          viewBoxWidth={Math.max(Number(realtimeState?.frameWidth) || 1280, 1)}
                          viewBoxHeight={Math.max(Number(realtimeState?.frameHeight) || 720, 1)}
                          selected={index === activeSlotIndex}
                          onClick={() => setActiveSlotIndex(index)}
                          minHeight={splitCount >= 9 ? 180 : splitCount >= 6 ? 220 : 260}
                          emptyText={undefined}
                          controls={Boolean(camera)}
                          autoHideControls={Boolean(camera)}
                          extra={
                            camera ? (
                              <Tag color={camera.online ? 'success' : 'default'} style={{ marginInlineEnd: 0 }}>
                                {camera.online ? '在线' : '离线'}
                              </Tag>
                            ) : undefined
                          }
                          footer={
                            endpointErrors[cameraId || ''] ? (
                              <Typography.Text style={{ color: '#ffccc7', fontSize: 12 }}>
                                {endpointErrors[cameraId || '']}
                              </Typography.Text>
                            ) : detectionBoxes.length ? (
                              <Typography.Text style={{ color: 'rgba(255,255,255,0.72)', fontSize: 12 }}>
                                {detectionBoxes
                                  .map((item) => [item.taskName, item.className || item.modelName].filter(Boolean).join(' / '))
                                  .join(' · ')}
                              </Typography.Text>
                            ) : useOverlayStream ? (
                              <Typography.Text style={{ color: 'rgba(255,255,255,0.72)', fontSize: 12 }}>
                                AI监测中
                              </Typography.Text>
                            ) : !hasRuntimeResolved ? (
                              <Typography.Text style={{ color: 'rgba(255,255,255,0.72)', fontSize: 12 }}>
                                正在优先探测算法流...
                              </Typography.Text>
                            ) : realtimeState?.lastError ? (
                              <Typography.Text style={{ color: '#ffd591', fontSize: 12 }}>{realtimeState.lastError}</Typography.Text>
                            ) : null
                          }
                        />
                      </div>
                    );
                  })}
                </div>

                <div style={{ ...panelStyle, minHeight: 132 }}>
                  <div
                    style={{
                      padding: '10px 14px',
                      borderBottom: '1px solid rgba(102, 163, 255, 0.14)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                    }}
                  >
                    <div style={headerTitleStyle}>告警图像</div>
                    <Typography.Text style={{ color: 'rgba(255,255,255,0.65)' }}>
                      共 {recentAlarms.length} 条
                    </Typography.Text>
                  </div>
                  <div style={{ padding: 12 }}>
                    {recentAlarms.length ? (
                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))',
                            gap: 12,
                          }}
                        >
                          {recentAlarms.map((item) => {
                            const thumbnailUrl = normalizeAssetUrl(item.recordThumbnailUrl);
                            const recordUrl = normalizeAssetUrl(item.recordFileUrl);
                            return (
                            <div
                              key={item.id}
                              style={{
                                borderRadius: 10,
                                overflow: 'hidden',
                                background: 'rgba(11, 26, 56, 0.9)',
                                border: '1px solid rgba(102, 163, 255, 0.14)',
                              }}
                            >
                              {thumbnailUrl ? (
                                <div
                                  onClick={() => {
                                    if (recordUrl) {
                                      setPlayingAlarm(item);
                                    }
                                  }}
                                  style={{
                                    cursor: recordUrl ? 'pointer' : 'default',
                                  }}
                                >
                                  <Image
                                    preview={false}
                                    src={thumbnailUrl}
                                    alt={item.recordFileName || item.eventContent || item.eventType}
                                    style={{ width: '100%', height: 84, objectFit: 'cover' }}
                                  />
                                </div>
                              ) : (
                                <div style={{ height: 84, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={false} />
                                </div>
                              )}
                              <div style={{ padding: 8 }}>
                                <Typography.Text
                                  ellipsis
                                  style={{ display: 'block', color: '#fff', width: '100%', fontSize: 12 }}
                                >
                                  {item.cameraName || item.eventType}
                                </Typography.Text>
                                <Typography.Text style={{ color: 'rgba(255,255,255,0.55)', fontSize: 11 }}>
                                  {item.alarmTime ? dayjs(item.alarmTime).format('MM-DD HH:mm:ss') : '-'}
                                </Typography.Text>
                              </div>
                            </div>
                            );
                          })}
                        </div>
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无告警图像" />
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: 0 }}>
              <div style={{ ...panelStyle, padding: '14px 16px', textAlign: 'center' }}>
                <div style={headerTitleStyle}>告警事件</div>
                <div style={{ marginTop: 8, color: '#d9ebff' }}>
                  今日告警 <span style={{ color: '#ff7875', fontSize: 28, fontWeight: 700 }}>{overview?.todayAlarmCount || 0}</span> 次
                </div>
              </div>

              <div style={{ ...panelStyle, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                <div
                  style={{
                    padding: '14px 16px 10px',
                    borderBottom: '1px solid rgba(102, 163, 255, 0.14)',
                  }}
                >
                  <div style={headerTitleStyle}>最新告警</div>
                </div>
                <div style={{ flex: 1, minHeight: 0, overflow: 'auto', padding: '8px 12px 12px' }}>
                  {recentAlarms.length ? (
                    <List
                      split={false}
                      dataSource={recentAlarms}
                      renderItem={(item) => (
                        <List.Item style={{ paddingInline: 0 }}>
                          <div
                            style={{
                              width: '100%',
                              borderRadius: 12,
                              padding: 12,
                              background: 'rgba(12, 29, 64, 0.88)',
                              border: '1px solid rgba(102, 163, 255, 0.12)',
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10 }}>
                              <Typography.Text style={{ color: '#fff', fontWeight: 600 }}>{item.eventType}</Typography.Text>
                              <Tag color={item.handleStatus === 'PENDING' ? 'error' : 'processing'} style={{ marginInlineEnd: 0 }}>
                                {item.handleStatus}
                              </Tag>
                            </div>
                            <Typography.Paragraph
                              ellipsis={{ rows: 2 }}
                              style={{
                                color: 'rgba(255,255,255,0.72)',
                                marginTop: 8,
                                marginBottom: 8,
                              }}
                            >
                              {item.eventContent || '暂无告警描述'}
                            </Typography.Paragraph>
                            <Typography.Text style={{ color: 'rgba(255,255,255,0.5)', fontSize: 12 }}>
                              {item.alarmTime ? dayjs(item.alarmTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
                            </Typography.Text>
                          </div>
                        </List.Item>
                      )}
                    />
                  ) : (
                    <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无告警信息" />
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </Spin>
      <Modal
        title={playingAlarm ? `告警录像 - ${playingAlarm.cameraName || playingAlarm.eventType}` : '告警录像'}
        open={Boolean(playingAlarm)}
        footer={null}
        width={960}
        destroyOnHidden
        onCancel={() => setPlayingAlarm(undefined)}
      >
        {playingAlarm?.recordFileUrl ? (
          <VideoPlayer
            src={normalizeAssetUrl(playingAlarm.recordFileUrl)}
            sourceType="auto"
            style={{ width: '100%', height: 520 }}
          />
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无告警录像" />
        )}
      </Modal>
    </div>
  );
};

export default DashboardPage;
