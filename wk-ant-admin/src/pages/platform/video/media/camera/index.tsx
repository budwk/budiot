import {
  AppstoreOutlined,
  CameraOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  FileImageOutlined,
  FolderAddOutlined,
  InfoCircleOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  StopOutlined,
  UnorderedListOutlined,
  VideoCameraAddOutlined,
  UserOutlined,
  LockOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import {
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Pagination,
  Radio,
  Row,
  Select,
  Spin,
  Space,
  Switch,
  Tag,
  Tree,
  TreeSelect,
  Typography,
  message,
} from 'antd';
import dayjs from 'dayjs';
import * as React from 'react';
import PlatformProTable from '@/components/PlatformProTable';
import TableRowActions from '@/components/TableRowActions';
import VideoPlayer from '@/components/VideoPlayer';
import {
  createVideoCatalog,
  deleteVideoCatalog,
  getVideoCatalogDetail,
  updateVideoCatalog,
} from '@/services/budiot/video/catalog';
import { clearVideoMediaFiles, deleteVideoMediaFile, getVideoMediaFilePage } from '@/services/budiot/video/media-file';
import {
  createVideoCamera,
  deleteVideoCamera,
  getVideoCameraDetail,
  getVideoCameraMeta,
  getVideoCameraPage,
  updateVideoCamera,
} from '@/services/budiot/video/camera';
import { stopVideoGb28181Live } from '@/services/budiot/video/gb28181';
import {
  getVideoMediaEndpoint,
  controlVideoPtz,
  enableVideoRtspProxy,
  scanOnvifDevices,
  testOnvifConnection,
  snapshotVideoCamera,
  startVideoRecord,
  stopVideoPtz,
  stopVideoRecord,
} from '@/services/budiot/video/media';
import { getVideoMonitorDashboardRuntime } from '@/services/budiot/video/monitor-dashboard';
import type {
  VideoCameraMeta,
  VideoCameraRecord,
  VideoCatalogRecord,
  VideoMediaEndpoint,
  VideoMediaFileRecord,
  VideoMonitorRealtimeCameraState,
  VideoOnvifScanResult,
} from '@/services/budiot/video/typing';

type CameraFormValues = Partial<VideoCameraRecord>;
type CatalogFormValues = Partial<VideoCatalogRecord>;
type CatalogTreeNode = VideoCatalogRecord & {
  key: string;
  title: string;
  children?: CatalogTreeNode[];
  isAll?: boolean;
};
type CatalogSelectNode = {
  title: string;
  value: string;
  children?: CatalogSelectNode[];
};
type OnvifAddFormValues = {
  username: string;
  password: string;
};
type CameraViewMode = 'table' | 'card';

const CAMERA_TYPE_LABELS: Record<string, string> = {
  custom: '自定义',
  hikvision: '海康',
  dahua: '大华',
  tplink: 'TPLINK',
};

const defaultFormValues: CameraFormValues = {
  cameraCode: '',
  name: '',
  protocolType: 'RTSP',
  cameraType: 'custom',
  vendor: '',
  modelName: '',
  serialNo: '',
  macAddress: '',
  firmwareVersion: '',
  hardwareId: '',
  host: '',
  port: 554,
  username: 'admin',
  password: '',
  channelCode: '',
  streamIndex: 0,
  streamUrl: '',
  onvifDeviceUrl: '',
  gb28181DeviceId: '',
  gb28181Password: '',
  gb28181Domain: '',
  gb28181DeviceIp: '',
  gb28181DevicePort: undefined,
  protocolConfig: '',
  longitude: '',
  latitude: '',
  address: '',
  description: '',
  supportMove: false,
  supportZoom: false,
  enabled: true,
};

const buildRtspUrl = (cameraType?: string, host?: string, port?: number, username?: string, password?: string, streamIndex?: number) => {
  if (!host || !cameraType || cameraType === 'custom') {
    return '';
  }
  const auth = username ? `${encodeURIComponent(username)}:${encodeURIComponent(password || '')}@` : '';
  const actualPort = port || 554;
  const subIndex = streamIndex === 1 ? 1 : 0;
  if (cameraType === 'hikvision') {
    return `rtsp://${auth}${host}:${actualPort}/Streaming/Channels/${subIndex === 0 ? '101' : '102'}`;
  }
  if (cameraType === 'dahua') {
    return `rtsp://${auth}${host}:${actualPort}/cam/realmonitor?channel=1&subtype=${subIndex}`;
  }
  if (cameraType === 'tplink') {
    return `rtsp://${auth}${host}:${actualPort}/stream${subIndex + 1}`;
  }
  return '';
};

const parseRtspAddress = (value?: string): Partial<CameraFormValues> | undefined => {
  if (!value || !value.startsWith('rtsp://')) {
    return undefined;
  }
  try {
    const url = new URL(value);
    const path = `${url.pathname}${url.search}`.toLowerCase();
    let parsedStreamIndex = 0;
    if (
      path.includes('subtype=1') ||
      path.includes('/102') ||
      path.includes('stream2') ||
      path.includes('substream')
    ) {
      parsedStreamIndex = 1;
    }
    return {
      host: url.hostname || '',
      port: url.port ? Number(url.port) : 554,
      username: decodeURIComponent(url.username || ''),
      password: decodeURIComponent(url.password || ''),
      streamIndex: parsedStreamIndex,
    };
  } catch (error) {
    return undefined;
  }
};

const inferCameraType = (vendor?: string) => {
  const value = (vendor || '').toLowerCase();
  if (value.includes('hik')) return 'hikvision';
  if (value.includes('dahua')) return 'dahua';
  if (value.includes('tp') || value.includes('tplink')) return 'tplink';
  return 'custom';
};

const normalizeAssetUrl = (url?: string) => {
  if (!url) {
    return '';
  }
  if (/^https?:\/\//i.test(url) || url.startsWith('/api/')) {
    return url;
  }
  return url.startsWith('/') ? `/api${url}` : `/api/${url}`;
};

const resolveGb28181Session = (camera?: VideoCameraRecord) => {
  const deviceId = camera?.gb28181DeviceId || camera?.channelCode || camera?.cameraCode;
  const channelId = camera?.channelCode;
  if (!deviceId || !channelId) {
    return undefined;
  }
  return { deviceId, channelId };
};

const isManagedProxyStreamUrl = (cameraCode?: string, streamUrl?: string) => {
  const normalizedStreamUrl = (streamUrl || '').trim().toLowerCase();
  const normalizedCameraCode = (cameraCode || '').trim().toLowerCase();
  return normalizedStreamUrl.startsWith('rtsp://') && normalizedCameraCode
    ? normalizedStreamUrl.includes(`/live/${normalizedCameraCode}`)
    : false;
};

const renderGb28181RegisterTag = (status?: string) => {
  if (status === 'REGISTERED') {
    return <Tag color="success">已注册</Tag>;
  }
  if (status === 'UNREGISTERED') {
    return <Tag>未注册</Tag>;
  }
  return <Tag color="processing">{status || '未知'}</Tag>;
};

const buildCatalogSelectNodes = (nodes: VideoCatalogRecord[]): CatalogSelectNode[] =>
  nodes.map((node) => ({
    title: node.name,
    value: node.id,
    children: node.children?.length ? buildCatalogSelectNodes(node.children) : undefined,
  }));

const collectCatalogDescendantIds = (nodes: VideoCatalogRecord[], targetId?: string): Set<string> => {
  const descendants = new Set<string>();

  const visit = (nodeList: VideoCatalogRecord[], include = false) => {
    for (const node of nodeList) {
      const shouldInclude = include || node.id === targetId;
      if (shouldInclude) {
        descendants.add(node.id);
      }
      if (node.children?.length) {
        visit(node.children, shouldInclude);
      }
    }
  };

  if (targetId) {
    visit(nodes);
  }

  return descendants;
};

const filterCatalogSelectNodes = (nodes: CatalogSelectNode[], excludedIds: Set<string>): CatalogSelectNode[] =>
  nodes
    .filter((node) => !excludedIds.has(node.value))
    .map((node) => ({
      ...node,
      children: node.children ? filterCatalogSelectNodes(node.children, excludedIds) : undefined,
    }));

const CameraPage: React.FC = () => {
  const access = useAccess();
  const { modal } = App.useApp();
  const actionRef = React.useRef<ActionType>(null);
  const assetActionRef = React.useRef<ActionType>(null);
  const [form] = Form.useForm<CameraFormValues>();
  const [catalogForm] = Form.useForm<CatalogFormValues>();
  const [onvifAddForm] = Form.useForm<OnvifAddFormValues>();
  const [meta, setMeta] = React.useState<VideoCameraMeta>({
    protocols: [],
    cameraTypes: [],
    streamIndexes: [],
    catalogs: [],
  });
  const [editingId, setEditingId] = React.useState<string>();
  const [catalogEditingId, setCatalogEditingId] = React.useState<string>();
  const [modalOpen, setModalOpen] = React.useState(false);
  const [catalogModalOpen, setCatalogModalOpen] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [endpointOpen, setEndpointOpen] = React.useState(false);
  const [endpoint, setEndpoint] = React.useState<VideoMediaEndpoint>();
  const [ptzLoading, setPtzLoading] = React.useState<string>();
  const [playerOpen, setPlayerOpen] = React.useState(false);
  const [playerCamera, setPlayerCamera] = React.useState<VideoCameraRecord>();
  const [playerError, setPlayerError] = React.useState<string>();
  const [playerRuntimeState, setPlayerRuntimeState] = React.useState<VideoMonitorRealtimeCameraState>();
  const [playerUseOverlayStream, setPlayerUseOverlayStream] = React.useState(false);
  const [scanOpen, setScanOpen] = React.useState(false);
  const [scanLoading, setScanLoading] = React.useState(false);
  const [scanResults, setScanResults] = React.useState<VideoOnvifScanResult[]>([]);
  const [scanIpRange, setScanIpRange] = React.useState('');
  const [onvifAddOpen, setOnvifAddOpen] = React.useState(false);
  const [onvifAddLoading, setOnvifAddLoading] = React.useState(false);
  const [selectedOnvifDevice, setSelectedOnvifDevice] = React.useState<VideoOnvifScanResult>();
  const [assetDrawerOpen, setAssetDrawerOpen] = React.useState(false);
  const [assetDrawerTitle, setAssetDrawerTitle] = React.useState('');
  const [assetMediaType, setAssetMediaType] = React.useState<'SNAPSHOT' | 'RECORD'>('SNAPSHOT');
  const [assetCamera, setAssetCamera] = React.useState<VideoCameraRecord>();
  const [detailOpen, setDetailOpen] = React.useState(false);
  const [detailCamera, setDetailCamera] = React.useState<VideoCameraRecord>();
  const [activeGb28181Session, setActiveGb28181Session] = React.useState<{ deviceId: string; channelId: string }>();
  const [selectedCatalog, setSelectedCatalog] = React.useState<VideoCatalogRecord>();
  const [hoveredCatalogId, setHoveredCatalogId] = React.useState<string>();
  const [viewMode, setViewMode] = React.useState<CameraViewMode>('table');
  const [tableData, setTableData] = React.useState<VideoCameraRecord[]>([]);
  const [pageInfo, setPageInfo] = React.useState({ current: 1, pageSize: 10, total: 0 });

  const protocolType = Form.useWatch('protocolType', form);
  const cameraType = Form.useWatch('cameraType', form);
  const host = Form.useWatch('host', form);
  const port = Form.useWatch('port', form);
  const username = Form.useWatch('username', form);
  const password = Form.useWatch('password', form);
  const streamIndex = Form.useWatch('streamIndex', form);
  const streamUrlValue = Form.useWatch('streamUrl', form);

  const loadMeta = React.useCallback(async () => {
    const response = await getVideoCameraMeta();
    setMeta(response.data);
  }, []);

  const requestCameras = React.useCallback(async (params: Record<string, any>) => {
    const response = await getVideoCameraPage({
      catalogId: selectedCatalog?.id,
      cameraCode: params.cameraCode,
      name: params.name,
      protocolType: params.protocolType,
      vendor: params.vendor,
      pageNo: params.current,
      pageSize: params.pageSize,
    });
    const list = response.data?.list || [];
    setTableData(list);
    setPageInfo({
      current: params.current || 1,
      pageSize: params.pageSize || 10,
      total: response.data?.totalCount || 0,
    });
    return {
      data: list,
      total: response.data?.totalCount || 0,
      success: true,
    };
  }, [selectedCatalog?.id]);

  React.useEffect(() => {
    loadMeta().catch(() => undefined);
  }, [loadMeta]);

  React.useEffect(() => {
    if (!modalOpen) {
      return;
    }
    if (protocolType === 'RTSP' && cameraType && cameraType !== 'custom') {
      form.setFieldValue('streamUrl', buildRtspUrl(cameraType, host, port, username, password, streamIndex));
    }
    if (protocolType === 'ONVIF' && host && !form.getFieldValue('onvifDeviceUrl')) {
      form.setFieldValue('onvifDeviceUrl', `http://${host}:${port || 80}/onvif/device_service`);
    }
  }, [cameraType, form, host, modalOpen, password, port, protocolType, streamIndex, username]);

  React.useEffect(() => {
    if (!modalOpen || protocolType !== 'RTSP' || cameraType !== 'custom' || !streamUrlValue) {
      return;
    }
    const parsed = parseRtspAddress(streamUrlValue);
    if (!parsed) {
      return;
    }
    form.setFieldsValue(parsed);
  }, [cameraType, form, modalOpen, protocolType, streamUrlValue]);

  React.useEffect(() => {
    if (!modalOpen || protocolType !== 'ONVIF' || editingId) {
      return;
    }

    if (!form.getFieldValue('supportMove')) {
      form.setFieldValue('supportMove', true);
    }
  }, [editingId, form, modalOpen, protocolType]);

  const catalogTreeNodes = React.useMemo<CatalogTreeNode[]>(
    () => {
      const allNode: CatalogTreeNode = {
        id: 'all',
        name: '全部',
        key: 'all',
        title: '全部',
        isAll: true,
      } as CatalogTreeNode;

      const children = meta.catalogs.map(function normalize(node): CatalogTreeNode {
        return {
          ...node,
          key: node.id,
          title: node.name,
          children: node.children?.map(normalize),
        };
      });

      return [allNode, ...children];
    },
    [meta.catalogs],
  );

  const catalogSelectTreeData = React.useMemo<CatalogSelectNode[]>(
    () => buildCatalogSelectNodes(meta.catalogs),
    [meta.catalogs],
  );

  const catalogParentTreeData = React.useMemo<CatalogSelectNode[]>(
    () => filterCatalogSelectNodes(catalogSelectTreeData, collectCatalogDescendantIds(meta.catalogs, catalogEditingId)),
    [catalogEditingId, catalogSelectTreeData, meta.catalogs],
  );

  const openCreate = () => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({
      ...defaultFormValues,
      catalogId: selectedCatalog?.id,
    });
    setModalOpen(true);
  };

  const openCreateFromScan = (record: VideoOnvifScanResult) => {
    setEditingId(undefined);
    form.resetFields();
    form.setFieldsValue({
      ...defaultFormValues,
      catalogId: selectedCatalog?.id,
      protocolType: 'ONVIF',
      cameraType: inferCameraType(record.vendor),
      cameraCode: record.serialNo || `${record.host}-${record.port || 80}`,
      name: record.name || `${record.vendor || 'ONVIF'}-${record.host}`,
      vendor: record.vendor,
      modelName: record.modelName,
      serialNo: record.serialNo,
      macAddress: record.mac,
      firmwareVersion: record.firmwareVersion,
      hardwareId: record.hardwareId,
      host: record.host,
      port: record.port || 80,
      streamUrl: record.rtspUrl,
      onvifDeviceUrl: record.onvifDeviceUrl,
      supportMove: true,
      supportZoom: record.supportZoom,
      protocolConfig: JSON.stringify(
        {
          onvifDeviceUrl: record.onvifDeviceUrl,
          supportMove: true,
          supportZoom: record.supportZoom,
        },
        null,
        2,
      ),
    });
    setScanOpen(false);
    setModalOpen(true);
  };

  // 新增：从扫描结果添加ONVIF设备（先验证用户名密码，再添加）
  const addOnvifCamera = async (values: OnvifAddFormValues) => {
    if (!selectedOnvifDevice) {
      message.warning('请先选择要添加的设备');
      return;
    }

    setOnvifAddLoading(true);
    try {
      // 先测试连接
      const testResult = await testOnvifConnection({
        host: selectedOnvifDevice.host,
        port: selectedOnvifDevice.port || 80,
        username: values.username,
        password: values.password,
      });

      if (!testResult.data?.success) {
        message.error('连接失败：' + (testResult.data?.message || testResult.msg || '未知错误'));
        return;
      }

      // 连接成功，添加设备
        const cameraData = {
          ...defaultFormValues,
          catalogId: selectedCatalog?.id,
          protocolType: 'ONVIF',
        cameraType: inferCameraType(selectedOnvifDevice.vendor),
        cameraCode: selectedOnvifDevice.serialNo || `${selectedOnvifDevice.host}-${selectedOnvifDevice.port || 80}`,
        name: selectedOnvifDevice.name || `${selectedOnvifDevice.vendor || 'ONVIF'}-${selectedOnvifDevice.host}`,
        vendor: selectedOnvifDevice.vendor,
        modelName: selectedOnvifDevice.modelName,
        serialNo: selectedOnvifDevice.serialNo,
        macAddress: selectedOnvifDevice.mac,
        firmwareVersion: selectedOnvifDevice.firmwareVersion,
        hardwareId: selectedOnvifDevice.hardwareId,
          host: selectedOnvifDevice.host,
          port: selectedOnvifDevice.port || 80,
          streamUrl: selectedOnvifDevice.rtspUrl,
          onvifDeviceUrl: selectedOnvifDevice.onvifDeviceUrl,
          username: values.username,
          password: values.password,
          supportMove: true,
          supportZoom: selectedOnvifDevice.supportZoom,
        protocolConfig: JSON.stringify(
          {
            onvifDeviceUrl: selectedOnvifDevice.onvifDeviceUrl,
            supportMove: true,
            supportZoom: selectedOnvifDevice.supportZoom,
          },
          null,
          2,
        ),
      };

      await createVideoCamera(cameraData);
      message.success('设备添加成功');
      setOnvifAddOpen(false);
      setSelectedOnvifDevice(undefined);
      setScanOpen(false);
      onvifAddForm.resetFields();
      actionRef.current?.reload();
    } catch (error) {
      message.error('添加失败：' + (error as Error).message);
    } finally {
      setOnvifAddLoading(false);
    }
  };

  const openEdit = async (record: VideoCameraRecord) => {
    const response = await getVideoCameraDetail(record.id);
    setEditingId(record.id);
    form.resetFields();
    form.setFieldsValue({
      ...defaultFormValues,
      ...response.data,
    });
    setModalOpen(true);
  };

  const openEndpoint = async (record: VideoCameraRecord) => {
    if (record.protocolType === 'GB28181' && !resolveGb28181Session(record)) {
      message.error('GB28181 摄像头缺少设备编码或通道编码，无法播放');
      return;
    }
    const [endpointResponse, runtimeResponse] = await Promise.all([
      getVideoMediaEndpoint(record.id),
      getVideoMonitorDashboardRuntime([record.id]).catch(() => undefined),
    ]);
    const gb28181Session = record.protocolType === 'GB28181'
      && !isManagedProxyStreamUrl(record.cameraCode, endpointResponse.data?.streamUrl)
      ? resolveGb28181Session(record)
      : undefined;
    const runtimeState = runtimeResponse?.data?.cameraStates?.[record.id];
    const useOverlayStream = Boolean(runtimeState?.overlayPlayUrl && runtimeState.overlayStatus === 'RUNNING');
    setPlayerCamera(record);
    setEndpoint(endpointResponse.data);
    setPlayerRuntimeState(runtimeState);
    setPlayerUseOverlayStream(useOverlayStream);
    setActiveGb28181Session(gb28181Session);
    setPlayerError(undefined);
    setPlayerOpen(true);
  };

  const closePlayer = React.useCallback(async () => {
    const session = activeGb28181Session;
    setPlayerOpen(false);
    setPlayerCamera(undefined);
    setEndpoint(undefined);
    setPlayerError(undefined);
    setPlayerRuntimeState(undefined);
    setPlayerUseOverlayStream(false);
    setActiveGb28181Session(undefined);
    if (!session) {
      return;
    }
    try {
      await stopVideoGb28181Live(session);
    } catch (error) {
      message.warning(`关闭 GB28181 预览失败：${(error as Error).message}`);
    }
  }, [activeGb28181Session]);

  React.useEffect(() => {
    if (playerOpen) {
      return;
    }
    setPlayerRuntimeState(undefined);
    setPlayerUseOverlayStream(false);
    setActiveGb28181Session(undefined);
  }, [playerOpen]);

  const playerSrc =
    (playerUseOverlayStream ? playerRuntimeState?.overlayPlayUrl : undefined) || endpoint?.playUrl;

  const openDetail = async (record: VideoCameraRecord) => {
    const response = await getVideoCameraDetail(record.id);
    setDetailCamera(response.data);
    setDetailOpen(true);
  };

  const openAssetDrawer = (record: VideoCameraRecord, mediaType: 'SNAPSHOT' | 'RECORD') => {
    setAssetCamera(record);
    setAssetMediaType(mediaType);
    setAssetDrawerTitle(`${record.name} - ${mediaType === 'SNAPSHOT' ? '图册' : '录像'}`);
    setAssetDrawerOpen(true);
    setTimeout(() => assetActionRef.current?.reload(), 0);
  };

  const downloadAssetFile = React.useCallback((url?: string, fileName?: string) => {
    const normalizedUrl = normalizeAssetUrl(url);
    if (!normalizedUrl) {
      return;
    }
    const link = document.createElement('a');
    link.href = normalizedUrl;
    link.download = fileName || '';
    link.target = '_blank';
    link.rel = 'noreferrer';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, []);

  const handleSnapshot = async (record: VideoCameraRecord) => {
    await snapshotVideoCamera(record.id);
  };

  const handleEnableRtspProxy = async (record: VideoCameraRecord) => {
    const response = await enableVideoRtspProxy(record.id);
    if (response.data?.streamUrl || response.data?.playUrl || response.data?.rtmpUrl) {
      message.success(response.data?.message || '已启用 RTSP 转发');
    }
    actionRef.current?.reload();
  };

  const handleStartRecord = async (record: VideoCameraRecord) => {
    await startVideoRecord(record.id);
  };

  const handleStopRecord = async (record: VideoCameraRecord) => {
    await stopVideoRecord(record.id);
  };

  const cameraActions = (record: VideoCameraRecord) => [
    {
      key: 'proxy',
      label: '启用 RTSP 转发',
      icon: <VideoCameraAddOutlined />,
      onClick: async () => handleEnableRtspProxy(record),
    },
    { key: 'endpoint', label: '播放', icon: <PlayCircleOutlined />, onClick: () => openEndpoint(record) },
    { key: 'album', label: '图册', icon: <FileImageOutlined />, onClick: () => openAssetDrawer(record, 'SNAPSHOT') },
    { key: 'recordAssets', label: '录像', icon: <PlayCircleOutlined />, onClick: () => openAssetDrawer(record, 'RECORD') },
    { key: 'detail', label: '详情', icon: <InfoCircleOutlined />, onClick: () => openDetail(record) },
    {
      key: 'edit',
      label: '修改',
      icon: <EditOutlined />,
      hidden: !access.hasPermission?.('video.manage.media.camera'),
      onClick: () => openEdit(record),
    },
    {
      key: 'delete',
      label: '删除',
      danger: true,
      icon: <DeleteOutlined />,
      hidden: !access.hasPermission?.('video.manage.media.camera'),
      onClick: () => {
        modal.confirm({
          title: '确认删除该摄像头吗？',
          onOk: async () => {
            await deleteVideoCamera(record.id);
            actionRef.current?.reload();
          },
        });
      },
    },
  ];

  const openCreateCatalog = () => {
    setCatalogEditingId(undefined);
    catalogForm.resetFields();
    catalogForm.setFieldsValue({
      parentId: selectedCatalog?.id || '',
      name: '',
      description: '',
    });
    setCatalogModalOpen(true);
  };

  const openEditCatalog = async (record?: VideoCatalogRecord) => {
    const target = record || selectedCatalog;
    if (!target) return;
    const response = await getVideoCatalogDetail(target.id);
    setCatalogEditingId(target.id);
    catalogForm.setFieldsValue(response.data);
    setCatalogModalOpen(true);
  };

  const handleDeleteCatalog = (record: VideoCatalogRecord) => {
    modal.confirm({
      title: '确认删除该目录吗？',
      onOk: async () => {
        await deleteVideoCatalog(record.id);
        if (selectedCatalog?.id === record.id) {
          setSelectedCatalog(undefined);
        }
        actionRef.current?.reload();
        await loadMeta();
      },
    });
  };

  const submitForm = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateVideoCamera({ ...values, id: editingId });
      } else {
        await createVideoCamera(values);
      }
      setModalOpen(false);
      actionRef.current?.reload();
      await loadMeta();
    } finally {
      setSubmitting(false);
    }
  };

  const submitCatalog = async () => {
    const values = await catalogForm.validateFields();
    if (catalogEditingId) {
      await updateVideoCatalog({ ...values, id: catalogEditingId });
    } else {
      await createVideoCatalog(values);
    }
    setCatalogModalOpen(false);
    await loadMeta();
  };

  const submitScan = async () => {
    if (!scanIpRange.trim()) {
      message.warning('请输入IP地址');
      return;
    }
    setScanLoading(true);
    try {
      const response = await scanOnvifDevices({ ipRange: scanIpRange.trim() });
      setScanResults(response.data || []);
    } catch (error) {
      message.error('扫描失败：' + (error as Error).message);
    } finally {
      setScanLoading(false);
    }
  };

  const columns = React.useMemo<ProColumns<VideoCameraRecord>[]>(
    () => [
      { title: '摄像头编码', dataIndex: 'cameraCode' },
      { title: '摄像头名称', dataIndex: 'name' },
      { title: '目录', dataIndex: 'catalogName', search: false },
      {
        title: '协议',
        dataIndex: 'protocolType',
        valueType: 'select',
        fieldProps: {
          options: meta.protocols.map((item) => ({ label: item.text, value: item.value })),
          allowClear: true,
        },
      },
      {
        title: '类型',
        dataIndex: 'cameraType',
        search: false,
        render: (_, record) => CAMERA_TYPE_LABELS[record.cameraType || ''] || record.cameraType || '-',
      },
      { title: '厂商', dataIndex: 'vendor' },
      {
        title: '在线',
        dataIndex: 'online',
        search: false,
        render: (_, record) => (record.online ? <Tag color="success">在线</Tag> : <Tag color="default">离线</Tag>),
      },
      {
        title: '启用',
        dataIndex: 'enabled',
        search: false,
        render: (_, record) => (record.enabled ? <Tag color="processing">启用</Tag> : <Tag color="error">禁用</Tag>),
      },
      {
        title: '操作',
        valueType: 'option',
        width: 320,
        render: (_, record) => (
          <TableRowActions
            actions={cameraActions(record)}
          />
        ),
      },
    ],
    [access, meta.protocols, modal],
  );

  return (
    <PageContainer title={false}>
      <Row gutter={16}>
        <Col span={4}>
          <Card
            title="目录分类"
            extra={
              <Button size="small" icon={<FolderAddOutlined />} onClick={openCreateCatalog} disabled={!access.hasPermission?.('video.manage.media.catalog')}>
                新增
              </Button>
            }
          >
            <Tree
              treeData={catalogTreeNodes}
              selectedKeys={selectedCatalog ? [selectedCatalog.id] : []}
              titleRender={(nodeData) => {
                const node = nodeData as CatalogTreeNode;
                const showActions =
                  hoveredCatalogId === node.id &&
                  !node.isAll &&
                  access.hasPermission?.('video.manage.media.catalog');

                return (
                  <div
                    style={{ display: 'flex', alignItems: 'center', minWidth: 0 }}
                    onMouseEnter={() => setHoveredCatalogId(node.id)}
                    onMouseLeave={() => setHoveredCatalogId((current) => (current === node.id ? undefined : current))}
                  >
                    <span style={{ flex: '0 1 auto' }}>{node.title}</span>
                    {showActions ? (
                      <Space size={4} style={{ marginLeft: 16, flexShrink: 0 }}>
                        <Button
                          type="text"
                          size="small"
                          icon={<EditOutlined />}
                          onMouseDown={(event) => {
                            event.preventDefault();
                            event.stopPropagation();
                          }}
                          onClick={(event) => {
                            event.stopPropagation();
                            void openEditCatalog(node);
                          }}
                        />
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onMouseDown={(event) => {
                            event.preventDefault();
                            event.stopPropagation();
                          }}
                          onClick={(event) => {
                            event.stopPropagation();
                            handleDeleteCatalog(node);
                          }}
                        />
                      </Space>
                    ) : null}
                  </div>
                );
              }}
              onSelect={(keys, info) => {
                const node = info.node as CatalogTreeNode;
                if (node.isAll) {
                  setSelectedCatalog(undefined);
                } else {
                  setSelectedCatalog(keys.length ? (node as unknown as VideoCatalogRecord) : undefined);
                }
                actionRef.current?.reload();
              }}
            />
          </Card>
        </Col>
        <Col span={20}>
          <PlatformProTable<VideoCameraRecord, Record<string, any>>
            actionRef={actionRef}
            persistenceKey="video-camera-table"
            rowKey="id"
            headerTitle={
              <Space>
                <Typography.Text strong>摄像头管理</Typography.Text>
                <Typography.Text type="secondary">
                  {selectedCatalog ? `当前目录：${selectedCatalog.name}` : '全部目录'}
                </Typography.Text>
              </Space>
            }
            columns={columns}
            request={requestCameras}
            toolBarRender={() => [
              <Button
                key="scan"
                icon={<SearchOutlined />}
                onClick={() => {
                  setScanResults([]);
                  setScanOpen(true);
                }}
              >
                扫描局域网 ONVIF
              </Button>,
              <Button key="create" type="primary" icon={<PlusOutlined />} onClick={openCreate}>
                新增摄像头
              </Button>,
              <Radio.Group
                key="view-mode"
                value={viewMode}
                onChange={(event) => setViewMode(event.target.value)}
                optionType="button"
                buttonStyle="solid"
                options={[
                  {
                    label: (
                      <Space size={6}>
                        <UnorderedListOutlined />
                        列表
                      </Space>
                    ),
                    value: 'table',
                  },
                  {
                    label: (
                      <Space size={6}>
                        <AppstoreOutlined />
                        平铺
                      </Space>
                    ),
                    value: 'card',
                  },
                ]}
              />,
            ]}
            tableRender={(_, defaultDom, domList) =>
              viewMode === 'table' ? (
                defaultDom
              ) : (
                <>
                  {domList.toolbar}
                  {domList.alert}
                  <Card>
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
                        gap: 16,
                      }}
                    >
                      {tableData.map((record) => (
                        <Card
                          key={record.id}
                          hoverable
                          styles={{ body: { padding: 18 } }}
                        >
                          <Space direction="vertical" size={14} style={{ width: '100%' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                              <div>
                                <Typography.Title level={5} style={{ margin: 0 }}>
                                  {record.name}
                                </Typography.Title>
                                <Typography.Text type="secondary">{record.cameraCode}</Typography.Text>
                              </div>
                              <Space direction="vertical" size={8} align="end">
                                <Tag color={record.online ? 'success' : 'default'}>{record.online ? '在线' : '离线'}</Tag>
                                <Tag color={record.enabled ? 'blue' : 'default'}>{record.enabled ? '已启用' : '已停用'}</Tag>
                              </Space>
                            </div>
                            <Descriptions size="small" column={1}>
                              <Descriptions.Item label="目录">{record.catalogName || '全部目录'}</Descriptions.Item>
                              <Descriptions.Item label="协议">{record.protocolType || '-'}</Descriptions.Item>
                              <Descriptions.Item label="类型">{CAMERA_TYPE_LABELS[record.cameraType || ''] || record.cameraType || '-'}</Descriptions.Item>
                              <Descriptions.Item label="地址">
                                {record.host || '-'}{record.port ? `:${record.port}` : ''}
                              </Descriptions.Item>
                              <Descriptions.Item label="厂商型号">
                                {[record.vendor, record.modelName].filter(Boolean).join(' / ') || '-'}
                              </Descriptions.Item>
                            </Descriptions>
                            <Space wrap>
                              {cameraActions(record)
                                .filter((item) => !item.hidden)
                                .map((item) => (
                                  <Button
                                    key={item.key}
                                    type="link"
                                    size="small"
                                    danger={item.danger}
                                    icon={item.icon}
                                    style={{ paddingInline: 0 }}
                                    onClick={() => void item.onClick?.()}
                                  >
                                    {item.label}
                                  </Button>
                                ))}
                            </Space>
                          </Space>
                        </Card>
                      ))}
                    </div>
                    <Pagination
                      style={{ marginTop: 16, textAlign: 'right' }}
                      current={pageInfo.current}
                      pageSize={pageInfo.pageSize}
                      total={pageInfo.total}
                      showSizeChanger
                      onChange={(current, pageSize) => {
                        actionRef.current?.setPageInfo?.({ current, pageSize });
                      }}
                    />
                  </Card>
                </>
              )
            }
          />
        </Col>
      </Row>

      <Modal
        title={catalogEditingId ? '修改目录' : '新增目录'}
        open={catalogModalOpen}
        onCancel={() => setCatalogModalOpen(false)}
        onOk={() => void submitCatalog()}
      >
        <Form form={catalogForm} layout="vertical">
          <Form.Item name="parentId" label="父目录">
            <TreeSelect allowClear treeData={catalogParentTreeData} placeholder="请选择父目录" treeDefaultExpandAll />
          </Form.Item>
          <Form.Item name="name" label="目录名称" rules={[{ required: true, message: '请输入目录名称' }]}>
            <Input maxLength={120} />
          </Form.Item>
          <Form.Item name="description" label="目录描述">
            <Input.TextArea rows={3} maxLength={255} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingId ? '编辑摄像头' : '新增摄像头'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => void submitForm()}
        confirmLoading={submitting}
        width={980}
      >
        <Form form={form} layout="vertical" initialValues={defaultFormValues}>
          <Divider>基础信息</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="catalogId" label="所属目录">
                <TreeSelect allowClear treeData={catalogSelectTreeData} placeholder="请选择所属目录" treeDefaultExpandAll />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="protocolType" label="接入协议" rules={[{ required: true, message: '请选择接入协议' }]}>
                <Select options={meta.protocols.map((item) => ({ label: item.text, value: item.value }))} />
              </Form.Item>
            </Col>
            {(protocolType === 'RTSP' || protocolType === 'ONVIF') ? (
              <Col span={8}>
                <Form.Item name="cameraType" label="摄像头类型">
                  <Select
                    allowClear
                    options={meta.cameraTypes.map((item) => ({
                      label: item.text,
                      value: item.value,
                    }))}
                  />
                </Form.Item>
              </Col>
            ) : null}
            <Col span={8}>
              <Form.Item name="name" label="设备名称" rules={[{ required: true, message: '请输入设备名称' }]}>
                <Input placeholder="请输入设备名称" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="enabled" label="是否启用" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
          </Row>

          <Divider>接入信息</Divider>
          <Row gutter={16}>
            {(protocolType === 'ONVIF' || (protocolType === 'RTSP' && cameraType !== 'custom')) ? (
              <>
                <Col span={8}>
                  <Form.Item name="host" label="设备 IP" rules={[{ required: true, message: '请输入设备IP' }]}>
                    <Input placeholder="如 192.168.1.100" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="port" label="端口" rules={[{ required: true, message: '请输入端口' }]}>
                    <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="streamIndex" label="码流类型">
                    <Select
                      options={meta.streamIndexes.map((item) => ({
                        label: item.text,
                        value: item.value,
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="username" label="用户名">
                    <Input />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="password" label="密码">
                    <Input.Password />
                  </Form.Item>
                </Col>
              </>
            ) : null}
            {protocolType === 'GB28181' ? (
              <>
                <Col span={8}>
                  <Form.Item
                    name="gb28181DeviceId"
                    label="设备国标编码"
                    rules={[{ required: true, message: '请输入 GB28181 设备编码' }]}
                  >
                    <Input placeholder="如 34020000001320000001" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="gb28181Domain" label="所属域">
                    <Input placeholder="如 3402000000" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="gb28181Password" label="SIP 密码">
                    <Input.Password placeholder="未填写则使用系统默认值" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item
                    name="channelCode"
                    label="通道编码"
                    rules={[{ required: true, message: '请输入通道编码' }]}
                  >
                    <Input placeholder="请输入 GB28181 通道编码" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="gb28181DeviceIp" label="注册设备 IP">
                    <Input placeholder="由注册同步回填，也可手工补充" />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="gb28181DevicePort" label="注册设备端口">
                    <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </>
            ) : null}
            {protocolType === 'ONVIF' ? (
              <Col span={24}>
                <Form.Item name="onvifDeviceUrl" label="ONVIF 设备地址">
                  <Input placeholder="如 http://ip:port/onvif/device_service" />
                </Form.Item>
              </Col>
            ) : null}
            {protocolType === 'RTSP' ? (
              <Col span={24}>
                <Form.Item
                  name="streamUrl"
                  label="RTSP 接入地址"
                  rules={[{ required: true, message: '请输入或生成 RTSP 接入地址' }]}
                >
                  <Input
                    placeholder={
                      cameraType && cameraType !== 'custom'
                        ? '将根据摄像头类型自动生成'
                        : '请输入完整 RTSP 地址'
                    }
                    disabled={cameraType !== 'custom'}
                  />
                </Form.Item>
              </Col>
            ) : null}
            {protocolType === 'ONVIF' ? (
              <Col span={24}>
                <Form.Item name="streamUrl" label="发现到的 RTSP 地址">
                  <Input placeholder="扫描导入时自动带入，也可手工补充" />
                </Form.Item>
              </Col>
            ) : null}
            {protocolType === 'ONVIF' ? (
              <Col span={8}>
                <Form.Item name="supportMove" label="支持云台" valuePropName="checked">
                  <Switch />
                </Form.Item>
              </Col>
            ) : null}
          </Row>

          <Divider>位置信息</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="longitude" label="经度">
                <Input placeholder="如 118.7969" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="latitude" label="纬度">
                <Input placeholder="如 32.0603" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="address" label="地址">
                <Input placeholder="请输入摄像头安装地址" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      <Modal
        title={playerCamera ? `视频播放 - ${playerCamera.name}` : '视频播放'}
        open={playerOpen}
        onCancel={() => {
          void closePlayer();
        }}
        width={1180}
        footer={[
          <Button
            key="close"
            onClick={() => {
              void closePlayer();
            }}
          >
            关闭
          </Button>,
        ]}
        destroyOnHidden
      >
        <Row gutter={16}>
          <Col span={16}>
            <div
              style={{
                minHeight: 480,
              }}
            >
              <VideoPlayer
                src={playerOpen ? playerSrc : undefined}
                sourceType="auto"
                isLive
                muted
                onError={(message) => {
                  setPlayerError(message);
                }}
                style={{ minHeight: 480, height: 480 }}
              />
            </div>
            {playerError ? (
              <Typography.Text type="danger" style={{ display: 'block', marginTop: 12 }}>
                {playerError}
              </Typography.Text>
            ) : null}
          </Col>
          <Col span={8}>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="摄像头编码">{endpoint?.cameraCode || '-'}</Descriptions.Item>
              <Descriptions.Item label="当前播放源">{playerUseOverlayStream ? '算法推流' : '原始播放流'}</Descriptions.Item>
              <Descriptions.Item label="流地址">{endpoint?.streamUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="HTTP 播放">{endpoint?.playUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="算法推流">{playerRuntimeState?.overlayPlayUrl || '-'}</Descriptions.Item>
              <Descriptions.Item label="RTMP 播放">{endpoint?.rtmpUrl || '-'}</Descriptions.Item>
              {endpoint?.protocolType === 'ONVIF' && (endpoint.supportMove || endpoint.supportZoom) ? (
                <Descriptions.Item label="云台控制">
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                    <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
                        alignItems: 'center',
                        justifyItems: 'center',
                        gap: 8,
                      }}
                    >
                      <Button
                        style={{ gridColumn: 2, minWidth: 72 }}
                        loading={ptzLoading === 'UP'}
                        onClick={async () => {
                          if (!endpoint?.cameraId) return;
                          setPtzLoading('UP');
                          try {
                            await controlVideoPtz(endpoint.cameraId, 'UP');
                          } finally {
                            setPtzLoading(undefined);
                          }
                        }}
                      >
                        上
                      </Button>
                      <Button
                        style={{ gridColumn: 1, gridRow: 2, minWidth: 72 }}
                        loading={ptzLoading === 'LEFT'}
                        onClick={async () => {
                          if (!endpoint?.cameraId) return;
                          setPtzLoading('LEFT');
                          try {
                            await controlVideoPtz(endpoint.cameraId, 'LEFT');
                          } finally {
                            setPtzLoading(undefined);
                          }
                        }}
                      >
                        左
                      </Button>
                      <Button
                        danger
                        style={{ gridColumn: 2, gridRow: 2, minWidth: 72 }}
                        loading={ptzLoading === 'STOP'}
                        onClick={async () => {
                          if (!endpoint?.cameraId) return;
                          setPtzLoading('STOP');
                          try {
                            await stopVideoPtz(endpoint.cameraId);
                          } finally {
                            setPtzLoading(undefined);
                          }
                        }}
                      >
                        停止
                      </Button>
                      <Button
                        style={{ gridColumn: 3, gridRow: 2, minWidth: 72 }}
                        loading={ptzLoading === 'RIGHT'}
                        onClick={async () => {
                          if (!endpoint?.cameraId) return;
                          setPtzLoading('RIGHT');
                          try {
                            await controlVideoPtz(endpoint.cameraId, 'RIGHT');
                          } finally {
                            setPtzLoading(undefined);
                          }
                        }}
                      >
                        右
                      </Button>
                      <Button
                        style={{ gridColumn: 2, gridRow: 3, minWidth: 72 }}
                        loading={ptzLoading === 'DOWN'}
                        onClick={async () => {
                          if (!endpoint?.cameraId) return;
                          setPtzLoading('DOWN');
                          try {
                            await controlVideoPtz(endpoint.cameraId, 'DOWN');
                          } finally {
                            setPtzLoading(undefined);
                          }
                        }}
                      >
                        下
                      </Button>
                    </div>
                    {endpoint.supportZoom ? (
                      <Space>
                        <Button
                          loading={ptzLoading === 'ZOOM_IN'}
                          onClick={async () => {
                            if (!endpoint?.cameraId) return;
                            setPtzLoading('ZOOM_IN');
                            try {
                              await controlVideoPtz(endpoint.cameraId, 'ZOOM_IN');
                            } finally {
                              setPtzLoading(undefined);
                            }
                          }}
                        >
                          放大
                        </Button>
                        <Button
                          loading={ptzLoading === 'ZOOM_OUT'}
                          onClick={async () => {
                            if (!endpoint?.cameraId) return;
                            setPtzLoading('ZOOM_OUT');
                            try {
                              await controlVideoPtz(endpoint.cameraId, 'ZOOM_OUT');
                            } finally {
                              setPtzLoading(undefined);
                            }
                          }}
                        >
                          缩小
                        </Button>
                      </Space>
                    ) : null}
                  </div>
                </Descriptions.Item>
              ) : null}
            </Descriptions>
          </Col>
        </Row>
      </Modal>

      <Drawer title="摄像头详情" open={detailOpen} onClose={() => setDetailOpen(false)} width={760}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="设备编码">{detailCamera?.cameraCode || '-'}</Descriptions.Item>
          <Descriptions.Item label="设备名称">{detailCamera?.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="所属目录">{detailCamera?.catalogName || '-'}</Descriptions.Item>
          <Descriptions.Item label="接入协议">{detailCamera?.protocolType || '-'}</Descriptions.Item>
          <Descriptions.Item label="摄像头类型">
            {CAMERA_TYPE_LABELS[detailCamera?.cameraType || ''] || detailCamera?.cameraType || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="码流类型">
            {detailCamera?.streamIndex === 1 ? '子码流' : '主码流'}
          </Descriptions.Item>
          <Descriptions.Item label="制造商">{detailCamera?.vendor || '-'}</Descriptions.Item>
          <Descriptions.Item label="设备型号">{detailCamera?.modelName || '-'}</Descriptions.Item>
          <Descriptions.Item label="序列号">{detailCamera?.serialNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="MAC 地址">{detailCamera?.macAddress || '-'}</Descriptions.Item>
          <Descriptions.Item label="固件版本">{detailCamera?.firmwareVersion || '-'}</Descriptions.Item>
          <Descriptions.Item label="硬件 ID">{detailCamera?.hardwareId || '-'}</Descriptions.Item>
          <Descriptions.Item label="设备 IP">{detailCamera?.host || '-'}</Descriptions.Item>
          <Descriptions.Item label="端口">{detailCamera?.port || '-'}</Descriptions.Item>
          <Descriptions.Item label="在线状态">
            {detailCamera?.online ? <Tag color="success">在线</Tag> : <Tag>离线</Tag>}
          </Descriptions.Item>
          <Descriptions.Item label="启用状态">
            {detailCamera?.enabled ? <Tag color="processing">启用</Tag> : <Tag color="error">禁用</Tag>}
          </Descriptions.Item>
          {detailCamera?.protocolType === 'GB28181' ? (
            <>
              <Descriptions.Item label="设备国标编码">{detailCamera?.gb28181DeviceId || '-'}</Descriptions.Item>
              <Descriptions.Item label="注册状态">
                {renderGb28181RegisterTag(detailCamera?.gb28181RegisterStatus)}
              </Descriptions.Item>
              <Descriptions.Item label="所属域">{detailCamera?.gb28181Domain || '-'}</Descriptions.Item>
              <Descriptions.Item label="通道编码">{detailCamera?.channelCode || '-'}</Descriptions.Item>
              <Descriptions.Item label="注册设备 IP">{detailCamera?.gb28181DeviceIp || '-'}</Descriptions.Item>
              <Descriptions.Item label="注册设备端口">{detailCamera?.gb28181DevicePort || '-'}</Descriptions.Item>
            </>
          ) : null}
          <Descriptions.Item label="支持云台">{detailCamera?.supportMove ? '支持' : '不支持'}</Descriptions.Item>
          <Descriptions.Item label="支持变焦">{detailCamera?.supportZoom ? '支持' : '不支持'}</Descriptions.Item>
          {detailCamera?.protocolType === 'ONVIF' ? (
            <Descriptions.Item label="ONVIF 地址" span={2}>
              {detailCamera?.onvifDeviceUrl || '-'}
            </Descriptions.Item>
          ) : null}
          <Descriptions.Item label="接入地址" span={2}>
            {detailCamera?.streamUrl || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="播放地址" span={2}>
            {detailCamera?.playUrl || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="经度">{detailCamera?.longitude || '-'}</Descriptions.Item>
          <Descriptions.Item label="纬度">{detailCamera?.latitude || '-'}</Descriptions.Item>
          <Descriptions.Item label="安装位置" span={2}>
            {detailCamera?.address || '-'}
          </Descriptions.Item>
          <Descriptions.Item label="协议配置" span={2}>
            <Typography.Text style={{ whiteSpace: 'pre-wrap' }}>{detailCamera?.protocolConfig || '-'}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="备注" span={2}>
            {detailCamera?.description || '-'}
          </Descriptions.Item>
        </Descriptions>
      </Drawer>

      <Drawer title={assetDrawerTitle} open={assetDrawerOpen} onClose={() => setAssetDrawerOpen(false)} width={"60%"} destroyOnClose>
        <PlatformProTable<VideoMediaFileRecord, Record<string, any>>
          actionRef={assetActionRef}
          persistenceKey={`video-camera-${assetMediaType.toLowerCase()}-assets-table`}
          rowKey="id"
          columns={[
            { title: '文件名称', dataIndex: 'fileName', search: false },
            {
              title: '缩略图',
              dataIndex: 'thumbnailUrl',
              search: false,
              render: (_, record) => {
                const previewImageUrl =
                  assetMediaType === 'RECORD' ? normalizeAssetUrl(record.thumbnailUrl) : normalizeAssetUrl(record.fileUrl);
                if (previewImageUrl) {
                  return (
                    <img
                      src={previewImageUrl}
                      alt="缩略图"
                      style={{ width: 80, height: 45, objectFit: 'cover', borderRadius: 4, cursor: 'pointer' }}
                      onClick={() => {
                        modal.info({
                          title: '图片预览',
                          content: <img src={previewImageUrl} alt="预览" style={{ maxWidth: '100%', maxHeight: '70vh' }} />,
                          width: 800,
                        });
                      }}
                    />
                  );
                }
                return '-';
              },
            },
            { title: '来源', dataIndex: 'sourceType', search: false, width: 100 },
            {
              title: '业务时间',
              dataIndex: 'bizTime',
              search: false,
              width: 180,
              render: (_, record) => (record.bizTime ? dayjs(record.bizTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
            },
            {
              title: '文件大小',
              dataIndex: 'fileSize',
              search: false,
              width: 100,
              render: (_, record) => {
                if (!record.fileSize) return '-';
                const sizeInMB = record.fileSize / (1024 * 1024);
                return sizeInMB >= 1 ? `${sizeInMB.toFixed(2)} MB` : `${(record.fileSize / 1024).toFixed(2)} KB`;
              },
            },
            {
              title: '时长',
              dataIndex: 'duration',
              search: false,
              width: 100,
              render: (_, record) => {
                if (assetMediaType === 'SNAPSHOT') return '-';
                if (!record.duration) return '-';
                const minutes = Math.floor(record.duration / 60);
                const seconds = record.duration % 60;
                return `${minutes}:${seconds.toString().padStart(2, '0')}`;
              },
            },
            {
              title: '文件地址',
              dataIndex: 'fileUrl',
              search: false,
              ellipsis: true,
              render: (_, record) =>
                record.fileUrl ? (
                  <a href={normalizeAssetUrl(record.fileUrl)} target="_blank" rel="noreferrer">
                    {assetMediaType === 'SNAPSHOT' ? '查看图片' : '播放/下载'}
                  </a>
                ) : (
                  '-'
                ),
            },
            {
              title: '操作',
              valueType: 'option',
              search: false,
              width: 180,
              render: (_, record) => (
                <TableRowActions
                  actions={[
                    ...(assetMediaType === 'RECORD'
                      ? [
                          {
                            key: 'play',
                            label: '播放',
                            onClick: () => {
                              if (record.fileUrl) {
                                window.open(normalizeAssetUrl(record.fileUrl), '_blank', 'noopener,noreferrer');
                              }
                            },
                          },
                          {
                            key: 'download',
                            label: '下载',
                            onClick: () => downloadAssetFile(record.fileUrl, record.fileName),
                          },
                        ]
                      : []),
                    {
                      key: 'delete',
                      label: '删除',
                      danger: true,
                      onClick: () => {
                        modal.confirm({
                          title: '确认删除该媒体文件吗？',
                          onOk: async () => {
                            await deleteVideoMediaFile(record.id);
                            assetActionRef.current?.reload();
                          },
                        });
                      },
                    },
                  ]}
                />
              ),
            },
          ]}
          request={async (params) => {
            if (!assetCamera?.id) {
              return { data: [], total: 0, success: true };
            }
            const response = await getVideoMediaFilePage({
              cameraId: assetCamera.id,
              mediaType: assetMediaType,
              fileName: params.fileName,
              pageNo: params.current,
              pageSize: params.pageSize,
            });
            return {
              data: response.data?.list || [],
              total: response.data?.totalCount || 0,
              success: true,
            };
          }}
          toolBarRender={() => [
            <Button
              key="clear"
              danger
              disabled={!assetCamera?.id}
              onClick={() => {
                if (!assetCamera?.id) {
                  return;
                }
                modal.confirm({
                  title: `确认清空当前${assetMediaType === 'SNAPSHOT' ? '图册' : '录像'}吗？`,
                  onOk: async () => {
                    await clearVideoMediaFiles({ cameraId: assetCamera.id, mediaType: assetMediaType });
                    assetActionRef.current?.reload();
                  },
                });
              }}
            >
              清空
            </Button>,
          ]}
        />
      </Drawer>

      <Modal
        title="扫描 ONVIF 摄像头"
        open={scanOpen}
        onCancel={() => {
          setScanOpen(false);
          setScanIpRange('');
          setScanResults([]);
        }}
        footer={[
          <Button key="close" onClick={() => {
            setScanOpen(false);
            setScanIpRange('');
            setScanResults([]);
          }}>
            关闭
          </Button>,
        ]}
        width={920}
      >
        <Row gutter={16}>
          <Col span={18}>
            <Input
              placeholder="请输入IP地址，如 10.10.10.10"
              value={scanIpRange}
              onChange={(e) => setScanIpRange(e.target.value)}
              onPressEnter={() => void submitScan()}
              disabled={scanLoading}
            />
          </Col>
          <Col span={6}>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={() => void submitScan()}
              loading={scanLoading}
              disabled={!scanIpRange.trim()}
              block
            >
              扫描
            </Button>
          </Col>
        </Row>

        <Divider />

        {scanResults.length > 0 && (
          <div>
            <Typography.Text strong>扫描结果 ({scanResults.length} 台)</Typography.Text>
            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              {scanResults.map((item) => (
                <Col
                  xs={24}
                  md={12}
                  key={`${item.host}:${item.port}`}
                >
                  <Card
                    size="small"
                    title={
                      <Space size={8} wrap>
                        <Typography.Text strong>{item.name || `${item.host}:${item.port}`}</Typography.Text>
                        <Tag color="blue">{item.host}:{item.port}</Tag>
                      </Space>
                    }
                    extra={(
                      <Button
                        size="small"
                        type="primary"
                        onClick={() => {
                          setSelectedOnvifDevice(item);
                          setOnvifAddOpen(true);
                        }}
                      >
                        添加
                      </Button>
                    )}
                  >
                    <Space direction="vertical" size={10} style={{ width: '100%' }}>
                      <div>
                        <Typography.Text type="secondary">厂商 / 型号</Typography.Text>
                        <div>{[item.vendor, item.modelName, item.serialNo].filter(Boolean).join(' / ') || '-'}</div>
                      </div>
                      <div>
                        <Typography.Text type="secondary">ONVIF</Typography.Text>
                        <div>{item.onvifDeviceUrl || '-'}</div>
                      </div>
                      <div>
                        <Typography.Text type="secondary">RTSP</Typography.Text>
                        <div>{item.rtspUrl || '-'}</div>
                      </div>
                      <div>
                        <Typography.Text type="secondary">截图</Typography.Text>
                        <div>{item.snapshotUrl || '-'}</div>
                      </div>
                      <Space size={[8, 8]} wrap>
                        <Tag color={item.supportMove ? 'success' : 'default'}>
                          PTZ {item.supportMove ? '支持' : '不支持'}
                        </Tag>
                        <Tag color={item.supportZoom ? 'success' : 'default'}>
                          Zoom {item.supportZoom ? '支持' : '不支持'}
                        </Tag>
                      </Space>
                    </Space>
                  </Card>
                </Col>
              ))}
            </Row>
          </div>
        )}

        {scanLoading && scanResults.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Space direction="vertical" size={16}>
              <Spin size="large" />
              <Typography.Text type="secondary">正在扫描 ONVIF 设备...</Typography.Text>
            </Space>
          </div>
        )}

        {!scanLoading && scanResults.length === 0 && scanIpRange.trim() && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Typography.Text type="secondary">未发现可用 ONVIF 设备</Typography.Text>
          </div>
        )}
      </Modal>

      <Modal
        title="添加 ONVIF 摄像头"
        open={onvifAddOpen}
        onCancel={() => {
          setOnvifAddOpen(false);
          setSelectedOnvifDevice(undefined);
          onvifAddForm.resetFields();
        }}
        onOk={() => {
          onvifAddForm.validateFields().then((values) => {
            void addOnvifCamera(values);
          });
        }}
        confirmLoading={onvifAddLoading}
        width={600}
      >
        <Form<OnvifAddFormValues>
          form={onvifAddForm}
          layout="vertical"
        >
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="设备地址">{selectedOnvifDevice?.host}:{selectedOnvifDevice?.port}</Descriptions.Item>
            <Descriptions.Item label="设备名称">{selectedOnvifDevice?.name || '-'}</Descriptions.Item>
            <Descriptions.Item label="厂商/型号">{[selectedOnvifDevice?.vendor, selectedOnvifDevice?.modelName].filter(Boolean).join(' / ') || '-'}</Descriptions.Item>
            <Descriptions.Item label="序列号">{selectedOnvifDevice?.serialNo || '-'}</Descriptions.Item>
            <Descriptions.Item label="ONVIF地址">{selectedOnvifDevice?.onvifDeviceUrl || '-'}</Descriptions.Item>
            <Descriptions.Item label="RTSP地址">{selectedOnvifDevice?.rtspUrl || '-'}</Descriptions.Item>
          </Descriptions>

          <Divider>认证信息</Divider>

          <Form.Item
            name="username"
            label="用户名"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入ONVIF设备用户名" prefix={<UserOutlined />} />
          </Form.Item>

          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="请输入ONVIF设备密码" prefix={<LockOutlined />} />
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default CameraPage;
