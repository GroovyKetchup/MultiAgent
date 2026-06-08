package ai.agent.service;

import ai.agent.enums.ThreadPoolType;
import ai.agent.factory.ThreadPoolFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GroupChatThreadPollManager {

    // 线程池注册器
    private static Map<ThreadPoolType, ExecutorService> POOL_REGISTRY = new ConcurrentHashMap<>();

    // 线程池-常规任务
    public static final String POLL_REGULAR_TASK = "regula-task";
    // 线程池-消息保存
    public static final String POLL_MSG_SAVE = "msg-save";
    // 线程池-运维日志
    public static final String POLL_OPS_LOG = "ops-log";

    static {

        initThreadPool();
    }

    private static void initThreadPool() {
        // 常规任务
        // 拒绝策略：CallerRuns
        enablePoolRegularTask();

        // 消息归档池
        // 拒绝策略：CallerRuns
        enablePoolMsgSave();

        // 运维日志池
        // 拒绝策略：Discard
        enablePoolOpsLog();

        // 优雅关机
        Runtime.getRuntime().addShutdownHook(new Thread(GroupChatThreadPollManager::shutdown));
    }


    // ========================= 线程池创建 =========================


    // 启用消息归档线程池
    private static void enablePoolMsgSave() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        POOL_REGISTRY.put(ThreadPoolType.MSG_ARCHIVE, ThreadPoolFactory.createThreadPool(
                POLL_MSG_SAVE,
                cpuCores * 2, cpuCores * 4, 2000,
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    // 启用常规任务线程池
    private static void enablePoolRegularTask() {
        POOL_REGISTRY.put(ThreadPoolType.REGULAR_TASK, ThreadPoolFactory.createThreadPool(
                POLL_REGULAR_TASK,
                5, 10, 500,
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    // 启用运维日志线程池
    private static void enablePoolOpsLog() {
        POOL_REGISTRY.put(ThreadPoolType.OPS_LOG, ThreadPoolFactory.createThreadPool(
                POLL_OPS_LOG,
                2, 4, 10000,
                new ThreadPoolExecutor.DiscardOldestPolicy()));
    }

    // ========================= 支撑方法 =========================

    // 获取普通线程池
    public static ExecutorService get(ThreadPoolType type) {
        if (POOL_REGISTRY == null || POOL_REGISTRY.isEmpty()) {
            POOL_REGISTRY = new ConcurrentHashMap<>();
            initThreadPool();
        }
        return POOL_REGISTRY.get(type);
    }


    // 统一销毁资源
    public static void shutdown() {
        System.out.println(">>> 开始关闭线程池资源...");
        POOL_REGISTRY.values().forEach(GroupChatThreadPollManager::gracefulShutdownSingle);
        System.out.println(">>> 线程池资源关闭完成。");
    }

    private static void gracefulShutdownSingle(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) return;
        try {
            executor.shutdown(); // 停止接收新任务
            // 等待现有任务执行，最多等 60 秒
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // 强制取消正在执行的任务
                // 再次等待
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("线程池无法正常关闭");
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}