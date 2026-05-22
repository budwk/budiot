import { BellOutlined } from '@ant-design/icons';
import { history, useModel } from '@umijs/max';
import { App, Badge, Button, Empty, Popover, Tabs, Typography } from 'antd';
import { createStyles } from 'antd-style';
import dayjs from 'dayjs';
import * as React from 'react';
import {
  doReadAllHomeMsg,
  getHomeMsgList,
} from '@/services/budiot/home/msg';
import type {
  HomeMsgListRecord,
  HomeMsgNoticePayload,
} from '@/services/budiot/typing';
import {
  addHomeNoticeRefreshListener,
  emitHomeNoticeRefresh,
} from '@/utils/homeNotice';
import { redirectToLogin } from '@/utils/request';
import { getStoredToken } from '@/utils/session';
import { buildWebSocketUrl, PLATFORM_WEBSOCKET_PATH } from '@/utils/websocket';

type NoticeTabKey = 'all' | 'unread';

type NoticeState = {
  unreadCount: number;
  allList: HomeMsgListRecord[];
  unreadList: HomeMsgListRecord[];
};

const NOTICE_PAGE_SIZE = 5;

const useStyles = createStyles(({ token }) => ({
  action: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: 40,
    height: 40,
    color: token.colorTextSecondary,
    cursor: 'pointer',
    borderRadius: token.borderRadius,
    transition: 'all 0.2s ease',
    '&:hover': {
      backgroundColor: token.colorBgTextHover,
      color: token.colorPrimary,
    },
  },
  badge: {
    color: 'inherit',
    '.ant-badge-count': {
      boxShadow: 'none',
    },
  },
  panel: {
    width: 360,
  },
  tabs: {
    marginBottom: 12,
    '.ant-tabs-nav': {
      marginBottom: 12,
    },
    '.ant-tabs-nav-list': {
      display: 'flex',
      width: '100%',
    },
    '.ant-tabs-tab': {
      flex: 1,
      justifyContent: 'center',
      margin: 0,
      paddingInline: 0,
    },
    '.ant-tabs-tab-btn': {
      width: '100%',
      textAlign: 'center',
    },
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
  },
  listViewport: {
    maxHeight: 238,
    overflowY: 'auto',
    paddingRight: 4,
  },
  listItem: {
    paddingBottom: 12,
    borderBottom: `1px solid ${token.colorBorderSecondary}`,
    '&:last-child': {
      paddingBottom: 0,
      borderBottom: 'none',
    },
  },
  listButton: {
    display: 'block',
    width: '100%',
    height: 'auto',
    padding: 0,
    textAlign: 'left',
    whiteSpace: 'normal',
  },
  titleRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 4,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: '50%',
    background: token.colorError,
    flex: '0 0 auto',
  },
  title: {
    display: 'block',
    color: token.colorText,
    fontWeight: 400,
  },
  titleUnread: {
    color: token.colorTextHeading,
    fontWeight: 600,
  },
  titleRead: {
    color: token.colorTextDescription,
  },
  time: {
    color: token.colorTextDescription,
    fontSize: token.fontSizeSM,
  },
  timeRead: {
    color: token.colorTextQuaternary,
  },
  footer: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 12,
  },
}));

const emptyNoticeState: NoticeState = {
  unreadCount: 0,
  allList: [],
  unreadList: [],
};

const NoticeDropdown: React.FC = () => {
  const { styles, cx } = useStyles();
  const { initialState } = useModel('@@initialState');
  const { modal, notification } = App.useApp();
  const currentUser = initialState?.currentUser;
  const platformInfo = initialState?.platformInfo;
  const websocketEnabled = platformInfo?.AppWebSocket !== false;
  const [notice, setNotice] = React.useState<NoticeState>(emptyNoticeState);
  const [loading, setLoading] = React.useState(false);
  const [activeTab, setActiveTab] = React.useState<NoticeTabKey>('all');
  const [open, setOpen] = React.useState(false);
  const manualCloseRef = React.useRef(false);
  const reconnectCountRef = React.useRef(0);
  const reconnectTimerRef = React.useRef<number | undefined>(undefined);
  const heartbeatTimerRef = React.useRef<number | undefined>(undefined);
  const socketRef = React.useRef<WebSocket | null>(null);
  const offlineHandledRef = React.useRef(false);
  const suppressNextNoticePopupRef = React.useRef(true);

  const goToMessage = React.useCallback((item?: HomeMsgListRecord) => {
    const msgId = item?.msgid || item?.id;
    history.push(msgId ? `/platform/home/msg?id=${msgId}` : '/platform/home/msg');
    setOpen(false);
  }, []);

  const refreshNotice = React.useCallback(async () => {
    if (!currentUser?.id) {
      setNotice(emptyNoticeState);
      return;
    }
    setLoading(true);
    try {
      const [allResponse, unreadResponse] = await Promise.all([
        getHomeMsgList({
          status: 'all',
          pageNo: 1,
          pageSize: NOTICE_PAGE_SIZE,
          pageOrderName: 'sendAt',
          pageOrderBy: 'descending',
        }),
        getHomeMsgList({
          status: 'unread',
          pageNo: 1,
          pageSize: NOTICE_PAGE_SIZE,
          pageOrderName: 'sendAt',
          pageOrderBy: 'descending',
        }),
      ]);
      setNotice({
        unreadCount:
          unreadResponse.data?.totalCount || unreadResponse.data?.list?.length || 0,
        allList: allResponse.data?.list || [],
        unreadList: unreadResponse.data?.list || [],
      });
    } catch (error) {
      console.warn('[wk-ant-admin] failed to refresh home notices', error);
    } finally {
      setLoading(false);
    }
  }, [currentUser?.id]);

  const stopHeartbeat = React.useCallback(() => {
    if (heartbeatTimerRef.current) {
      window.clearInterval(heartbeatTimerRef.current);
      heartbeatTimerRef.current = undefined;
    }
  }, []);

  const closeSocket = React.useCallback(
    (sendLeft: boolean) => {
      const ws = socketRef.current;
      if (!ws) {
        return;
      }
      manualCloseRef.current = true;
      stopHeartbeat();
      if (sendLeft && ws.readyState === WebSocket.OPEN && currentUser?.id) {
        ws.send(
          JSON.stringify({
            userId: currentUser.id,
            action: 'left',
            token: getStoredToken(),
          }),
        );
      }
      ws.close();
      socketRef.current = null;
    },
    [currentUser?.id, stopHeartbeat],
  );

  const handleReadAll = React.useCallback(async () => {
    await doReadAllHomeMsg();
    emitHomeNoticeRefresh();
    await refreshNotice();
  }, [refreshNotice]);

  React.useEffect(() => {
    if (!currentUser?.id) {
      return;
    }
    void refreshNotice();
    return addHomeNoticeRefreshListener(() => {
      void refreshNotice();
    });
  }, [currentUser?.id, refreshNotice]);

  React.useEffect(() => {
    const token = getStoredToken();
    if (!websocketEnabled || !currentUser?.id || !token) {
      return;
    }

    let destroyed = false;

    const scheduleReconnect = () => {
      if (destroyed || manualCloseRef.current) {
        return;
      }
      if (reconnectCountRef.current >= 5) {
        notification.error({
          title: 'WebSocket 连接失败',
          description: '请检查网络连接，或刷新页面后重试。',
        });
        return;
      }
      reconnectCountRef.current += 1;
      reconnectTimerRef.current = window.setTimeout(() => {
        connect();
      }, 3000);
    };

    const startHeartbeat = (ws: WebSocket) => {
      stopHeartbeat();
      heartbeatTimerRef.current = window.setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send('{}');
          return;
        }
        stopHeartbeat();
      }, 30000);
    };

    const handleOffline = () => {
      if (offlineHandledRef.current) {
        return;
      }
      offlineHandledRef.current = true;
      closeSocket(false);
      modal.warning({
        title: '下线通知',
        content:
          '您的帐号在其他地方登录，您已被迫下线，如果不是您本人操作，请及时修改密码。',
        okText: '重新登录',
        mask: { closable: false },
        onOk: () => {
          redirectToLogin();
        },
      });
    };

    const handleMessage = (event: MessageEvent<string>) => {
      try {
        const payload = JSON.parse(event.data) as HomeMsgNoticePayload;
        if (payload.action === 'offline') {
          handleOffline();
          return;
        }
        if (payload.action === 'notice') {
          void refreshNotice();
          if (suppressNextNoticePopupRef.current) {
            suppressNextNoticePopupRef.current = false;
            return;
          }
          if ((payload.size || 0) > 0 && payload.notify) {
            notification.success({
              title: '站内通知',
              description: `您有 ${payload.size} 条新消息，请查收。`,
            });
          }
        }
      } catch (error) {
        console.warn('[wk-ant-admin] failed to parse websocket payload', error);
      }
    };

    const connect = () => {
      if (destroyed) {
        return;
      }
      const ws = new WebSocket(buildWebSocketUrl(PLATFORM_WEBSOCKET_PATH));
      manualCloseRef.current = false;
      suppressNextNoticePopupRef.current = true;
      socketRef.current = ws;

      ws.onopen = () => {
        reconnectCountRef.current = 0;
        offlineHandledRef.current = false;
        ws.send(
          JSON.stringify({
            userId: currentUser.id,
            action: 'join',
            token,
          }),
        );
        startHeartbeat(ws);
      };
      ws.onmessage = handleMessage;
      ws.onerror = () => undefined;
      ws.onclose = () => {
        stopHeartbeat();
        socketRef.current = null;
        scheduleReconnect();
      };
    };

    connect();

    return () => {
      destroyed = true;
      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = undefined;
      }
      closeSocket(true);
    };
  }, [closeSocket, currentUser?.id, modal, notification, refreshNotice, stopHeartbeat, websocketEnabled]);

  if (!currentUser?.id) {
    return null;
  }

  const currentList = activeTab === 'unread' ? notice.unreadList : notice.allList;

  return (
    <Popover
      placement="bottomRight"
      trigger="click"
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (nextOpen) {
          void refreshNotice();
        }
      }}
      content={
        <div className={styles.panel}>
          <Tabs
            className={styles.tabs}
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as NoticeTabKey)}
            items={[
              {
                key: 'all',
                label: '全部',
              },
              {
                key: 'unread',
                label: '未读',
              },
            ]}
          />
          {loading ? (
            <Typography.Text type="secondary">加载中...</Typography.Text>
          ) : currentList.length ? (
            <div className={styles.listViewport}>
              <div className={styles.list}>
                {currentList.map((item) => {
                  const unread = item.status === 0;
                  return (
                    <div key={item.id} className={styles.listItem}>
                      <Button
                        type="link"
                        className={styles.listButton}
                        onClick={() => goToMessage(item)}
                      >
                        <span className={styles.titleRow}>
                          {unread ? <span className={styles.unreadDot} /> : null}
                          <span
                            className={cx(
                              styles.title,
                              unread ? styles.titleUnread : styles.titleRead,
                            )}
                          >
                            {item.title}
                          </span>
                        </span>
                        <span
                          className={cx(
                            styles.time,
                            unread ? undefined : styles.timeRead,
                          )}
                        >
                          {item.sendat
                            ? dayjs(item.sendat).format('YYYY-MM-DD HH:mm:ss')
                            : '-'}
                        </span>
                      </Button>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={activeTab === 'unread' ? '暂无未读消息' : '暂无消息'}
            />
          )}
          <div className={styles.footer}>
            <Button
              type="link"
              disabled={!notice.unreadCount}
              onClick={() => void handleReadAll()}
            >
              全部已读
            </Button>
            <Button type="link" onClick={() => goToMessage()}>
              查看更多
            </Button>
          </div>
        </div>
      }
    >
      <span className={styles.action}>
        <Badge
          count={notice.unreadCount}
          overflowCount={99}
          size="small"
          className={styles.badge}
        >
          <BellOutlined style={{ fontSize: 18, color: 'inherit' }} />
        </Badge>
      </span>
    </Popover>
  );
};

export default NoticeDropdown;
