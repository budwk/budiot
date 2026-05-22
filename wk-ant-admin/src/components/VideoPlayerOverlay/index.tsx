import { CameraOutlined } from '@ant-design/icons';
import { Tag, Typography } from 'antd';
import * as React from 'react';
import VideoPlayer from '@/components/VideoPlayer';

type Point = { x: number; y: number };

export type VideoPlayerOverlayRegion = {
  id?: string;
  name?: string;
  taskName?: string;
  modelName?: string;
  polygonPoints: string;
  enabled?: boolean;
  color?: string;
};

export type VideoPlayerOverlayDetection = {
  id?: string;
  taskId?: string;
  taskName?: string;
  modelName?: string;
  className?: string;
  confidence?: number;
  bbox: number[];
  color?: string;
};

export type VideoPlayerOverlayProps = {
  src?: string;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  footer?: React.ReactNode;
  overlays?: VideoPlayerOverlayRegion[];
  detections?: VideoPlayerOverlayDetection[];
  selected?: boolean;
  onClick?: () => void;
  minHeight?: number;
  viewBoxWidth?: number;
  viewBoxHeight?: number;
  controls?: boolean;
  autoHideControls?: boolean;
  showScreenshotButton?: boolean;
  muted?: boolean;
  hasAudio?: boolean;
  onError?: (message?: string) => void;
  emptyText?: React.ReactNode;
  extra?: React.ReactNode;
};

const OVERLAY_COLORS = ['#1d9bf0', '#13c2c2', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#eb2f96'];

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

const toSvgPoints = (points: Point[]) => points.map((item) => `${item.x},${item.y}`).join(' ');

const getCenteredLabelLayout = (points: Point[], label?: string) => {
  if (!points.length || !label) {
    return undefined;
  }
  const minX = Math.min(...points.map((item) => item.x));
  const maxX = Math.max(...points.map((item) => item.x));
  const minY = Math.min(...points.map((item) => item.y));
  const maxY = Math.max(...points.map((item) => item.y));
  const width = Math.max(90, label.length * 14 + 26);
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

const renderCenteredLabel = (points: Point[], label?: string, color = '#1d9bf0') => {
  const layout = getCenteredLabelLayout(points, label);
  if (!layout) {
    return null;
  }
  return (
    <g>
      <rect x={layout.x} y={layout.y} width={layout.width} height={layout.height} rx={8} fill={color} fillOpacity={0.92} />
      <text
        x={layout.textX}
        y={layout.textY}
        fill="#fff"
        fontSize="13"
        fontWeight="600"
        textAnchor="middle"
        dominantBaseline="middle"
      >
        {label}
      </text>
    </g>
  );
};

const VideoPlayerOverlay: React.FC<VideoPlayerOverlayProps> = ({
  src,
  title,
  subtitle,
  footer,
  overlays,
  detections,
  selected = false,
  onClick,
  minHeight = 260,
  viewBoxWidth = 1280,
  viewBoxHeight = 720,
  controls = false,
  autoHideControls = false,
  showScreenshotButton = false,
  muted = true,
  hasAudio = false,
  onError,
  emptyText,
  extra,
}) => {
  const normalizedOverlays = React.useMemo(
    () =>
      (overlays || [])
        .filter((item) => item.enabled !== false)
        .map((item, index) => ({
          ...item,
          color: item.color || OVERLAY_COLORS[index % OVERLAY_COLORS.length],
          points: parsePolygonPoints(item.polygonPoints),
        }))
        .filter((item) => item.points.length >= 3),
    [overlays],
  );
  const normalizedDetections = React.useMemo(
    () =>
      (detections || [])
        .filter((item) => Array.isArray(item.bbox) && item.bbox.length >= 4)
        .map((item, index) => ({
          ...item,
          color: item.color || OVERLAY_COLORS[index % OVERLAY_COLORS.length],
          bbox: item.bbox.map((value) => Number(value || 0)),
        })),
    [detections],
  );

  return (
    <div
      onClick={onClick}
      style={{
        position: 'relative',
        minHeight,
        height: '100%',
        borderRadius: 12,
        overflow: 'hidden',
        border: `1px solid ${selected ? '#3ea6ff' : 'rgba(62, 166, 255, 0.28)'}`,
        background: '#020b1f',
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      <VideoPlayer
        src={src}
        sourceType="auto"
        isLive
        controls={controls}
        autoHideControls={autoHideControls}
        showScreenshotButton={showScreenshotButton}
        muted={muted}
        hasAudio={hasAudio}
        onError={onError}
        style={{ minHeight, height: '100%', borderRadius: 0 }}
        videoStyle={{ minHeight, height: '100%' }}
      />
      <div
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          top: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 12px',
          pointerEvents: 'none',
        }}
      >
        <div style={{ minWidth: 0 }}>
          <Typography.Text
            style={{
              color: '#fff',
              fontWeight: 600,
              display: 'block',
            }}
            ellipsis
          >
            {title || '未分配摄像头'}
          </Typography.Text>
          {subtitle ? (
            <Typography.Text style={{ color: 'rgba(255,255,255,0.72)', fontSize: 12 }} ellipsis>
              {subtitle}
            </Typography.Text>
          ) : null}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, pointerEvents: 'auto' }}>
          {normalizedOverlays.length ? (
            <Tag color="processing" style={{ marginInlineEnd: 0 }}>
              AI区域 {normalizedOverlays.length}
            </Tag>
          ) : normalizedDetections.length ? (
            <Tag color="error" style={{ marginInlineEnd: 0 }}>
              实时命中 {normalizedDetections.length}
            </Tag>
          ) : null}
          {extra}
        </div>
      </div>
      {normalizedOverlays.length || normalizedDetections.length ? (
        <svg
          viewBox={`0 0 ${viewBoxWidth} ${viewBoxHeight}`}
          preserveAspectRatio="none"
          style={{
            position: 'absolute',
            inset: 0,
            width: '100%',
            height: '100%',
            pointerEvents: 'none',
          }}
        >
          {normalizedOverlays.map((item) => {
            const label = item.modelName || item.name || item.taskName;
            return (
              <g key={item.id || `${item.name}-${item.modelName}`}>
                <polygon
                  points={toSvgPoints(item.points)}
                  fill={item.color}
                  fillOpacity={0.14}
                  stroke={item.color}
                  strokeWidth={3}
                />
                {renderCenteredLabel(item.points, label, item.color)}
              </g>
            );
          })}
          {normalizedDetections.map((item, index) => {
            const [x1, y1, x2, y2] = item.bbox;
            const width = Math.max(1, x2 - x1);
            const height = Math.max(1, y2 - y1);
            const label = [item.taskName, item.className || item.modelName]
              .filter(Boolean)
              .join(' / ');
            const score = typeof item.confidence === 'number' ? `${Math.round(item.confidence * 100)}%` : '';
            const labelText = [label, score].filter(Boolean).join(' ');
            return (
              <g key={item.id || `${item.taskId}-${item.className}-${x1}-${y1}-${index}`}>
                <rect x={x1} y={y1} width={width} height={height} fill="transparent" stroke={item.color} strokeWidth={3} />
                {labelText ? (
                  <g>
                    <rect
                      x={x1}
                      y={Math.max(0, y1 - 28)}
                      width={Math.max(96, labelText.length * 8 + 20)}
                      height={24}
                      rx={6}
                      fill={item.color}
                      fillOpacity={0.94}
                    />
                    <text x={x1 + 10} y={Math.max(0, y1 - 12)} fill="#fff" fontSize="12" fontWeight="600">
                      {labelText}
                    </text>
                  </g>
                ) : null}
              </g>
            );
          })}
        </svg>
      ) : null}
      {!src ? (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 24,
            pointerEvents: 'none',
            flexDirection: 'column',
            gap: 10,
          }}
        >
          <CameraOutlined style={{ color: 'rgba(255,255,255,0.75)', fontSize: 42 }} />
          {emptyText ? (
            <Typography.Text style={{ color: 'rgba(255,255,255,0.78)', fontSize: 15 }}>{emptyText}</Typography.Text>
          ) : null}
        </div>
      ) : null}
      {footer ? (
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: controls ? 44 : 0,
            padding: '8px 12px',
            color: 'rgba(255,255,255,0.82)',
            pointerEvents: 'none',
          }}
        >
          {footer}
        </div>
      ) : null}
    </div>
  );
};

export default VideoPlayerOverlay;
