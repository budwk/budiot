import {
  AudioMutedOutlined,
  CameraOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  SoundOutlined,
} from '@ant-design/icons';
import { Button, Space } from 'antd';
import * as React from 'react';
import { getStoredToken } from '@/utils/session';

export type VideoPlayerSourceType = 'auto' | 'jessibuca' | 'mp4' | 'ts';

export type VideoPlayerProps = {
  src?: string;
  sourceType?: VideoPlayerSourceType;
  isLive?: boolean;
  autoPlay?: boolean;
  controls?: boolean;
  autoHideControls?: boolean;
  showScreenshotButton?: boolean;
  muted?: boolean;
  hasAudio?: boolean;
  className?: string;
  style?: React.CSSProperties;
  videoStyle?: React.CSSProperties;
  onError?: (message?: string) => void;
};

type JessibucaConfig = {
  container: HTMLElement | string;
  decoder?: string;
  autoWasm?: boolean;
  background?: string;
  controlAutoHide?: boolean;
  debug?: boolean;
  forceNoOffscreen?: boolean;
  hasAudio?: boolean;
  heartTimeout?: number;
  heartTimeoutReplay?: boolean;
  heartTimeoutReplayTimes?: number;
  hiddenAutoPause?: boolean;
  hotKey?: boolean;
  isFlv?: boolean;
  isFullResize?: boolean;
  isNotMute?: boolean;
  isResize?: boolean;
  keepScreenOn?: boolean;
  loadingText?: string;
  loadingTimeout?: number;
  loadingTimeoutReplay?: boolean;
  loadingTimeoutReplayTimes?: number;
  openWebglAlignment?: boolean;
  operateBtns?: {
    fullscreen?: boolean;
    screenshot?: boolean;
    play?: boolean;
    audio?: boolean;
    record?: boolean;
  };
  recordType?: string;
  rotate?: number;
  showBandwidth?: boolean;
  supportDblclickFullscreen?: boolean;
  timeout?: number;
  useMSE?: boolean;
  useWCS?: boolean;
  useWebFullScreen?: boolean;
  videoBuffer?: number;
  wasmDecodeErrorReplay?: boolean;
  wcsUseVideoRender?: boolean;
};

type JessibucaEventMap = {
  play: [];
  pause: [];
  load: [];
  error: [unknown];
  timeout: [unknown];
  loadingTimeout: [];
  mute: [boolean];
  fullscreen: [boolean];
  kBps: [number];
};

type JessibucaInstance = {
  play: (url?: string) => Promise<void>;
  pause: () => Promise<void>;
  destroy: () => void;
  mute: () => void;
  cancelMute: () => void;
  screenshot: (filename?: string, format?: string, quality?: number, type?: string) => void;
  setFullscreen: (flag: boolean) => void;
  resize: () => void;
  isMute: () => boolean;
  on: <T extends keyof JessibucaEventMap>(event: T, callback: (...args: JessibucaEventMap[T]) => void) => void;
};

type JessibucaConstructor = new (config?: JessibucaConfig) => JessibucaInstance;

declare global {
  interface Window {
    Jessibuca?: JessibucaConstructor;
  }
}

const JESSIBUCA_SCRIPT_SRC = '/static/js/jessibuca/jessibuca.js';
const JESSIBUCA_DECODER_SRC = '/static/js/jessibuca/decoder.js';

let jessibucaScriptPromise: Promise<void> | undefined;

const formatTraffic = (trafficKB: number) => {
  if (trafficKB >= 1024 * 1024) {
    return `${(trafficKB / (1024 * 1024)).toFixed(2)} GB`;
  }
  if (trafficKB >= 1024) {
    return `${(trafficKB / 1024).toFixed(1)} MB`;
  }
  return `${Math.max(0, Math.round(trafficKB))} KB`;
};

const appendPlayToken = (src?: string) => {
  if (!src) {
    return src;
  }
  const token = getStoredToken();
  if (!token) {
    return src;
  }
  try {
    const url = new URL(src, window.location.origin);
    if (!/^https?:|^wss?:/i.test(url.protocol)) {
      return src;
    }
    url.searchParams.set('token', token);
    if (/^(https?:)?\/\//i.test(src)) {
      return url.toString();
    }
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return src;
  }
};

const ensureJessibucaScript = () => {
  if (window.Jessibuca) {
    return Promise.resolve();
  }

  if (jessibucaScriptPromise) {
    return jessibucaScriptPromise;
  }

  jessibucaScriptPromise = new Promise<void>((resolve, reject) => {
    const existingScript = document.querySelector<HTMLScriptElement>(`script[src="${JESSIBUCA_SCRIPT_SRC}"]`);
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(), { once: true });
      existingScript.addEventListener('error', () => reject(new Error('Jessibuca 脚本加载失败')), { once: true });
      return;
    }

    const script = document.createElement('script');
    script.src = JESSIBUCA_SCRIPT_SRC;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Jessibuca 脚本加载失败'));
    document.body.appendChild(script);
  });

  return jessibucaScriptPromise;
};

const VideoPlayer: React.FC<VideoPlayerProps> = ({
  src,
  autoPlay = true,
  controls = true,
  autoHideControls = false,
  showScreenshotButton = false,
  muted = true,
  hasAudio = false,
  className,
  style,
  videoStyle,
  onError,
}) => {
  const containerRef = React.useRef<HTMLDivElement>(null);
  const playerRef = React.useRef<JessibucaInstance | undefined>(undefined);
  const [errorMessage, setErrorMessage] = React.useState<string>();
  const [playing, setPlaying] = React.useState(false);
  const [isMuted, setIsMuted] = React.useState(muted || !hasAudio);
  const [isFullscreen, setIsFullscreen] = React.useState(false);
  const [bandwidth, setBandwidth] = React.useState(0);
  const [trafficKB, setTrafficKB] = React.useState(0);
  const [controlsVisible, setControlsVisible] = React.useState(!autoHideControls);
  const resolvedSrc = React.useMemo(() => appendPlayToken(src), [src]);
  const containerKey = React.useMemo(() => resolvedSrc || 'video-player-empty', [resolvedSrc]);
  const bandwidthTickAtRef = React.useRef<number | undefined>(undefined);
  const controlsHideTimerRef = React.useRef<number | undefined>(undefined);
  const onErrorRef = React.useRef<typeof onError>(onError);

  React.useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  React.useEffect(() => {
    setControlsVisible(!autoHideControls);
    if (controlsHideTimerRef.current) {
      window.clearTimeout(controlsHideTimerRef.current);
      controlsHideTimerRef.current = undefined;
    }
  }, [autoHideControls, resolvedSrc]);

  React.useEffect(() => () => {
    if (controlsHideTimerRef.current) {
      window.clearTimeout(controlsHideTimerRef.current);
    }
  }, []);

  React.useEffect(() => {
    let cancelled = false;

    const destroyPlayer = () => {
      setPlaying(false);
      setBandwidth(0);
      setTrafficKB(0);
      bandwidthTickAtRef.current = undefined;
      if (playerRef.current) {
        try {
          playerRef.current.destroy();
        } finally {
          playerRef.current = undefined;
        }
      }
      if (containerRef.current) {
        containerRef.current.innerHTML = '';
      }
    };

    const setPlayerError = (message: string) => {
      if (cancelled) {
        return;
      }
      setErrorMessage(message);
      onErrorRef.current?.(message);
    };

    destroyPlayer();
    setErrorMessage(undefined);
    onErrorRef.current?.(undefined);
    setIsMuted(muted || !hasAudio);

    if (!resolvedSrc || !containerRef.current) {
      return () => {
        cancelled = true;
        destroyPlayer();
      };
    }

    const createPlayer = async () => {
      try {
        await ensureJessibucaScript();
        if (cancelled || !containerRef.current || !window.Jessibuca) {
          return;
        }

        const player = new window.Jessibuca({
          container: containerRef.current,
          autoWasm: true,
          background: '',
          controlAutoHide: false,
          debug: true,
          decoder: JESSIBUCA_DECODER_SRC,
          forceNoOffscreen: false,
          hasAudio,
          heartTimeout: 5,
          heartTimeoutReplay: true,
          heartTimeoutReplayTimes: 3,
          hiddenAutoPause: false,
          hotKey: true,
          isFlv: false,
          isFullResize: false,
          isNotMute: hasAudio && !muted,
          isResize: false,
          keepScreenOn: true,
          loadingText: '请稍等，视频加载中...',
          loadingTimeout: 10,
          loadingTimeoutReplay: true,
          loadingTimeoutReplayTimes: 3,
          openWebglAlignment: false,
          operateBtns: {
            fullscreen: false,
            screenshot: false,
            play: false,
            audio: false,
            record: false,
          },
          recordType: 'mp4',
          rotate: 0,
          showBandwidth: false,
          supportDblclickFullscreen: false,
          timeout: 10,
          useMSE: true,
          useWCS: window.location.hostname === 'localhost' || window.location.protocol === 'https:',
          useWebFullScreen: true,
          videoBuffer: 0.2,
          wasmDecodeErrorReplay: true,
          wcsUseVideoRender: true,
        });

        playerRef.current = player;

        player.on('play', () => {
          if (cancelled) {
            return;
          }
          setPlaying(true);
          setErrorMessage(undefined);
          onErrorRef.current?.(undefined);
          setIsMuted(player.isMute());
        });
        player.on('pause', () => !cancelled && setPlaying(false));
        player.on('mute', (value) => !cancelled && setIsMuted(value));
        player.on('fullscreen', (value) => !cancelled && setIsFullscreen(value));
        player.on('kBps', (value) => {
          if (cancelled) {
            return;
          }
          const now = Date.now();
          const rounded = Math.max(0, Math.round(value));
          setBandwidth(rounded);
          if (bandwidthTickAtRef.current) {
            const elapsedSeconds = Math.max((now - bandwidthTickAtRef.current) / 1000, 0);
            setTrafficKB((current) => current + rounded * elapsedSeconds);
          }
          bandwidthTickAtRef.current = now;
        });
        player.on('error', (error) => {
          const detail = typeof error === 'string' ? error : 'Jessibuca 播放失败';
          setPlayerError(detail);
        });
        player.on('timeout', () => setPlayerError('视频连接超时'));
        player.on('loadingTimeout', () => setPlayerError('视频加载超时'));

        if (autoPlay) {
          await player.play(resolvedSrc);
        }
      } catch (error) {
        setPlayerError((error as Error).message || '播放器初始化失败');
      }
    };

    const handleResize = () => {
      playerRef.current?.resize();
    };

    const resizeObserver =
      typeof ResizeObserver !== 'undefined' && containerRef.current
        ? new ResizeObserver(() => {
            playerRef.current?.resize();
          })
        : undefined;

    window.addEventListener('resize', handleResize);
    if (resizeObserver && containerRef.current) {
      resizeObserver.observe(containerRef.current);
    }
    void createPlayer();

    return () => {
      cancelled = true;
      window.removeEventListener('resize', handleResize);
      resizeObserver?.disconnect();
      destroyPlayer();
    };
  }, [autoPlay, hasAudio, muted, resolvedSrc]);

  const handleTogglePlay = async () => {
    const player = playerRef.current;
    if (!player) {
      return;
    }

    if (playing) {
      await player.pause().catch(() => undefined);
      return;
    }

    await player.play(resolvedSrc).catch((error) => {
      const message = (error as Error).message || '视频播放失败';
      setErrorMessage(message);
      onErrorRef.current?.(message);
    });
  };

  const handleToggleMute = () => {
    const player = playerRef.current;
    if (!player || !hasAudio) {
      return;
    }

    if (isMuted) {
      player.cancelMute();
      setIsMuted(false);
      return;
    }

    player.mute();
    setIsMuted(true);
  };

  const handleToggleFullscreen = () => {
    const player = playerRef.current;
    if (!player) {
      return;
    }
    player.setFullscreen(!isFullscreen);
  };

  const handleScreenshot = () => {
    playerRef.current?.screenshot(`snapshot-${Date.now()}`, 'png', 0.92, 'download');
  };

  const scheduleControlsHide = React.useCallback(() => {
    if (!controls || !autoHideControls) {
      return;
    }
    if (controlsHideTimerRef.current) {
      window.clearTimeout(controlsHideTimerRef.current);
    }
    controlsHideTimerRef.current = window.setTimeout(() => {
      setControlsVisible(false);
    }, 1800);
  }, [autoHideControls, controls]);

  const showControls = React.useCallback(() => {
    if (!controls) {
      return;
    }
    setControlsVisible(true);
    scheduleControlsHide();
  }, [controls, scheduleControlsHide]);

  const hideControlsImmediately = React.useCallback(() => {
    if (!controls || !autoHideControls) {
      return;
    }
    if (controlsHideTimerRef.current) {
      window.clearTimeout(controlsHideTimerRef.current);
      controlsHideTimerRef.current = undefined;
    }
    setControlsVisible(false);
  }, [autoHideControls, controls]);

  return (
    <div
      className={className}
      onMouseMove={showControls}
      onMouseEnter={showControls}
      onMouseLeave={hideControlsImmediately}
      style={{
        background: '#000',
        borderRadius: 8,
        overflow: 'hidden',
        minHeight: 320,
        position: 'relative',
        ...style,
      }}
    >
      <div
        key={containerKey}
        ref={containerRef}
        className="video-player-container"
        style={{
          width: '100%',
          height: '100%',
          minHeight: 320,
          background: '#000',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          ...videoStyle,
        }}
        onDoubleClick={handleToggleFullscreen}
      />
      <style>{`
        .video-player-container video,
        .video-player-container canvas {
          margin: auto;
        }
      `}</style>
      {!controls && showScreenshotButton && src ? (
        <Button
          type="text"
          icon={<CameraOutlined />}
          onClick={handleScreenshot}
          style={{
            position: 'absolute',
            right: 12,
            bottom: 12,
            zIndex: 3,
            color: '#fff',
            background: 'rgba(43, 51, 63, 0.7)',
          }}
        />
      ) : null}
      {controls ? (
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '8px 12px',
            background: 'rgba(43, 51, 63, 0.7)',
            opacity: controlsVisible ? 1 : 0,
            pointerEvents: controlsVisible ? 'auto' : 'none',
            transition: 'opacity 0.2s ease',
          }}
        >
          <Space size={4}>
            <Button
              type="text"
              icon={playing ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
              onClick={() => void handleTogglePlay()}
              style={{ color: '#fff' }}
            />
            <Button
              type="text"
              icon={isMuted || !hasAudio ? <AudioMutedOutlined /> : <SoundOutlined />}
              onClick={handleToggleMute}
              disabled={!hasAudio}
              style={{ color: '#fff' }}
            />
          </Space>
          <Space size={8}>
            <span style={{ color: '#fff', minWidth: 160, textAlign: 'right' }}>
              {bandwidth} kb/s · 流量 {formatTraffic(trafficKB)}
            </span>
            <Button type="text" icon={<CameraOutlined />} onClick={handleScreenshot} style={{ color: '#fff' }} />
            <Button
              type="text"
              icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
              onClick={handleToggleFullscreen}
              style={{ color: '#fff' }}
            />
          </Space>
        </div>
      ) : null}
      {errorMessage ? (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 24,
            color: '#fff',
            background: 'rgba(0, 0, 0, 0.55)',
            textAlign: 'center',
            pointerEvents: 'none',
          }}
        >
          {errorMessage}
        </div>
      ) : null}
    </div>
  );
};

export default VideoPlayer;
