import type { MsgChannelRecord, MsgTemplateRecord, PagedList } from '@/services/budiot/typing';

export type VideoModuleSummary = {
  module: string;
  protocols: string[];
  aiCapabilities: string[];
  subServices: string[];
  menuPaths: string[];
};

export type VideoModuleOverview = {
  cameraCount: number;
  onlineCameraCount: number;
  offlineCameraCount: number;
  taskCount: number;
  modelCount: number;
  alarmCount: number;
  todayAlarmCount: number;
  imageCount: number;
  recordCount: number;
};

export interface VideoMonitorDashboardData {
  summary: VideoModuleSummary;
  overview: VideoModuleOverview;
  catalogTree: VideoCatalogRecord[];
  cameras: VideoCameraRecord[];
  runningTasks: VideoAlgoTaskRecord[];
  recentAlarms: VideoAlarmRecord[];
  recentSnapshots?: VideoMediaFileRecord[];
}

export interface VideoMonitorRealtimeDetection {
  taskId: string;
  taskName?: string;
  cameraId: string;
  modelId?: string;
  modelName?: string;
  className?: string;
  confidence?: number;
  bbox: number[];
  frameWidth?: number;
  frameHeight?: number;
  updatedAt?: number;
}

export interface VideoMonitorRealtimeCameraState {
  cameraId: string;
  status?: string;
  lastInferenceAt?: number;
  frameWidth?: number;
  frameHeight?: number;
  lastError?: string;
  overlayPlayUrl?: string;
  overlayRtmpUrl?: string;
  overlayStatus?: string;
  overlayUpdatedAt?: number;
  overlayLastError?: string;
  detections: VideoMonitorRealtimeDetection[];
}

export interface VideoMonitorRealtimeData {
  serverTime: number;
  cameraStates: Record<string, VideoMonitorRealtimeCameraState>;
}

export interface VideoCatalogRecord {
  id: string;
  parentId?: string;
  path?: string;
  name: string;
  description?: string;
  location?: number;
  hasChildren?: boolean;
  createdAt?: number;
  updatedAt?: number;
  value?: string;
  label?: string;
  leaf?: boolean;
  children?: VideoCatalogRecord[];
}

export interface VideoOption {
  value: string | number;
  text: string;
}

export interface VideoCameraRecord {
  id: string;
  catalogId?: string;
  catalogName?: string;
  cameraCode: string;
  name: string;
  protocolType: string;
  cameraType?: string;
  vendor?: string;
  modelName?: string;
  serialNo?: string;
  macAddress?: string;
  firmwareVersion?: string;
  hardwareId?: string;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  channelCode?: string;
  streamIndex?: number;
  streamUrl?: string;
  playUrl?: string;
  snapshotUrl?: string;
  snapshotImageUrl?: string;
  onvifDeviceUrl?: string;
  gb28181DeviceId?: string;
  gb28181Password?: string;
  gb28181Domain?: string;
  gb28181DeviceIp?: string;
  gb28181DevicePort?: number;
  gb28181RegisterStatus?: string;
  protocolConfig?: string;
  longitude?: string;
  latitude?: string;
  address?: string;
  description?: string;
  supportMove?: boolean;
  supportZoom?: boolean;
  online?: boolean;
  enabled: boolean;
  createdAt?: number;
  updatedAt?: number;
}

export interface VideoCameraMeta {
  protocols: VideoOption[];
  cameraTypes: VideoOption[];
  streamIndexes: VideoOption[];
  catalogs: VideoCatalogRecord[];
}

export interface VideoTaskNotifyConfigRecord {
  channelId: string;
  templateId?: string;
}

export interface VideoTaskRegionConfigRecord {
  id?: string;
  taskId?: string;
  cameraId: string;
  cameraName?: string;
  modelId: string;
  modelName?: string;
  name: string;
  polygonPoints: string;
  location?: number;
  enabled: boolean;
}

export interface VideoAlgoTaskRecord {
  id: string;
  name: string;
  taskType: string;
  status: string;
  cron?: string;
  frameInterval?: number;
  trackingEnabled: boolean;
  trackingSimilarityThreshold?: number;
  trackingMaxAliveFrames?: number;
  trackingSmoothFactor?: number;
  allDayArming: boolean;
  beginTime?: string;
  endTime?: string;
  alarmEnabled: boolean;
  alarmRecordSeconds?: number;
  sameAlarmWindowMinutes?: number;
  notifyEnabled: boolean;
  notifyUserIdsJson?: string;
  notifyChannelsJson?: string;
  notifyConfigs?: VideoTaskNotifyConfigRecord[];
  cameraIds?: string[];
  cameraNames?: string[];
  modelIds?: string[];
  modelNames?: string[];
  regionConfigs?: VideoTaskRegionConfigRecord[];
  createdAt?: number;
  updatedAt?: number;
}

export interface VideoAlgoTaskRuntimeCameraState {
  status?: string;
  last_capture_at?: number;
  last_inference_at?: number;
  last_alarm_at?: number;
  last_image_url?: string;
  last_media_file_id?: string;
  last_record_file_id?: string;
  last_error?: string;
  frame_width?: number;
  frame_height?: number;
  inference_pending?: boolean;
  detection_count?: number;
  last_detections?: VideoMonitorRealtimeDetection[];
  overlay_stream_id?: string;
  overlay_play_url?: string;
  overlay_rtmp_url?: string;
  overlay_status?: string;
  overlay_updated_at?: number;
  overlay_last_error?: string;
}

export interface VideoAlgoTaskRuntimeRecord {
  task_id: string;
  status: string;
  created_at?: number;
  updated_at?: number;
  inference_workers?: number;
  inference_queue_size?: number;
  log_count?: number;
  last_event_summary?: Record<string, unknown>;
  cameras: Record<string, VideoAlgoTaskRuntimeCameraState>;
}

export interface VideoAlgoTaskLogEntry {
  timestamp: number;
  level: string;
  task_id: string;
  camera_id?: string;
  message: string;
}

export interface VideoAlgoTaskLogResponse {
  task_id: string;
  status: string;
  count: number;
  entries: VideoAlgoTaskLogEntry[];
}

export interface VideoAlgoTaskMeta {
  taskTypes: string[];
  taskStatuses: string[];
  cameras: VideoCameraRecord[];
  models: Array<{
    id: string;
    name: string;
    currentVersion?: string;
    status?: string;
  }>;
  notifyChannels: MsgChannelRecord[];
  notifyTemplates: MsgTemplateRecord[];
}

export interface VideoAlgoTaskQueryParams {
  name?: string;
  taskType?: string;
  status?: string;
  pageNo?: number;
  pageSize?: number;
  pageOrderName?: string;
  pageOrderBy?: string;
}

export interface VideoCameraQueryParams {
  catalogId?: string;
  cameraCode?: string;
  name?: string;
  protocolType?: string;
  vendor?: string;
  online?: boolean;
  enabled?: boolean;
  pageNo?: number;
  pageSize?: number;
  pageOrderName?: string;
  pageOrderBy?: string;
}

export interface VideoPagedList<T> extends PagedList<T> {}

export interface VideoMediaEndpoint {
  cameraId: string;
  cameraCode: string;
  streamUrl?: string;
  playUrl?: string;
  httpPublicDomain?: string;
  rtmpUrl?: string;
  snapshotUrl?: string;
  recordApi?: string;
  protocolType?: string;
  supportMove?: boolean;
  supportZoom?: boolean;
}

export interface VideoMediaAction {
  cameraId: string;
  action: string;
  accepted: boolean;
  requestUrl?: string;
  filePath?: string;
  fileName?: string;
  fileUrl?: string;
  thumbnailUrl?: string;
  streamUrl?: string;
  playUrl?: string;
  rtmpUrl?: string;
  message?: string;
}

export interface VideoOnvifScanResult {
  host: string;
  port?: number;
  name?: string;
  vendor?: string;
  modelName?: string;
  serialNo?: string;
  mac?: string;
  firmwareVersion?: string;
  hardwareId?: string;
  rtspUrl?: string;
  snapshotUrl?: string;
  onvifDeviceUrl?: string;
  supportMove?: boolean;
  supportZoom?: boolean;
}

export interface VideoMediaFileRecord {
  id: string;
  cameraId: string;
  taskId?: string;
  mediaType: string;
  fileName: string;
  filePath?: string;
  fileUrl?: string;
  thumbnailUrl?: string;
  sourceType?: string;
  fileSize?: number;
  duration?: number;
  bizTime?: number;
  createdAt?: number;
}

export interface VideoInspectionRecord {
  camera: VideoCameraRecord;
  snapshot?: VideoMediaFileRecord;
}

export interface VideoMediaFileQueryParams {
  cameraId?: string;
  taskId?: string;
  mediaType?: string;
  fileName?: string;
  sourceType?: string;
  bizTimeBegin?: number;
  bizTimeEnd?: number;
  pageNo?: number;
  pageSize?: number;
  pageOrderName?: string;
  pageOrderBy?: string;
}

export interface VideoAlarmRecord {
  id: string;
  taskId?: string;
  cameraId?: string;
  cameraName?: string;
  eventType: string;
  eventContent?: string;
  imageFileId?: string;
  imageFileUrl?: string;
  imageThumbnailUrl?: string;
  recordFileId?: string;
  recordFileUrl?: string;
  recordThumbnailUrl?: string;
  recordFileName?: string;
  alarmTime?: number;
  handleStatus: string;
  createdAt?: number;
}

export interface VideoWorkOrderRecord {
  id: string;
  eventId?: string;
  workOrderNo: string;
  assigneeId?: string;
  taskContent?: string;
  handleStatus: string;
  lastReply?: string;
  createdAt?: number;
}

export interface VideoDatasetRecord {
  id: string;
  name: string;
  datasetType: string;
  coverFileId?: string;
  description?: string;
  totalCount?: number;
  labeledCount?: number;
  createdAt?: number;
}

export interface VideoDatasetLabelRecord {
  id: string;
  datasetId: string;
  labelName: string;
  labelColor: string;
  createdAt?: number;
}

export interface VideoDatasetAnnotationRecord {
  id: string;
  datasetId: string;
  dataObjectId: string;
  dataObjectName?: string;
  annotationType: string;
  pointsJson: string;
  labelId: string;
  labelName: string;
  labelColor: string;
  createdAt?: number;
}

export interface VideoDatasetItemRecord {
  id: string;
  datasetId: string;
  mediaType: string;
  fileId: string;
  fileName: string;
  contentType?: string;
  fileSize?: number;
  coverFileId?: string;
  sourceType?: string;
  extractedFromItemId?: string;
  frameIndex?: number;
  frameTimeMs?: number;
  annotated?: boolean;
  annotationCount?: number;
  toTrain?: boolean;
  toValid?: boolean;
  toTest?: boolean;
  createdAt?: number;
}

export interface VideoDatasetAiConfigRecord {
  id?: string;
  provider: string;
  modelName: string;
  apiKey: string;
  promptText?: string;
}

export interface VideoDatasetAiTaskRecord {
  id: string;
  datasetId: string;
  status:
    | { value: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'FAILED' | string; text: string }
    | string;
  totalCount?: number;
  processedCount?: number;
  annotatedItems?: number;
  annotationCount?: number;
  skippedCount?: number;
  failedCount?: number;
  progress?: number;
  currentItemName?: string;
  message?: string;
  cancelRequested?: boolean;
  startedAt?: number;
  finishedAt?: number;
}

export interface VideoDatasetItemMeta {
  mediaTypes: VideoOption[];
  sourceTypes: VideoOption[];
}

export interface VideoModelRecord {
  id: string;
  name: string;
  currentVersion?: string;
  status: string;
  coverFileId?: string;
  modelFileId?: string;
  description?: string;
  createdAt?: number;
}

export interface VideoModelTrainTaskRecord {
  id: string;
  taskName: string;
  modelId: string;
  modelName?: string;
  datasetId: string;
  datasetName?: string;
  modelVersion: string;
  status: string;
  epochs?: number;
  batchSize?: number;
  imgsz?: number;
  pretrainedModel?: string;
  progress?: number;
  trainLog?: string;
  externalTaskId?: string;
  createdAt?: number;
}

export interface VideoModelDeploymentRecord {
  id: string;
  modelId: string;
  modelName?: string;
  version: string;
  instanceName: string;
  servicePort: number;
  serviceUrl?: string;
  nacosServiceName?: string;
  status: string;
  grayRelease: boolean;
  runtimeLog?: string;
  lastHeartbeatAt?: number;
  createdAt?: number;
}

export interface VideoModelTrainMeta {
  models: VideoModelRecord[];
  datasets: VideoDatasetRecord[];
  trainStatuses: VideoOption[];
  deploymentStatuses: VideoOption[];
  inferenceTypes: VideoOption[];
  inferenceStatuses: VideoOption[];
}

export interface VideoModelInferenceTaskRecord {
  id: string;
  taskName: string;
  modelId: string;
  modelName?: string;
  version?: string;
  taskType: string;
  sourceUri: string;
  promptText?: string;
  language?: string;
  status: string;
  externalTaskId?: string;
  resultSummary?: string;
  resultJson?: string;
  createdAt?: number;
}
