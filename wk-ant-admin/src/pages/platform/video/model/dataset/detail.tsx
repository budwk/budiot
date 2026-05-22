import {
  ApartmentOutlined,
  AppstoreOutlined,
  BorderOutlined,
  ClusterOutlined,
  ClearOutlined,
  DeleteOutlined,
  EditOutlined,
  FileZipOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  RobotOutlined,
  UnorderedListOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { PageContainer } from '@ant-design/pro-components';
import { history, useParams } from '@umijs/max';
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Pagination,
  Progress,
  Radio,
  Select,
  Slider,
  Space,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography,
  Upload,
} from 'antd';
import JSZip from 'jszip';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import * as React from 'react';
import {
  ManagedFileImagePreview,
  VideoCoverUploadField,
  useManagedFilePreviewUrl,
} from '../../shared';
import {
  batchCreateVideoDatasetItems,
  cancelVideoDatasetAiAnnotateTask,
  createVideoDatasetAnnotation,
  createVideoDatasetItem,
  createVideoDatasetLabel,
  deleteVideoDatasetAnnotation,
  deleteVideoDatasetItem,
  deleteVideoDatasetLabel,
  extractVideoDatasetFrames,
  getVideoDatasetAiAnnotateTask,
  getVideoDatasetAnnotationPage,
  getVideoDatasetDetail,
  getVideoDatasetItemPage,
  getVideoDatasetLabelDetail,
  listVideoDatasetLabels,
  resetVideoDatasetItemAnnotationStatus,
  resetVideoDatasetItemUsage,
  splitVideoDatasetItemUsage,
  startVideoDatasetAiAnnotateTask,
  updateVideoDatasetItem,
  updateVideoDatasetLabel,
} from '@/services/budiot/video/dataset';
import type {
  VideoDatasetAnnotationRecord,
  VideoDatasetItemRecord,
  VideoDatasetAiTaskRecord,
  VideoDatasetLabelRecord,
  VideoDatasetRecord,
} from '@/services/budiot/video/typing';
import PlatformProTable from '@/components/PlatformProTable';
import { uploadFile } from '@/utils/file';

type Point = { x: number; y: number };
type ImagePaginationState = { pageNo: number; pageSize: number; total: number };
type DatasetUploadSourceType = 'MANUAL' | 'ZIP_IMPORT' | 'FRAME_EXTRACT';
type PagedResponse<T> = { list?: T[]; totalCount?: number; pageNo?: number; pageSize?: number };
type VideoItemFormValues = {
  fileName?: string;
  fileId?: string;
  contentType?: string;
  fileSize?: number;
  coverFileId?: string;
  uploadedVideoName?: string;
};
type VideoFrameFormValues = { videoItemId: string; intervalSeconds: number; imageQuality: number };

const normalizePageState = (pageNo: number, pageSize: number, total: number) => {
  const safePageSize = Math.max(1, pageSize || 1);
  const safeTotal = Math.max(0, total || 0);
  const maxPageNo = Math.max(1, Math.ceil(safeTotal / safePageSize));
  return {
    pageNo: safeTotal ? Math.min(Math.max(1, pageNo || 1), maxPageNo) : 1,
    pageSize: safePageSize,
    total: safeTotal,
  };
};

const normalizePageStateFromResponse = <T,>(
  payload: PagedResponse<T> | undefined,
  fallbackPageNo: number,
  fallbackPageSize: number,
) =>
  normalizePageState(
    Number(payload?.pageNo) || fallbackPageNo,
    Number(payload?.pageSize) || fallbackPageSize,
    Number(payload?.totalCount) || 0,
  );

const DATASET_TYPE_LABELS: Record<string, string> = {
  IMAGE: '图片',
  TEXT: '文本',
};

const IMAGE_FILE_NAME_PATTERN = /\.(png|jpe?g|gif|bmp|webp)$/i;

const getUsageMeta = (record: Pick<VideoDatasetItemRecord, 'toTrain' | 'toValid' | 'toTest'>) => {
  if (record.toTrain) {
    return { text: '训练集', color: 'processing' as const };
  }
  if (record.toValid) {
    return { text: '验证集', color: 'warning' as const };
  }
  if (record.toTest) {
    return { text: '测试集', color: 'purple' as const };
  }
  return { text: '未划分', color: 'default' as const };
};

const DATASET_USAGE_OPTIONS = [
  { label: '未划分', value: 'NONE' },
  { label: '训练集', value: 'TRAIN' },
  { label: '验证集', value: 'VALID' },
  { label: '测试集', value: 'TEST' },
] as const;

const getUsageValue = (record: Pick<VideoDatasetItemRecord, 'toTrain' | 'toValid' | 'toTest'>) => {
  if (record.toTrain) {
    return 'TRAIN';
  }
  if (record.toValid) {
    return 'VALID';
  }
  if (record.toTest) {
    return 'TEST';
  }
  return 'NONE';
};

const getAiTaskStatusValue = (status?: VideoDatasetAiTaskRecord['status']) =>
  typeof status === 'string' ? status : status?.value;

const parsePoints = (pointsJson?: string): Point[] => {
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

const toPolylinePoints = (points: Point[]) => points.map((item) => `${item.x},${item.y}`).join(' ');

const getAnnotationLabelLayout = (points: Point[], labelName?: string) => {
  if (!points.length) {
    return undefined;
  }
  const minX = Math.min(...points.map((item) => item.x));
  const minY = Math.min(...points.map((item) => item.y));
  const width = Math.max(56, (labelName?.length || 0) * 14 + 20);
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

const DatasetDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { modal, message } = App.useApp();
  const datasetImageUploadSubPath = React.useMemo(() => (id ? `dataset/${id}/image` : undefined), [id]);
  const datasetVideoUploadSubPath = React.useMemo(() => (id ? `dataset/${id}/video` : undefined), [id]);
  const [labelForm] = Form.useForm<Partial<VideoDatasetLabelRecord>>();
  const [itemForm] = Form.useForm<VideoItemFormValues>();
  const [frameForm] = Form.useForm<VideoFrameFormValues>();
  const [splitForm] = Form.useForm<{ trainRatio: number; valRatio: number; testRatio: number }>();
  const [usageForm] = Form.useForm<{ usage: string }>();
  const [dataset, setDataset] = React.useState<VideoDatasetRecord>();
  const [labels, setLabels] = React.useState<VideoDatasetLabelRecord[]>([]);
  const [imageItems, setImageItems] = React.useState<VideoDatasetItemRecord[]>([]);
  const [videoItems, setVideoItems] = React.useState<VideoDatasetItemRecord[]>([]);
  const [annotations, setAnnotations] = React.useState<VideoDatasetAnnotationRecord[]>([]);
  const [annotationLoading, setAnnotationLoading] = React.useState(false);
  const [imagePagination, setImagePagination] = React.useState<ImagePaginationState>({
    pageNo: 1,
    pageSize: 8,
    total: 0,
  });
  const [videoPagination, setVideoPagination] = React.useState<ImagePaginationState>({
    pageNo: 1,
    pageSize: 8,
    total: 0,
  });
  const [imagePageInfo, setImagePageInfo] = React.useState({ current: 1, pageSize: 8, total: 0 });
  const [videoPageInfo, setVideoPageInfo] = React.useState({ current: 1, pageSize: 8, total: 0 });
  const [previewImageItem, setPreviewImageItem] = React.useState<VideoDatasetItemRecord>();
  const [playingVideoItem, setPlayingVideoItem] = React.useState<VideoDatasetItemRecord>();
  const [currentImageOrder, setCurrentImageOrder] = React.useState(0);
  const [currentImageItemId, setCurrentImageItemId] = React.useState<string>();
  const [selectedAnnotationId, setSelectedAnnotationId] = React.useState<string>();
  const [labelOpen, setLabelOpen] = React.useState(false);
  const [labelEditingId, setLabelEditingId] = React.useState<string>();
  const [itemOpen, setItemOpen] = React.useState(false);
  const [itemEditing, setItemEditing] = React.useState<VideoDatasetItemRecord>();
  const [itemSubmitting, setItemSubmitting] = React.useState(false);
  const [frameOpen, setFrameOpen] = React.useState(false);
  const [extractingFrames, setExtractingFrames] = React.useState(false);
  const [splitOpen, setSplitOpen] = React.useState(false);
  const [splittingUsage, setSplittingUsage] = React.useState(false);
  const [usageOpen, setUsageOpen] = React.useState(false);
  const [usageSubmitting, setUsageSubmitting] = React.useState(false);
  const [usageEditingItem, setUsageEditingItem] = React.useState<VideoDatasetItemRecord>();
  const [aiTaskOpen, setAiTaskOpen] = React.useState(false);
  const [aiTask, setAiTask] = React.useState<VideoDatasetAiTaskRecord>();
  const [annotatingByAi, setAnnotatingByAi] = React.useState(false);
  const [imageViewMode, setImageViewMode] = React.useState<'table' | 'card'>('card');
  const [videoViewMode, setVideoViewMode] = React.useState<'table' | 'card'>('card');
  const [onlyUnannotated, setOnlyUnannotated] = React.useState(false);
  const [selectedLabelId, setSelectedLabelId] = React.useState<string>();
  const [drawMode, setDrawMode] = React.useState<'RECTANGLE' | 'POLYGON'>('RECTANGLE');
  const [drawingRect, setDrawingRect] = React.useState<Point | undefined>();
  const [rectDraft, setRectDraft] = React.useState<Point[]>([]);
  const [polygonDraft, setPolygonDraft] = React.useState<Point[]>([]);
  const [imageNaturalSize, setImageNaturalSize] = React.useState({ width: 1280, height: 720 });
  const imageActionRef = React.useRef<ActionType>(null);
  const videoActionRef = React.useRef<ActionType>(null);
  const imagePaginationRef = React.useRef<ImagePaginationState>({ pageNo: 1, pageSize: 8, total: 0 });
  const videoPaginationRef = React.useRef<ImagePaginationState>({ pageNo: 1, pageSize: 8, total: 0 });
  const svgRef = React.useRef<SVGSVGElement>(null);

  React.useEffect(() => {
    imagePaginationRef.current = imagePagination;
  }, [imagePagination]);

  React.useEffect(() => {
    videoPaginationRef.current = videoPagination;
  }, [videoPagination]);

  React.useEffect(() => {
    if (!labels.length) {
      return;
    }
    if (!selectedLabelId || !labels.some((item) => item.id === selectedLabelId)) {
      setSelectedLabelId(labels[0].id);
    }
  }, [labels, selectedLabelId]);

  const currentImageItem = previewImageItem;
  const currentImageUrl = useManagedFilePreviewUrl(currentImageItem?.fileId);
  const currentVideoUrl = useManagedFilePreviewUrl(playingVideoItem?.fileId);
  const currentEditingVideoName = Form.useWatch('uploadedVideoName', itemForm);
  const currentEditingVideoFileId = Form.useWatch('fileId', itemForm);
  const datasetTypeText = dataset?.datasetType ? DATASET_TYPE_LABELS[dataset.datasetType] || dataset.datasetType : '-';

  const selectPreviewImage = React.useCallback(
    (item: VideoDatasetItemRecord, indexInPage?: number) => {
      setCurrentImageItemId(item.id);
      setPreviewImageItem(item);
      if (typeof indexInPage === 'number') {
        setCurrentImageOrder((imagePagination.pageNo - 1) * imagePagination.pageSize + indexInPage + 1);
      }
    },
    [imagePagination.pageNo, imagePagination.pageSize],
  );

  React.useEffect(() => {
    if (!imageItems.length) {
      if (!imagePagination.total) {
        setPreviewImageItem(undefined);
        setCurrentImageItemId(undefined);
        setCurrentImageOrder(0);
        setSelectedAnnotationId(undefined);
      }
      return;
    }
    if (!previewImageItem) {
      setCurrentImageItemId(imageItems[0].id);
      setPreviewImageItem(imageItems[0]);
      setCurrentImageOrder((imagePagination.pageNo - 1) * imagePagination.pageSize + 1);
      return;
    }
    const matched = imageItems.find((item) => item.id === previewImageItem.id);
    if (matched && matched !== previewImageItem) {
      setPreviewImageItem(matched);
      return;
    }
    if (!matched) {
      setCurrentImageItemId(imageItems[0].id);
      setPreviewImageItem(imageItems[0]);
      setCurrentImageOrder((imagePagination.pageNo - 1) * imagePagination.pageSize + 1);
    }
  }, [imageItems, imagePagination.pageNo, imagePagination.pageSize, imagePagination.total, previewImageItem]);

  React.useEffect(() => {
    imageActionRef.current?.setPageInfo?.({ current: 1, pageSize: imagePaginationRef.current.pageSize });
    imageActionRef.current?.reload?.();
  }, [onlyUnannotated]);

  React.useEffect(() => {
    if (!annotations.length) {
      setSelectedAnnotationId(undefined);
      return;
    }
    if (!selectedAnnotationId || !annotations.some((item) => item.id === selectedAnnotationId)) {
      setSelectedAnnotationId(annotations[0].id);
    }
  }, [annotations, selectedAnnotationId]);

  const loadDataset = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const res = await getVideoDatasetDetail(id);
    setDataset(res.data);
  }, [id]);

  const loadLabels = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const res = await listVideoDatasetLabels(id);
    setLabels(res.data || []);
  }, [id]);

  const loadImageItems = React.useCallback(
    async (pageNo = imagePaginationRef.current.pageNo, pageSize = imagePaginationRef.current.pageSize) => {
      if (!id) {
        return { list: [], pageNo: 1, pageSize, total: 0 };
      }
      const requestedPageNo = Math.max(1, pageNo || 1);
      const requestedPageSize = Math.max(1, pageSize || imagePaginationRef.current.pageSize || 8);
      let res = await getVideoDatasetItemPage({
        datasetId: id,
        mediaType: 'IMAGE',
        annotated: onlyUnannotated ? false : undefined,
        pageNo: requestedPageNo,
        pageSize: requestedPageSize,
        pageOrderName: 'createdAt',
        pageOrderBy: 'descending',
      });
      let normalized = normalizePageStateFromResponse(
        res.data as PagedResponse<VideoDatasetItemRecord>,
        requestedPageNo,
        requestedPageSize,
      );
      const total = normalized.total;
      if (total > 0 && normalized.pageNo !== requestedPageNo) {
        res = await getVideoDatasetItemPage({
          datasetId: id,
          mediaType: 'IMAGE',
          annotated: onlyUnannotated ? false : undefined,
          pageNo: normalized.pageNo,
          pageSize: normalized.pageSize,
          pageOrderName: 'createdAt',
          pageOrderBy: 'descending',
        });
        normalized = normalizePageStateFromResponse(
          res.data as PagedResponse<VideoDatasetItemRecord>,
          normalized.pageNo,
          normalized.pageSize,
        );
      }
      const list = res.data.list || [];
      setImageItems(list);
      setImagePagination(normalized);
      if (!list.length && !normalized.total) {
        setCurrentImageItemId(undefined);
        setPreviewImageItem(undefined);
        setCurrentImageOrder(0);
      }
      return { list, pageNo: normalized.pageNo, pageSize: normalized.pageSize, total: normalized.total };
    },
    [id, onlyUnannotated],
  );

  const requestImageItems = React.useCallback(
    async (params: Record<string, any>) => {
      const result = await loadImageItems(params.current, params.pageSize);
      setImagePageInfo({
        current: result.pageNo,
        pageSize: result.pageSize,
        total: result.total,
      });
      return {
        data: result.list,
        total: result.total,
        success: true,
      };
    },
    [loadImageItems],
  );

  const loadVideoItems = React.useCallback(
    async (pageNo = videoPaginationRef.current.pageNo, pageSize = videoPaginationRef.current.pageSize) => {
      if (!id) {
        return { list: [], pageNo: 1, pageSize, total: 0 };
      }
      const requestedPageNo = Math.max(1, pageNo || 1);
      const requestedPageSize = Math.max(1, pageSize || videoPaginationRef.current.pageSize || 8);
      let res = await getVideoDatasetItemPage({
        datasetId: id,
        mediaType: 'VIDEO',
        pageNo: requestedPageNo,
        pageSize: requestedPageSize,
        pageOrderName: 'createdAt',
        pageOrderBy: 'descending',
      });
      let normalized = normalizePageStateFromResponse(
        res.data as PagedResponse<VideoDatasetItemRecord>,
        requestedPageNo,
        requestedPageSize,
      );
      const total = normalized.total;
      if (total > 0 && normalized.pageNo !== requestedPageNo) {
        res = await getVideoDatasetItemPage({
          datasetId: id,
          mediaType: 'VIDEO',
          pageNo: normalized.pageNo,
          pageSize: normalized.pageSize,
          pageOrderName: 'createdAt',
          pageOrderBy: 'descending',
        });
        normalized = normalizePageStateFromResponse(
          res.data as PagedResponse<VideoDatasetItemRecord>,
          normalized.pageNo,
          normalized.pageSize,
        );
      }
      const list = res.data.list || [];
      setVideoItems(list);
      setVideoPagination(normalized);
      return { list, pageNo: normalized.pageNo, pageSize: normalized.pageSize, total: normalized.total };
    },
    [id],
  );

  const requestVideoItems = React.useCallback(
    async (params: Record<string, any>) => {
      const result = await loadVideoItems(params.current, params.pageSize);
      setVideoPageInfo({
        current: result.pageNo,
        pageSize: result.pageSize,
        total: result.total,
      });
      return {
        data: result.list,
        total: result.total,
        success: true,
      };
    },
    [loadVideoItems],
  );

  const loadAnnotations = React.useCallback(
    async (dataObjectId?: string) => {
      if (!id || !dataObjectId) {
        setAnnotations([]);
        return;
      }
      setAnnotationLoading(true);
      try {
        const res = await getVideoDatasetAnnotationPage({
          datasetId: id,
          dataObjectId,
          pageNo: 1,
          pageSize: 200,
          pageOrderName: 'createdAt',
          pageOrderBy: 'descending',
        });
        setAnnotations(res.data.list || []);
      } finally {
        setAnnotationLoading(false);
      }
    },
    [id],
  );

  React.useEffect(() => {
    loadDataset().catch(() => undefined);
    loadLabels().catch(() => undefined);
    loadImageItems(1, imagePaginationRef.current.pageSize).catch(() => undefined);
    loadVideoItems(1, videoPaginationRef.current.pageSize).catch(() => undefined);
  }, [loadDataset, loadImageItems, loadLabels, loadVideoItems]);

  React.useEffect(() => {
    loadAnnotations(currentImageItem?.id).catch(() => undefined);
    setRectDraft([]);
    setPolygonDraft([]);
    setDrawingRect(undefined);
  }, [currentImageItem?.id, loadAnnotations]);

  const refreshSummary = React.useCallback(async () => {
    await Promise.all([loadDataset(), loadImageItems(), loadVideoItems()]);
    imageActionRef.current?.setPageInfo?.({
      current: imagePaginationRef.current.pageNo,
      pageSize: imagePaginationRef.current.pageSize,
    });
    videoActionRef.current?.setPageInfo?.({
      current: videoPaginationRef.current.pageNo,
      pageSize: videoPaginationRef.current.pageSize,
    });
    imageActionRef.current?.reload?.();
    videoActionRef.current?.reload?.();
  }, [loadDataset, loadImageItems, loadVideoItems]);

  React.useEffect(() => {
    if (!aiTaskOpen || !aiTask?.id) {
      return undefined;
    }
    const status = getAiTaskStatusValue(aiTask.status);
    if (!status || ['COMPLETED', 'CANCELLED', 'FAILED'].includes(status)) {
      return undefined;
    }
    const timer = window.setInterval(() => {
      getVideoDatasetAiAnnotateTask(aiTask.id)
        .then((res) => {
          const nextTask = res.data;
          const nextStatus = getAiTaskStatusValue(nextTask.status);
          setAiTask(nextTask);
          if (nextStatus && ['COMPLETED', 'CANCELLED', 'FAILED'].includes(nextStatus)) {
            window.clearInterval(timer);
            setAnnotatingByAi(false);
            refreshSummary().catch(() => undefined);
            if (currentImageItem?.id) {
              loadAnnotations(currentImageItem.id).catch(() => undefined);
            }
          }
        })
        .catch(() => undefined);
    }, 2000);
    return () => window.clearInterval(timer);
  }, [aiTask?.id, aiTask?.status, aiTaskOpen, currentImageItem?.id, loadAnnotations, refreshSummary]);

  const handleLabelSubmit = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const values = await labelForm.validateFields();
    if (labelEditingId) {
      await updateVideoDatasetLabel({ ...values, datasetId: id, id: labelEditingId });
    } else {
      await createVideoDatasetLabel({ ...values, datasetId: id });
    }
    setLabelOpen(false);
    setLabelEditingId(undefined);
    await loadLabels();
  }, [id, labelEditingId, labelForm, loadLabels]);

  const openEditLabel = React.useCallback(
    async (record: VideoDatasetLabelRecord) => {
      const res = await getVideoDatasetLabelDetail(record.id);
      setLabelEditingId(record.id);
      labelForm.setFieldsValue(res.data);
      setLabelOpen(true);
    },
    [labelForm],
  );

  const uploadDatasetAssets = React.useCallback(
    async (
      files: File[],
      mediaType: 'IMAGE' | 'VIDEO',
      sourceType: DatasetUploadSourceType,
      buildExtra?: (file: File, index: number) => Record<string, unknown>,
    ) => {
      if (!id || !files.length) {
        return 0;
      }
      const payloads: Record<string, unknown>[] = [];
      for (const [index, file] of files.entries()) {
        const formData = new FormData();
        formData.append('Filedata', file);
        const response = await uploadFile(formData, {
          type: mediaType === 'IMAGE' ? 'image' : 'video',
          params: {
            subPath: mediaType === 'IMAGE' ? datasetImageUploadSubPath : datasetVideoUploadSubPath,
          },
        });
        const fileRecord = response.data as { id?: string; fileName?: string };
        payloads.push({
          datasetId: id,
          mediaType,
          fileId: fileRecord.id,
          fileName: fileRecord.fileName || file.name,
          contentType: file.type,
          fileSize: file.size,
          sourceType,
          ...(buildExtra ? buildExtra(file, index) : undefined),
        });
      }
      if (payloads.length) {
        await batchCreateVideoDatasetItems(payloads);
      }
      await refreshSummary();
      return payloads.length;
    },
    [datasetImageUploadSubPath, datasetVideoUploadSubPath, id, refreshSummary],
  );

  const handleUploadItems = React.useCallback(
    async (options: UploadRequestOption, mediaType: 'IMAGE' | 'VIDEO') => {
      const file = options.file as File;
      await uploadDatasetAssets([file], mediaType, 'MANUAL');
      options.onSuccess?.({});
    },
    [uploadDatasetAssets],
  );

  const handleZipImport = React.useCallback(
    async (options: UploadRequestOption) => {
      const zipFile = options.file as File;
      const zip = await JSZip.loadAsync(zipFile);
      const imageEntries = Object.values(zip.files).filter(
        (entry) => !entry.dir && IMAGE_FILE_NAME_PATTERN.test(entry.name),
      );
      if (!imageEntries.length) {
        throw new Error('压缩包中未找到图片文件');
      }
      const files: File[] = [];
      for (const entry of imageEntries) {
        const blob = await entry.async('blob');
        const fileName = entry.name.split('/').pop() || entry.name;
        files.push(new File([blob], fileName, { type: blob.type || 'image/jpeg' }));
      }
      const count = await uploadDatasetAssets(files, 'IMAGE', 'ZIP_IMPORT');
      options.onSuccess?.({});
      message.success(`已导入 ${count} 张图片`);
    },
    [message, uploadDatasetAssets],
  );

  const openPlayVideo = React.useCallback((record: VideoDatasetItemRecord) => {
    setPlayingVideoItem(record);
  }, []);

  const openCreateVideoItem = React.useCallback(() => {
    setItemEditing(undefined);
    itemForm.resetFields();
    itemForm.setFieldsValue({
      contentType: 'video/mp4',
      uploadedVideoName: '',
    });
    setItemOpen(true);
  }, [itemForm]);

  const openEditVideoItem = React.useCallback(
    (record: VideoDatasetItemRecord) => {
      setItemEditing(record);
      itemForm.setFieldsValue({
        fileName: record.fileName,
        fileId: record.fileId,
        contentType: record.contentType,
        fileSize: record.fileSize,
        coverFileId: record.coverFileId,
        uploadedVideoName: record.fileName,
      });
      setItemOpen(true);
    },
    [itemForm],
  );

  const handleVideoFileUpload = React.useCallback(
    async (options: UploadRequestOption) => {
      const file = options.file as File;
      const isMp4 = file.type === 'video/mp4' || /\.mp4$/i.test(file.name);
      if (!isMp4) {
        const error = new Error('仅支持上传 mp4 文件');
        options.onError?.(error);
        throw error;
      }
      try {
        const formData = new FormData();
        formData.append('Filedata', file);
        const response = await uploadFile(formData, {
          type: 'video',
          params: { subPath: datasetVideoUploadSubPath },
        });
        const fileRecord = response.data as {
          id?: string;
          filename?: string;
          fileName?: string;
          contentType?: string;
          size?: number;
        };
        const uploadedVideoName = fileRecord.filename || fileRecord.fileName || file.name;
        const currentVideoName = itemForm.getFieldValue('fileName');
        itemForm.setFieldsValue({
          fileId: fileRecord.id,
          fileName: currentVideoName || uploadedVideoName.replace(/\.[^.]+$/, ''),
          uploadedVideoName,
          contentType: fileRecord.contentType || file.type || 'video/mp4',
          fileSize: fileRecord.size || file.size,
        });
        options.onSuccess?.({});
      } catch (error) {
        options.onError?.(error as Error);
        throw error;
      }
    },
    [datasetVideoUploadSubPath, itemForm],
  );

  const handleSubmitVideoItem = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const values = await itemForm.validateFields();
    setItemSubmitting(true);
    try {
      const payload = {
        datasetId: id,
        mediaType: 'VIDEO',
        fileName: values.fileName?.trim(),
        fileId: values.fileId,
        contentType: values.contentType || 'video/mp4',
        fileSize: values.fileSize || 0,
        coverFileId: values.coverFileId,
      };
      if (itemEditing?.id) {
        await updateVideoDatasetItem({
          ...payload,
          id: itemEditing.id,
        });
      } else {
        await createVideoDatasetItem(payload);
      }
      setItemOpen(false);
      setItemEditing(undefined);
      itemForm.resetFields();
      await refreshSummary();
    } finally {
      setItemSubmitting(false);
    }
  }, [id, itemEditing?.id, itemForm, refreshSummary]);

  const handleOpenFrameExtract = React.useCallback(
    (record?: VideoDatasetItemRecord) => {
      frameForm.setFieldsValue({
        videoItemId: record?.id || videoItems[0]?.id,
        intervalSeconds: 1,
        imageQuality: 80,
      });
      setFrameOpen(true);
    },
    [frameForm, videoItems],
  );

  const handleExtractFrames = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const values = await frameForm.validateFields();
    const videoItem = videoItems.find((item) => item.id === values.videoItemId);
    if (!videoItem) {
      message.warning('请选择视频');
      return;
    }
    setExtractingFrames(true);
    try {
      const res = await extractVideoDatasetFrames({
        datasetId: id,
        videoItemId: values.videoItemId,
        intervalSeconds: values.intervalSeconds,
        imageQuality: values.imageQuality,
      });
      setFrameOpen(false);
      await refreshSummary();
      const count = Number((res.data as { createdCount?: number })?.createdCount || 0);
      message.success(`已从视频中抽取 ${count} 帧图片`);
    } finally {
      setExtractingFrames(false);
    }
  }, [frameForm, id, message, refreshSummary, videoItems]);

  const handleSplitUsage = React.useCallback(async () => {
    if (!id) {
      return;
    }
    const values = await splitForm.validateFields();
    const totalRatio = Number(values.trainRatio) + Number(values.valRatio) + Number(values.testRatio);
    if (Math.abs(totalRatio - 1) > 0.000001) {
      message.warning('训练集、验证集、测试集比例之和必须为 1');
      return;
    }
    setSplittingUsage(true);
    try {
      await splitVideoDatasetItemUsage({
        datasetId: id,
        trainRatio: values.trainRatio,
        valRatio: values.valRatio,
        testRatio: values.testRatio,
      });
      setSplitOpen(false);
      await loadImageItems(1, imagePagination.pageSize);
      imageActionRef.current?.setPageInfo?.({ current: 1, pageSize: imagePagination.pageSize });
      await loadDataset();
    } finally {
      setSplittingUsage(false);
    }
  }, [id, imagePagination.pageSize, loadDataset, loadImageItems, message, splitForm]);

  const handleResetUsage = React.useCallback(() => {
    if (!id) {
      return;
    }
    modal.confirm({
      title: '确认一键重置数据集用途吗？',
      content: '将把当前数据集下所有图片的训练集/验证集/测试集标记重置为未划分。',
      onOk: async () => {
        await resetVideoDatasetItemUsage(id);
        await loadImageItems(1, imagePagination.pageSize);
        imageActionRef.current?.setPageInfo?.({ current: 1, pageSize: imagePagination.pageSize });
      },
    });
  }, [id, imagePagination.pageSize, loadImageItems, modal]);

  const handleResetAnnotationStatus = React.useCallback(() => {
    if (!id) {
      return;
    }
    modal.confirm({
      title: '确认一键重置标注状态吗？',
      content: '将把当前数据集下所有图片重置为未标注，并删除关联的标注信息。',
      onOk: async () => {
        await resetVideoDatasetItemAnnotationStatus(id);
        await refreshSummary();
        if (currentImageItem?.id) {
          await loadAnnotations(currentImageItem.id);
        } else {
          setAnnotations([]);
        }
      },
    });
  }, [currentImageItem?.id, id, loadAnnotations, modal, refreshSummary]);

  const handleUpdateImageUsage = React.useCallback(
    async (record: VideoDatasetItemRecord, usage: string) => {
      await updateVideoDatasetItem({
        id: record.id,
        datasetId: record.datasetId,
        mediaType: record.mediaType,
        fileId: record.fileId,
        fileName: record.fileName,
        contentType: record.contentType,
        fileSize: record.fileSize,
        coverFileId: record.coverFileId,
        sourceType: record.sourceType,
        extractedFromItemId: record.extractedFromItemId,
        frameIndex: record.frameIndex,
        frameTimeMs: record.frameTimeMs,
        toTrain: usage === 'TRAIN',
        toValid: usage === 'VALID',
        toTest: usage === 'TEST',
      });
      await refreshSummary();
    },
    [refreshSummary],
  );

  const openUsageModal = React.useCallback(
    (record: VideoDatasetItemRecord) => {
      setUsageEditingItem(record);
      usageForm.setFieldsValue({ usage: getUsageValue(record) });
      setUsageOpen(true);
    },
    [usageForm],
  );

  const handleSubmitImageUsage = React.useCallback(async () => {
    if (!usageEditingItem) {
      return;
    }
    const values = await usageForm.validateFields();
    setUsageSubmitting(true);
    try {
      await handleUpdateImageUsage(usageEditingItem, values.usage);
      setUsageOpen(false);
      setUsageEditingItem(undefined);
    } finally {
      setUsageSubmitting(false);
    }
  }, [handleUpdateImageUsage, usageEditingItem, usageForm]);

  const handleAiAnnotate = React.useCallback(() => {
    if (!id) {
      return;
    }
    const total = Math.max(imagePagination.total || 0, imageItems.length || 0);
    if (!total) {
      message.warning('请先上传图片');
      return;
    }
    setAiTask(undefined);
    setAiTaskOpen(true);
  }, [id, imageItems.length, imagePagination.total, message]);

  const handleStartAiAnnotate = React.useCallback(async () => {
    if (!id) {
      return;
    }
    setAnnotatingByAi(true);
    try {
      const response = await startVideoDatasetAiAnnotateTask(id);
      setAiTask(response.data);
      setAiTaskOpen(true);
    } finally {
      setAnnotatingByAi(false);
    }
  }, [id]);

  const handleCancelAiAnnotate = React.useCallback(async () => {
    if (!aiTask?.id) {
      return;
    }
    const res = await cancelVideoDatasetAiAnnotateTask(aiTask.id);
    setAiTask(res.data);
  }, [aiTask?.id]);

  const svgPoint = React.useCallback((event: React.MouseEvent<SVGSVGElement>) => {
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
      x: Math.max(0, Math.min(imageNaturalSize.width, transformed.x)),
      y: Math.max(0, Math.min(imageNaturalSize.height, transformed.y)),
    };
  }, [imageNaturalSize.height, imageNaturalSize.width]);

  const saveAnnotation = React.useCallback(
    async (points: Point[], annotationType: 'RECTANGLE' | 'POLYGON') => {
      if (!id || !currentImageItem || !selectedLabelId) {
        message.warning('请先选择标签');
        return;
      }
      const label = labels.find((item) => item.id === selectedLabelId);
      await createVideoDatasetAnnotation({
        datasetId: id,
        dataObjectId: currentImageItem.id,
        dataObjectName: currentImageItem.fileName,
        labelId: selectedLabelId,
        labelName: label?.labelName,
        labelColor: label?.labelColor,
        annotationType,
        pointsJson: JSON.stringify(points),
      });
      setRectDraft([]);
      setPolygonDraft([]);
      setDrawingRect(undefined);
      await Promise.all([loadAnnotations(currentImageItem.id), refreshSummary()]);
    },
    [currentImageItem, id, labels, loadAnnotations, message, refreshSummary, selectedLabelId],
  );

  const finishPolygon = React.useCallback(() => {
    if (polygonDraft.length < 3) {
      message.warning('多边形至少需要 3 个点');
      return;
    }
    saveAnnotation(polygonDraft, 'POLYGON').catch(() => undefined);
  }, [message, polygonDraft, saveAnnotation]);

  const annotationRows = React.useMemo(
    () =>
      annotations.map((item) => ({
        ...item,
        points: parsePoints(item.pointsJson),
      })),
    [annotations],
  );

  const annotationLabelStats = React.useMemo(
    () =>
      labels.map((label) => ({
        ...label,
        count: annotationRows.filter((item) => item.labelId === label.id).length,
      })),
    [annotationRows, labels],
  );

  const confirmDeleteVideoItem = React.useCallback(
    (record: VideoDatasetItemRecord, titlePrefix: string) => {
      modal.confirm({
        title: `确认删除${titlePrefix}“${record.fileName}”吗？`,
        onOk: async () => {
          await deleteVideoDatasetItem(record.id);
          await refreshSummary();
        },
      });
    },
    [modal, refreshSummary],
  );

  const imageTableColumns = React.useMemo<ProColumns<VideoDatasetItemRecord>[]>(
    () => [
      {
        title: '图片',
        dataIndex: 'fileId',
        width: 100,
        search: false,
        render: (_, record) => (
          <div style={{ width: 56, height: 56, overflow: 'hidden', borderRadius: 8 }}>
            <ManagedFileImagePreview
              fileId={record.fileId}
              alt={record.fileName}
              style={{ width: 56, height: 56, objectFit: 'contain', background: '#f5f5f5' }}
            />
          </div>
        ),
      },
      { title: '文件名', dataIndex: 'fileName', search: false },
      {
        title: '是否标注',
        dataIndex: 'annotated',
        search: false,
        render: (_, record) => (record.annotated ? <Tag color="success">已标注</Tag> : <Tag>未标注</Tag>),
      },
      {
        title: '集类型',
        search: false,
        render: (_, record) => {
          const usage = getUsageMeta(record);
          return <Tag color={usage.color}>{usage.text}</Tag>;
        },
      },
      {
        title: '标注数',
        dataIndex: 'annotationCount',
        width: 100,
        search: false,
      },
      {
        title: '上传时间',
        dataIndex: 'createdAt',
        search: false,
        render: (_, record) => (record.createdAt ? new Date(record.createdAt).toLocaleString() : '-'),
      },
      {
        title: '操作',
        width: 180,
        valueType: 'option',
        render: (_, record) => [
          <Button key="usage" type="link" icon={<EditOutlined />} onClick={() => openUsageModal(record)}>
            更改
          </Button>,
          <Button
            key="delete"
            danger
            type="link"
            icon={<DeleteOutlined />}
            onClick={() =>
              modal.confirm({
                title: `确认删除图片“${record.fileName}”吗？`,
                onOk: async () => {
                  await deleteVideoDatasetItem(record.id);
                  await refreshSummary();
                },
              })
            }
          >
            删除
          </Button>,
        ],
      },
    ],
    [modal, openUsageModal, refreshSummary],
  );

  const videoTableColumns = React.useMemo<ProColumns<VideoDatasetItemRecord>[]>(
    () => [
      {
        title: '封面',
        dataIndex: 'coverFileId',
        width: 120,
        search: false,
        render: (_, record) => (
          <div
            style={{ width: 88, height: 56, overflow: 'hidden', borderRadius: 8, cursor: 'pointer' }}
            onClick={() => openPlayVideo(record)}
          >
            <ManagedFileImagePreview
              fileId={record.coverFileId || record.fileId}
              alt={record.fileName}
              preview={false}
              style={{ width: 88, height: 56, objectFit: 'cover', background: '#f5f5f5' }}
            />
          </div>
        ),
      },
      { title: '视频名称', dataIndex: 'fileName', search: false },
      {
        title: '来源',
        dataIndex: 'sourceType',
        search: false,
        render: (_, record) => {
          const sourceText =
            record.sourceType === 'FRAME_EXTRACT' ? '抽帧生成' : record.sourceType === 'ZIP_IMPORT' ? 'ZIP 导入' : '手动上传';
          return <Tag color={record.sourceType === 'FRAME_EXTRACT' ? 'purple' : 'blue'}>{sourceText}</Tag>;
        },
      },
      {
        title: '上传时间',
        dataIndex: 'createdAt',
        search: false,
        render: (_, record) => (record.createdAt ? new Date(record.createdAt).toLocaleString() : '-'),
      },
      {
        title: '操作',
        width: 260,
        valueType: 'option',
        render: (_, record) => [
          <Button key="play" type="link" icon={<PlayCircleOutlined />} onClick={() => openPlayVideo(record)}>
            播放
          </Button>,
          <Button key="extract" type="link" icon={<BorderOutlined />} onClick={() => handleOpenFrameExtract(record)}>
            抽帧
          </Button>,
          <Button key="edit" type="link" icon={<EditOutlined />} onClick={() => openEditVideoItem(record)}>
            修改
          </Button>,
          <Button
            key="delete"
            danger
            type="link"
            icon={<DeleteOutlined />}
            onClick={() => confirmDeleteVideoItem(record, '视频')}
          >
            删除
          </Button>,
        ],
      },
    ],
    [confirmDeleteVideoItem, handleOpenFrameExtract, openEditVideoItem, openPlayVideo],
  );

  return (
    <PageContainer title={dataset?.name || '数据集详情'} onBack={() => history.push('/platform/video/model/dataset')}>
      <Tabs
        items={[
          {
            key: 'base',
            label: '基础信息',
            children: (
              <Card>
                <Descriptions column={2} bordered>
                  <Descriptions.Item label="数据集名称">{dataset?.name || '-'}</Descriptions.Item>
                  <Descriptions.Item label="数据集类型">
                    {dataset?.datasetType ? <Tag color="blue">{datasetTypeText}</Tag> : '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="标注进度" span={2}>
                    <Space direction="vertical" size={4} style={{ width: 320 }}>
                      <span>
                        {dataset?.labeledCount || 0}/{dataset?.totalCount || 0}
                      </span>
                      <Progress
                        percent={
                          (dataset?.totalCount || 0) > 0
                            ? Math.round(((dataset?.labeledCount || 0) / (dataset?.totalCount || 0)) * 100)
                            : 0
                        }
                      />
                    </Space>
                  </Descriptions.Item>
                  <Descriptions.Item label="数据集描述" span={2}>
                    {dataset?.description || '暂无描述'}
                  </Descriptions.Item>
                  <Descriptions.Item label="数据集封面" span={2}>
                    <div style={{ width: 240, height: 160, overflow: 'hidden', borderRadius: 8 }}>
                      <ManagedFileImagePreview
                        fileId={dataset?.coverFileId}
                        alt={dataset?.name}
                        style={{ width: 240, height: 160, objectFit: 'contain', background: '#f5f5f5' }}
                      />
                    </div>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            ),
          },
          {
            key: 'labels',
            label: '数据集标签',
            children: (
              <Card
                extra={
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => {
                      setLabelEditingId(undefined);
                      labelForm.resetFields();
                      setLabelOpen(true);
                    }}
                  >
                    新增标签
                  </Button>
                }
              >
                <Table<VideoDatasetLabelRecord>
                  rowKey="id"
                  pagination={false}
                  dataSource={labels}
                  columns={[
                    { title: '标签名称', dataIndex: 'labelName' },
                    {
                      title: '颜色',
                      dataIndex: 'labelColor',
                      render: (_, record) => <Tag color={record.labelColor}>{record.labelColor}</Tag>,
                    },
                    { title: '描述', dataIndex: 'description' },
                    {
                      title: '操作',
                      width: 180,
                      render: (_, record) => (
                        <Space>
                          <Button type="link" icon={<EditOutlined />} onClick={() => openEditLabel(record)}>
                            修改
                          </Button>
                          <Button
                            danger
                            type="link"
                            icon={<DeleteOutlined />}
                            onClick={() =>
                              modal.confirm({
                                title: `确认删除标签“${record.labelName}”吗？`,
                                onOk: async () => {
                                  await deleteVideoDatasetLabel(record.id);
                                  await loadLabels();
                                },
                              })
                            }
                          >
                            删除
                          </Button>
                        </Space>
                      ),
                    },
                  ]}
                />
              </Card>
            ),
          },
          {
            key: 'images',
            label: '图片数据集',
            children: (
              <Card
                extra={
                  <Space>
                    <Upload
                      accept="image/*"
                      multiple
                      showUploadList={false}
                      customRequest={(options) => {
                        handleUploadItems(options, 'IMAGE').catch((error) => {
                          options.onError?.(error);
                        });
                      }}
                    >
                      <Button type="primary" icon={<UploadOutlined />}>
                        上传单个/批量图片
                      </Button>
                    </Upload>
                    <Upload
                      accept=".zip,application/zip"
                      showUploadList={false}
                      customRequest={(options) => {
                        handleZipImport(options).catch((error) => {
                          options.onError?.(error);
                        });
                      }}
                    >
                      <Button icon={<FileZipOutlined />}>ZIP 导入</Button>
                    </Upload>
                    <Button
                      icon={<ApartmentOutlined />}
                      onClick={() => {
                        splitForm.setFieldsValue({ trainRatio: 0.7, valRatio: 0.2, testRatio: 0.1 });
                        setSplitOpen(true);
                      }}
                    >
                      按比例划分数据集用途
                    </Button>
                    <Button icon={<ClearOutlined />} onClick={handleResetUsage}>
                      一键重置数据集用途
                    </Button>
                    <Button icon={<DeleteOutlined />} onClick={handleResetAnnotationStatus}>
                      一键重置标注状态
                    </Button>
                    <Button
                      icon={<RobotOutlined />}
                      loading={annotatingByAi}
                      onClick={handleAiAnnotate}
                    >
                      一键AI标注
                    </Button>
                    <Radio.Group
                      value={imageViewMode}
                      onChange={(event) => setImageViewMode(event.target.value)}
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
                    />
                  </Space>
                  }
                >
                <PlatformProTable<VideoDatasetItemRecord, Record<string, any>>
                  actionRef={imageActionRef}
                  persistenceKey="video-dataset-image-items-table"
                  rowKey="id"
                  search={false}
                  options={false}
                  request={requestImageItems}
                  toolBarRender={false}
                  columns={imageTableColumns}
                  pagination={{
                    defaultPageSize: imagePageInfo.pageSize,
                    showSizeChanger: true,
                  }}
                  tableRender={(_, defaultDom) =>
                    imageViewMode === 'table' ? (
                      defaultDom
                    ) : (
                      <>
                        <div
                          style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                            gap: 16,
                          }}
                        >
                          {imageItems.map((record) => {
                            const usage = getUsageMeta(record);
                            return (
                        <Card
                          key={record.id}
                          hoverable
                          onClick={() => selectPreviewImage(record)}
                          styles={{ body: { padding: 16 } }}
                        >
                                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                                  <div
                                    style={{
                                      width: '100%',
                                      height: 220,
                                      borderRadius: 8,
                                      overflow: 'hidden',
                                      background: '#f5f5f5',
                                    }}
                                  >
                                    <ManagedFileImagePreview
                                      fileId={record.fileId}
                                      alt={record.fileName}
                                      style={{ width: '100%', height: '100%', objectFit: 'contain' }}
                                    />
                                  </div>
                                  <Space wrap>
                                    {record.annotated ? <Tag color="success">已标注</Tag> : <Tag>未标注</Tag>}
                                    <Tag color={usage.color}>{usage.text}</Tag>
                                  </Space>
                                  <div style={{ fontWeight: 500 }}>{record.fileName}</div>
                                  <div>上传时间：{record.createdAt ? new Date(record.createdAt).toLocaleString() : '-'}</div>
                                  <Space wrap>
                                    <Button
                                      type="link"
                                      icon={<EditOutlined />}
                                      style={{ paddingInline: 0 }}
                                      onClick={(event) => {
                                        event.stopPropagation();
                                        openUsageModal(record);
                                      }}
                                    >
                                      更改
                                    </Button>
                                    <Button
                                      danger
                                      type="link"
                                      icon={<DeleteOutlined />}
                                      style={{ paddingInline: 0 }}
                                      onClick={(event) => {
                                        event.stopPropagation();
                                        modal.confirm({
                                          title: `确认删除图片“${record.fileName}”吗？`,
                                          onOk: async () => {
                                            await deleteVideoDatasetItem(record.id);
                                            await refreshSummary();
                                          },
                                        });
                                      }}
                                    >
                                      删除
                                    </Button>
                                  </Space>
                                </Space>
                              </Card>
                            );
                          })}
                        </div>
                        <Pagination
                          style={{ marginTop: 16, textAlign: 'right' }}
                          current={imagePageInfo.current}
                          pageSize={imagePageInfo.pageSize}
                          total={imagePageInfo.total}
                          showSizeChanger
                          onChange={(current, pageSize) => {
                            imageActionRef.current?.setPageInfo?.({ current, pageSize });
                          }}
                        />
                      </>
                    )
                  }
                />
              </Card>
            ),
          },
          {
            key: 'videos',
            label: '视频数据源',
            children: (
              <Card
                extra={
                  <Space>
                    <Button type="primary" icon={<PlusOutlined />} onClick={openCreateVideoItem}>
                      新增视频
                    </Button>
                    <Radio.Group
                      value={videoViewMode}
                      onChange={(event) => setVideoViewMode(event.target.value)}
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
                    />
                  </Space>
                }
              >
                <PlatformProTable<VideoDatasetItemRecord, Record<string, any>>
                  actionRef={videoActionRef}
                  persistenceKey="video-dataset-video-items-table"
                  rowKey="id"
                  search={false}
                  options={false}
                  request={requestVideoItems}
                  toolBarRender={false}
                  columns={videoTableColumns}
                  pagination={{
                    defaultPageSize: videoPageInfo.pageSize,
                    showSizeChanger: true,
                  }}
                  tableRender={(_, defaultDom) =>
                    videoViewMode === 'table' ? (
                      defaultDom
                    ) : (
                      <>
                        {videoItems.length ? (
                          <div
                            style={{
                              display: 'grid',
                              gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                              gap: 16,
                            }}
                          >
                            {videoItems.map((record) => (
                              <Card key={record.id} hoverable styles={{ body: { padding: 16 } }}>
                                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                                  <div
                                    style={{
                                      position: 'relative',
                                      width: '100%',
                                      height: 200,
                                      borderRadius: 10,
                                      overflow: 'hidden',
                                      background: '#0f172a',
                                      cursor: 'pointer',
                                    }}
                                    onClick={() => openPlayVideo(record)}
                                  >
                                    <ManagedFileImagePreview
                                      fileId={record.coverFileId || record.fileId}
                                      alt={record.fileName}
                                      preview={false}
                                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                    />
                                    <div
                                      style={{
                                        position: 'absolute',
                                        inset: 0,
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        background: 'rgba(15,23,42,0.18)',
                                      }}
                                    >
                                      <PlayCircleOutlined style={{ fontSize: 42, color: '#fff' }} />
                                    </div>
                                  </div>
                                  <div style={{ fontSize: 16, fontWeight: 600 }}>{record.fileName}</div>
                                  <Space wrap>
                                    <Tag color="blue">{record.sourceType === 'FRAME_EXTRACT' ? '抽帧生成' : '手动上传'}</Tag>
                                    <Tag>{record.createdAt ? new Date(record.createdAt).toLocaleString() : '-'}</Tag>
                                  </Space>
                                  <Space wrap>
                                    <Button type="link" icon={<PlayCircleOutlined />} style={{ paddingInline: 0 }} onClick={() => openPlayVideo(record)}>
                                      播放
                                    </Button>
                                    <Button type="link" icon={<BorderOutlined />} style={{ paddingInline: 0 }} onClick={() => handleOpenFrameExtract(record)}>
                                      抽帧
                                    </Button>
                                    <Button type="link" icon={<EditOutlined />} style={{ paddingInline: 0 }} onClick={() => openEditVideoItem(record)}>
                                      修改
                                    </Button>
                                    <Button
                                      danger
                                      type="link"
                                      icon={<DeleteOutlined />}
                                      style={{ paddingInline: 0 }}
                                      onClick={() => confirmDeleteVideoItem(record, '视频')}
                                    >
                                      删除
                                    </Button>
                                  </Space>
                                </Space>
                              </Card>
                            ))}
                          </div>
                        ) : (
                          <Empty description="暂无数据" />
                        )}
                        <Pagination
                          style={{ marginTop: 16, textAlign: 'right' }}
                          current={videoPageInfo.current}
                          pageSize={videoPageInfo.pageSize}
                          total={videoPageInfo.total}
                          showSizeChanger
                          onChange={(current, pageSize) => {
                            videoActionRef.current?.setPageInfo?.({ current, pageSize });
                          }}
                        />
                      </>
                    )
                  }
                />
              </Card>
            ),
          },
          {
            key: 'annotation',
            label: '数据标注',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card bodyStyle={{ padding: 0 }}>
                  {imageItems.length === 0 || !currentImageItem ? (
                    <Empty description="请先在图片数据集中上传图片" />
                  ) : (
                    <div>
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: '120px minmax(0, 1fr) 320px',
                          minHeight: 720,
                        }}
                      >
                        <div
                          style={{
                            padding: 16,
                            background: '#f8fafc',
                            borderRight: '1px solid #e5e7eb',
                          }}
                        >
                          <Space direction="vertical" size={12} style={{ width: '100%' }}>
                            <Button
                              icon={<BorderOutlined />}
                              type={drawMode === 'RECTANGLE' ? 'primary' : 'default'}
                              block
                              size="large"
                              style={{
                                height: 72,
                                borderRadius: 14,
                                fontWeight: 600,
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 6,
                                boxShadow: drawMode === 'RECTANGLE' ? '0 8px 20px rgba(22,119,255,0.22)' : '0 4px 12px rgba(15,23,42,0.06)',
                              }}
                              onClick={() => {
                                setDrawMode('RECTANGLE');
                                setRectDraft([]);
                                setPolygonDraft([]);
                                setDrawingRect(undefined);
                              }}
                            >
                              矩形
                            </Button>
                            <Button
                              icon={<ClusterOutlined />}
                              type={drawMode === 'POLYGON' ? 'primary' : 'default'}
                              block
                              size="large"
                              style={{
                                height: 72,
                                borderRadius: 14,
                                fontWeight: 600,
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 6,
                                boxShadow: drawMode === 'POLYGON' ? '0 8px 20px rgba(22,119,255,0.22)' : '0 4px 12px rgba(15,23,42,0.06)',
                              }}
                              onClick={() => {
                                setDrawMode('POLYGON');
                                setRectDraft([]);
                                setPolygonDraft([]);
                                setDrawingRect(undefined);
                              }}
                            >
                              多边形
                            </Button>
                            {drawMode === 'POLYGON' ? (
                              <>
                                <Typography.Text
                                  style={{
                                    display: 'block',
                                    fontSize: 12,
                                    color: '#64748b',
                                    textAlign: 'center',
                                  }}
                                >
                                  提示：绘制多边形时可鼠标右键封闭多边形
                                </Typography.Text>
                                <Button
                                  block
                                  size="middle"
                                  style={{
                                    height: 40,
                                    borderRadius: 8,
                                    fontWeight: 500,
                                    fontSize: 13,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    paddingInline: 8,
                                    background: '#ffffff',
                                    boxShadow: 'none',
                                  }}
                                  onClick={() => {
                                    setRectDraft([]);
                                    setPolygonDraft([]);
                                    setDrawingRect(undefined);
                                  }}
                                >
                                  清空绘制
                                </Button>
                                <Button
                                  type="primary"
                                  block
                                  size="middle"
                                  style={{
                                    height: 40,
                                    borderRadius: 8,
                                    fontWeight: 500,
                                    fontSize: 13,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    paddingInline: 8,
                                    boxShadow: '0 2px 8px rgba(22,119,255,0.16)',
                                  }}
                                  onClick={finishPolygon}
                                >
                                  保存绘制
                                </Button>
                              </>
                            ) : null}
                          </Space>
                        </div>

                        <div
                          style={{
                            background: '#1f2937',
                            padding: 20,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 16,
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                            <Tag color="blue" style={{ marginInlineEnd: 0 }}>
                              {drawMode === 'RECTANGLE' ? '矩形标注模式' : '多边形标注模式'}
                            </Tag>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                              <Space size={8}>
                                <span style={{ color: '#fff' }}>仅未标注</span>
                                <Switch checked={onlyUnannotated} onChange={setOnlyUnannotated} />
                              </Space>
                              <div
                                style={{
                                  padding: '6px 14px',
                                  borderRadius: 999,
                                  background: 'rgba(15, 23, 42, 0.8)',
                                  color: '#fff',
                                  fontWeight: 600,
                                }}
                              >
                                当前图片：{currentImageOrder || 0}/{imagePagination.total || imagePageInfo.total || imageItems.length}
                              </div>
                            </div>
                            <Tag color={annotations.length > 0 ? 'success' : 'default'} style={{ marginInlineEnd: 0 }}>
                              {annotations.length > 0 ? '已完成标注' : '未标注'}
                            </Tag>
                          </div>

                          <div
                            style={{
                              flex: 1,
                              minHeight: 0,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              overflow: 'auto',
                            }}
                          >
                            {currentImageUrl ? (
                              <div style={{ display: 'inline-block', position: 'relative', maxWidth: '100%' }}>
                                <img
                                  src={currentImageUrl}
                                  alt={currentImageItem.fileName}
                                  style={{ display: 'block', maxWidth: '100%', maxHeight: 620, width: 'auto', height: 'auto' }}
                                  onLoad={(event) => {
                                    const target = event.currentTarget;
                                    setImageNaturalSize({
                                      width: target.naturalWidth || 1280,
                                      height: target.naturalHeight || 720,
                                    });
                                  }}
                                />
                                <svg
                                  ref={svgRef}
                                  viewBox={`0 0 ${imageNaturalSize.width} ${imageNaturalSize.height}`}
                                  style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', cursor: 'crosshair' }}
                                  onMouseDown={(event) => {
                                    if (drawMode !== 'RECTANGLE') {
                                      return;
                                    }
                                    const point = svgPoint(event);
                                    if (!point) {
                                      return;
                                    }
                                    setDrawingRect(point);
                                    setRectDraft([point, point, point, point]);
                                  }}
                                  onMouseMove={(event) => {
                                    if (drawMode !== 'RECTANGLE' || !drawingRect) {
                                      return;
                                    }
                                    const point = svgPoint(event);
                                    if (!point) {
                                      return;
                                    }
                                    const left = Math.min(drawingRect.x, point.x);
                                    const right = Math.max(drawingRect.x, point.x);
                                    const top = Math.min(drawingRect.y, point.y);
                                    const bottom = Math.max(drawingRect.y, point.y);
                                    setRectDraft([
                                      { x: left, y: top },
                                      { x: right, y: top },
                                      { x: right, y: bottom },
                                      { x: left, y: bottom },
                                    ]);
                                  }}
                                  onMouseUp={() => {
                                    if (drawMode !== 'RECTANGLE' || rectDraft.length !== 4) {
                                      setDrawingRect(undefined);
                                      return;
                                    }
                                    const width = Math.abs(rectDraft[1].x - rectDraft[0].x);
                                    const height = Math.abs(rectDraft[2].y - rectDraft[1].y);
                                    setDrawingRect(undefined);
                                    if (width < 5 || height < 5) {
                                      setRectDraft([]);
                                      return;
                                    }
                                    saveAnnotation(rectDraft, 'RECTANGLE').catch(() => undefined);
                                  }}
                                  onClick={(event) => {
                                    if (drawMode !== 'POLYGON') {
                                      return;
                                    }
                                    const point = svgPoint(event);
                                    if (!point) {
                                      return;
                                    }
                                    setPolygonDraft((prev) => [...prev, point]);
                                  }}
                                  onContextMenu={(event) => {
                                    if (drawMode !== 'POLYGON') {
                                      return;
                                    }
                                    event.preventDefault();
                                    finishPolygon();
                                  }}
                                >
                                  {annotationRows.map((annotation) => {
                                    const color = annotation.labelColor || '#1677ff';
                                    const labelLayout = getAnnotationLabelLayout(annotation.points, annotation.labelName);
                                    const active = selectedAnnotationId === annotation.id;
                                    return (
                                      <g key={annotation.id}>
                                        {annotation.annotationType === 'RECTANGLE' ? (
                                          <polygon
                                            points={toPolylinePoints(annotation.points)}
                                            fill={active ? `${color}55` : `${color}33`}
                                            stroke={color}
                                            strokeWidth={active ? 5 : 3}
                                          />
                                        ) : (
                                          <polyline
                                            points={`${toPolylinePoints(annotation.points)} ${
                                              annotation.points[0]
                                                ? `${annotation.points[0].x},${annotation.points[0].y}`
                                                : ''
                                            }`}
                                            fill={active ? `${color}44` : `${color}22`}
                                            stroke={color}
                                            strokeWidth={active ? 5 : 3}
                                          />
                                        )}
                                        {labelLayout ? (
                                          <>
                                            <rect
                                              x={labelLayout.x}
                                              y={labelLayout.y}
                                              width={labelLayout.width}
                                              height={labelLayout.height}
                                              rx={6}
                                              fill={color}
                                            />
                                            <text
                                              x={labelLayout.textX}
                                              y={labelLayout.textY}
                                              fill="#fff"
                                              fontSize="13"
                                              fontWeight="600"
                                            >
                                              {annotation.labelName || '未命名标签'}
                                            </text>
                                          </>
                                        ) : null}
                                      </g>
                                    );
                                  })}
                                  {rectDraft.length === 4 ? (
                                    <polygon points={toPolylinePoints(rectDraft)} fill="#1677ff22" stroke="#1677ff" strokeWidth={2} />
                                  ) : null}
                                  {polygonDraft.length > 0 ? (
                                    <polyline points={toPolylinePoints(polygonDraft)} fill="none" stroke="#fa8c16" strokeWidth={3} />
                                  ) : null}
                                  {polygonDraft.map((point, index) => (
                                    <circle key={`${point.x}-${point.y}-${index}`} cx={point.x} cy={point.y} r={6} fill="#fa8c16" />
                                  ))}
                                </svg>
                              </div>
                            ) : (
                              <Empty description="图片预览加载中" />
                            )}
                          </div>
                        </div>

                        <div
                          style={{
                            padding: 16,
                            background: '#fff',
                            borderLeft: '1px solid #f0f0f0',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 16,
                          }}
                        >
                          <Card
                            size="small"
                            styles={{
                              body: {
                                background: '#111827',
                                color: '#fff',
                                borderRadius: 8,
                              },
                            }}
                          >
                            <Space direction="vertical" size={8} style={{ width: '100%' }}>
                              <div style={{ fontWeight: 700 }}>{annotations.length > 0 ? '已完成标注' : '待补充标注'}</div>
                              <div>{currentImageItem.fileName}</div>
                              <div>上传时间：{currentImageItem.createdAt ? new Date(currentImageItem.createdAt).toLocaleString() : '-'}</div>
                              <div>已标注 {annotationRows.length} 个对象</div>
                            </Space>
                          </Card>

                          <div>
                            <div style={{ marginBottom: 12, fontWeight: 600 }}>标签管理</div>
                            <div style={{ display: 'grid', gap: 8 }}>
                              {annotationLabelStats.map((item) => (
                                <Button
                                  key={item.id}
                                  block
                                  type={selectedLabelId === item.id ? 'primary' : 'default'}
                                  onClick={() => setSelectedLabelId(item.id)}
                                  style={{
                                    justifyContent: 'space-between',
                                    height: 42,
                                  }}
                                >
                                  <Space>
                                    <span
                                      style={{
                                        width: 12,
                                        height: 12,
                                        borderRadius: '50%',
                                        background: item.labelColor,
                                        display: 'inline-block',
                                      }}
                                    />
                                    {item.labelName}
                                  </Space>
                                  <span>{item.count}</span>
                                </Button>
                              ))}
                            </div>
                          </div>

                          <div style={{ flex: 1, minHeight: 0 }}>
                            <div style={{ marginBottom: 12, fontWeight: 600 }}>对象图层 ({annotationRows.length})</div>
                            <div style={{ display: 'grid', gap: 8, maxHeight: 320, overflowY: 'auto' }}>
                              {annotationRows.length ? (
                                annotationRows.map((record, index) => (
                                  <Card
                                    key={record.id}
                                    hoverable
                                    size="small"
                                    bodyStyle={{ padding: 12 }}
                                    style={{
                                      borderColor: selectedAnnotationId === record.id ? record.labelColor : undefined,
                                      boxShadow:
                                        selectedAnnotationId === record.id ? `0 0 0 2px ${record.labelColor}33 inset` : undefined,
                                      cursor: 'pointer',
                                    }}
                                    onClick={() => setSelectedAnnotationId(record.id)}
                                  >
                                    <Space
                                      align="start"
                                      style={{ width: '100%', justifyContent: 'space-between' }}
                                    >
                                      <Space direction="vertical" size={4}>
                                        <Space>
                                          <span
                                            style={{
                                              width: 12,
                                              height: 12,
                                              borderRadius: '50%',
                                              background: record.labelColor,
                                              display: 'inline-block',
                                            }}
                                          />
                                          <span>{`${record.labelName} #${index + 1}`}</span>
                                        </Space>
                                        <span style={{ color: '#6b7280', fontSize: 12 }}>
                                          {record.annotationType === 'RECTANGLE' ? '矩形标注' : '多边形标注'}
                                        </span>
                                      </Space>
                                      <Button
                                        danger
                                        type="link"
                                        icon={<DeleteOutlined />}
                                        onClick={() =>
                                          modal.confirm({
                                            title: '确认删除该标注吗？',
                                            onOk: async () => {
                                              await deleteVideoDatasetAnnotation(record.id);
                                              await Promise.all([loadAnnotations(currentImageItem.id), refreshSummary()]);
                                            },
                                          })
                                        }
                                      />
                                    </Space>
                                  </Card>
                                ))
                              ) : (
                                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前图片暂无标注对象" />
                              )}
                            </div>
                          </div>
                        </div>
                      </div>

                      <div
                        style={{
                          padding: 16,
                          background: '#f8fafc',
                          borderTop: '1px solid #e5e7eb',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                        }}
                      >
                        <List
                          style={{ width: 'fit-content', maxWidth: '100%' }}
                          grid={{ gutter: 12, xs: 2, sm: 4, md: 6, lg: 8 }}
                          dataSource={imageItems}
                          locale={{ emptyText: <Empty description="暂无图片" /> }}
                          renderItem={(item, index) => (
                            <List.Item>
                              <Card
                                hoverable
                                size="small"
                                bodyStyle={{ padding: 8 }}
                                style={{
                                  borderColor: item.id === currentImageItemId ? '#1677ff' : undefined,
                                  cursor: 'pointer',
                                }}
                                onClick={() => selectPreviewImage(item, index)}
                              >
                                <div style={{ position: 'relative', width: '100%', height: 72, overflow: 'hidden', borderRadius: 6 }}>
                                  <Tag
                                    color={item.annotated ? 'success' : 'warning'}
                                    style={{
                                      position: 'absolute',
                                      top: 4,
                                      left: 4,
                                      zIndex: 1,
                                      marginInlineEnd: 0,
                                    }}
                                  >
                                    {item.annotated ? '已标注' : '未标注'}
                                  </Tag>
                                  <ManagedFileImagePreview
                                    fileId={item.fileId}
                                    alt={item.fileName}
                                    preview={false}
                                    style={{ width: '100%', height: 72, objectFit: 'cover', display: 'block' }}
                                  />
                                </div>
                              </Card>
                            </List.Item>
                          )}
                        />

                        <div style={{ marginTop: 8, display: 'flex', justifyContent: 'center', width: '100%' }}>
                          <Pagination
                            current={imagePagination.pageNo}
                            pageSize={imagePagination.pageSize}
                            total={imagePagination.total}
                            onChange={(pageNo, pageSize) => {
                              imageActionRef.current?.setPageInfo?.({ current: pageNo, pageSize });
                              loadImageItems(pageNo, pageSize).catch(() => undefined);
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  )}
                </Card>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title="更改数据集用途"
        open={usageOpen}
        confirmLoading={usageSubmitting}
        onOk={() => {
          handleSubmitImageUsage().catch(() => undefined);
        }}
        onCancel={() => {
          setUsageOpen(false);
          setUsageEditingItem(undefined);
        }}
        destroyOnHidden
      >
        <Form form={usageForm} layout="vertical">
          <Form.Item label="图片文件" style={{ marginBottom: 12 }}>
            <Typography.Text>{usageEditingItem?.fileName || '-'}</Typography.Text>
          </Form.Item>
          <Form.Item name="usage" label="数据集用途" rules={[{ required: true, message: '请选择数据集用途' }]}>
            <Radio.Group
              options={DATASET_USAGE_OPTIONS.map((item) => ({ label: item.label, value: item.value }))}
              optionType="button"
              buttonStyle="solid"
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="按比例划分数据集用途"
        open={splitOpen}
        confirmLoading={splittingUsage}
        onOk={() => {
          handleSplitUsage().catch(() => undefined);
        }}
        onCancel={() => setSplitOpen(false)}
        destroyOnHidden
      >
        <Alert
          showIcon
          type="info"
          message="默认比例为 train=0.7、val=0.2、test=0.1，三者之和必须等于 1，多出的图片会自动归入测试集。"
          style={{ marginBottom: 16 }}
        />
        <Form form={splitForm} layout="vertical" initialValues={{ trainRatio: 0.7, valRatio: 0.2, testRatio: 0.1 }}>
          <Form.Item
            name="trainRatio"
            label="训练集比例"
            rules={[{ required: true, message: '请输入训练集比例' }]}
          >
            <InputNumber min={0} max={1} step={0.1} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="valRatio"
            label="验证集比例"
            rules={[{ required: true, message: '请输入验证集比例' }]}
          >
            <InputNumber min={0} max={1} step={0.1} precision={2} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="testRatio"
            label="测试集比例"
            rules={[{ required: true, message: '请输入测试集比例' }]}
          >
            <InputNumber min={0} max={1} step={0.1} precision={2} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="AI标注进度"
        open={aiTaskOpen}
        onCancel={() => setAiTaskOpen(false)}
        footer={[
          !aiTask ? (
            <Button
              key="start-task"
              type="primary"
              loading={annotatingByAi}
              onClick={() => {
                handleStartAiAnnotate().catch(() => undefined);
              }}
            >
              开始
            </Button>
          ) : null,
          getAiTaskStatusValue(aiTask?.status) === 'PENDING' || getAiTaskStatusValue(aiTask?.status) === 'RUNNING' ? (
            <Button
              key="cancel-task"
              danger
              onClick={() => {
                handleCancelAiAnnotate().catch(() => undefined);
              }}
              disabled={!!aiTask?.cancelRequested}
            >
              {aiTask?.cancelRequested ? '取消中...' : '取消标注'}
            </Button>
          ) : null,
          <Button key="close" type="primary" onClick={() => setAiTaskOpen(false)}>
            关闭
          </Button>,
        ]}
      >
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          {!aiTask ? (
            <Alert showIcon type="info" message="确认后点击“开始”，系统将异步执行一键AI标注并在此处展示进度。" />
          ) : null}
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="任务状态">
              <Tag
                color={
                  getAiTaskStatusValue(aiTask?.status) === 'COMPLETED'
                    ? 'success'
                    : getAiTaskStatusValue(aiTask?.status) === 'FAILED'
                      ? 'error'
                      : getAiTaskStatusValue(aiTask?.status) === 'CANCELLED'
                        ? 'default'
                        : 'processing'
                }
              >
                {typeof aiTask?.status === 'string' ? aiTask.status : aiTask?.status?.text || '-'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="当前图片">{aiTask?.currentItemName || '-'}</Descriptions.Item>
            <Descriptions.Item label="已处理/总数">
              {aiTask?.processedCount || 0}/{aiTask?.totalCount || 0}
            </Descriptions.Item>
            <Descriptions.Item label="新增标注数">{aiTask?.annotationCount || 0}</Descriptions.Item>
            <Descriptions.Item label="成功标注图片">{aiTask?.annotatedItems || 0}</Descriptions.Item>
            <Descriptions.Item label="跳过/失败">
              {(aiTask?.skippedCount || 0) + '/' + (aiTask?.failedCount || 0)}
            </Descriptions.Item>
          </Descriptions>
          <Progress percent={aiTask?.progress || 0} status={getAiTaskStatusValue(aiTask?.status) === 'FAILED' ? 'exception' : undefined} />
          {aiTask?.message ? <Alert showIcon type="info" message={aiTask.message} /> : null}
        </Space>
      </Modal>

      <Modal
        destroyOnClose
        open={labelOpen}
        title={labelEditingId ? '修改标签' : '新增标签'}
        onCancel={() => setLabelOpen(false)}
        onOk={() => {
          handleLabelSubmit().catch(() => undefined);
        }}
      >
        <Form form={labelForm} layout="vertical">
          <Form.Item name="labelName" label="标签名称" rules={[{ required: true, message: '请输入标签名称' }]}>
            <Input maxLength={80} />
          </Form.Item>
          <Form.Item
            name="labelColor"
            label="标签颜色"
            rules={[{ required: true, message: '请输入标签颜色' }]}
            initialValue="#1677ff"
          >
            <Input type="color" />
          </Form.Item>
          <Form.Item name="description" label="标签描述">
            <Input.TextArea rows={3} maxLength={300} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        destroyOnClose
        open={!!playingVideoItem}
        title={playingVideoItem?.fileName || '播放视频'}
        footer={null}
        width={960}
        onCancel={() => setPlayingVideoItem(undefined)}
      >
        <div style={{ background: '#000', borderRadius: 10, overflow: 'hidden' }}>
          {currentVideoUrl ? (
            <video
              key={playingVideoItem?.id}
              src={currentVideoUrl}
              controls
              autoPlay
              style={{ width: '100%', maxHeight: 560, display: 'block' }}
            />
          ) : (
            <Empty description="视频加载中" style={{ padding: '80px 0', background: '#111827' }} />
          )}
        </div>
      </Modal>

      <Modal
        destroyOnClose
        confirmLoading={itemSubmitting}
        open={itemOpen}
        title={itemEditing ? '修改视频' : '新增视频'}
        onCancel={() => {
          setItemOpen(false);
          setItemEditing(undefined);
          itemForm.resetFields();
        }}
        onOk={() => {
          handleSubmitVideoItem().catch(() => undefined);
        }}
      >
        <Form form={itemForm} layout="vertical">
          <Form.Item
            label="视频名称"
            name="fileName"
            rules={[{ required: true, message: '请输入视频名称' }]}
          >
            <Input maxLength={255} placeholder="请输入视频名称" />
          </Form.Item>
          <Form.Item
            label="视频文件"
            required
            extra={currentEditingVideoName ? `当前文件：${currentEditingVideoName}` : '仅支持 mp4 文件'}
          >
            <Upload accept=".mp4,video/mp4" maxCount={1} showUploadList={false} customRequest={(options) => {
              handleVideoFileUpload(options).catch(() => undefined);
            }}>
              <Button icon={<UploadOutlined />}>{currentEditingVideoFileId ? '重新上传 mp4' : '上传 mp4'}</Button>
            </Upload>
          </Form.Item>
          <Form.Item
            name="fileId"
            rules={[{ required: true, message: '请上传视频文件' }]}
            style={{ display: 'none' }}
          >
            <Input />
          </Form.Item>
          <Form.Item name="uploadedVideoName" style={{ display: 'none' }}>
            <Input />
          </Form.Item>
          <Form.Item name="contentType" style={{ display: 'none' }}>
            <Input />
          </Form.Item>
          <Form.Item name="fileSize" style={{ display: 'none' }}>
            <Input />
          </Form.Item>
          <Form.Item name="coverFileId" label="视频封面">
            <VideoCoverUploadField uploadParams={{ subPath: datasetVideoUploadSubPath }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        destroyOnClose
        confirmLoading={extractingFrames}
        open={frameOpen}
        title="视频抽帧"
        okText="开始抽帧"
        onCancel={() => setFrameOpen(false)}
        onOk={() => {
          handleExtractFrames().catch(() => undefined);
        }}
      >
        <Form form={frameForm} layout="vertical">
          <Form.Item
            name="videoItemId"
            label="视频文件"
            rules={[{ required: true, message: '请选择视频文件' }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={videoItems.map((item) => ({
                label: item.fileName,
                value: item.id,
              }))}
              placeholder="请选择要抽帧的视频"
            />
          </Form.Item>
          <Form.Item
            name="intervalSeconds"
            label="抽帧间隔"
            rules={[{ required: true, message: '请设置抽帧间隔' }]}
          >
            <Slider min={0.5} max={10} step={0.5} marks={{ 0.5: '0.5秒', 1: '1秒', 5: '5秒', 10: '10秒' }} />
          </Form.Item>
          <Form.Item shouldUpdate noStyle>
            {() => (
              <div style={{ marginTop: -8, marginBottom: 16, color: '#64748b' }}>
                当前抽帧间隔：{frameForm.getFieldValue('intervalSeconds') || 1} 秒
              </div>
            )}
          </Form.Item>
          <Form.Item
            name="imageQuality"
            label="图像质量"
            rules={[{ required: true, message: '请设置图像质量' }]}
          >
            <Slider min={20} max={100} step={1} marks={{ 20: '20', 80: '80', 100: '100' }} />
          </Form.Item>
          <Form.Item shouldUpdate noStyle>
            {() => (
              <div style={{ marginTop: -8, color: '#64748b' }}>
                当前图像质量：{frameForm.getFieldValue('imageQuality') || 80}
              </div>
            )}
          </Form.Item>
        </Form>
      </Modal>
    </PageContainer>
  );
};

export default DatasetDetailPage;
