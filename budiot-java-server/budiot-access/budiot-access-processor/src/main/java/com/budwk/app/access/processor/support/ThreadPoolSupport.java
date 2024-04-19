package com.budwk.app.access.processor.support;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

public class ThreadPoolSupport {
    private static final Map<String, Executor> executors = new ConcurrentHashMap<>();

    public static Executor getProcessorExecutor() {
        return getExecutor("processor");
    }

    public static Executor getExecutor(String id) {
        return executors.computeIfAbsent(id, ThreadPoolSupport::buildExecutor);
    }

    public static Executor buildExecutor(String id) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        Executor executor = new ThreadPoolExecutor(availableProcessors, availableProcessors * 2,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10000),
                new NamedThreadFactory("executor." + id),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    private static void watch() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executors.forEach((k, exe) -> {

                });
            }
        }, 10000, 10000);

    }
}
