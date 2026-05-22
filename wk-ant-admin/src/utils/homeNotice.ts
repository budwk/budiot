const HOME_NOTICE_REFRESH_EVENT = 'budiot:home-msg-notice-refresh';

export const emitHomeNoticeRefresh = () => {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(HOME_NOTICE_REFRESH_EVENT));
};

export const addHomeNoticeRefreshListener = (listener: () => void) => {
  if (typeof window === 'undefined') {
    return () => undefined;
  }
  const handler = () => listener();
  window.addEventListener(HOME_NOTICE_REFRESH_EVENT, handler);
  return () => {
    window.removeEventListener(HOME_NOTICE_REFRESH_EVENT, handler);
  };
};
