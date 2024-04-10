package com.budwk.app.access.processor.timer;

/**
 * 延迟任务
 */
public interface DelayTaskHelper {

    void delayRun(Runnable runnable, long delaySeconds);

    void delayRunAsync(Runnable runnable, long delaySeconds);
}
