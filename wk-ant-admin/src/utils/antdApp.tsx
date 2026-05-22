import { App } from 'antd';
import * as React from 'react';

type AntdAppInstance = ReturnType<typeof App.useApp>;

let currentAntdApp: AntdAppInstance | null = null;
let pendingTasks: Array<(app: AntdAppInstance) => void> = [];

const runWithAntdApp = <T,>(runner: (app: AntdAppInstance) => T): T | undefined => {
  if (currentAntdApp) {
    return runner(currentAntdApp);
  }
  pendingTasks.push((app) => {
    runner(app);
  });
  return undefined;
};

export const AntdAppHolder: React.FC = () => {
  const antdApp = App.useApp();

  React.useEffect(() => {
    currentAntdApp = antdApp;
    if (pendingTasks.length) {
      const tasks = pendingTasks;
      pendingTasks = [];
      for (const task of tasks) {
        task(antdApp);
      }
    }
    return () => {
      if (currentAntdApp === antdApp) {
        currentAntdApp = null;
      }
    };
  }, [antdApp]);

  return null;
};

export const appMessage = {
  success: (...args: Parameters<AntdAppInstance['message']['success']>) =>
    runWithAntdApp((app) => app.message.success(...args)),
  error: (...args: Parameters<AntdAppInstance['message']['error']>) =>
    runWithAntdApp((app) => app.message.error(...args)),
  warning: (...args: Parameters<AntdAppInstance['message']['warning']>) =>
    runWithAntdApp((app) => app.message.warning(...args)),
};

export const appNotification = {
  open: (...args: Parameters<AntdAppInstance['notification']['open']>) =>
    runWithAntdApp((app) => app.notification.open(...args)),
  success: (...args: Parameters<AntdAppInstance['notification']['success']>) =>
    runWithAntdApp((app) => app.notification.success(...args)),
  error: (...args: Parameters<AntdAppInstance['notification']['error']>) =>
    runWithAntdApp((app) => app.notification.error(...args)),
  warning: (...args: Parameters<AntdAppInstance['notification']['warning']>) =>
    runWithAntdApp((app) => app.notification.warning(...args)),
  destroy: (...args: Parameters<AntdAppInstance['notification']['destroy']>) =>
    runWithAntdApp((app) => app.notification.destroy(...args)),
};

export const appModal = {
  warning: (...args: Parameters<AntdAppInstance['modal']['warning']>) =>
    runWithAntdApp((app) => app.modal.warning(...args)),
};
