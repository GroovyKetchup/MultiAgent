package ai.agent.factory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolFactory {

    // 构建标准线程池
    public static ThreadPoolExecutor createThreadPool(
            String poolName,
            int coreSize,
            int maxSize,
            int queueCapacity,
            RejectedExecutionHandler rejectPolicy) {

        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new NamedThreadFactory(poolName), // 自定义线程名
                rejectPolicy
        );
    }

    // 简单的命名线程工厂
   public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String name) {
            this.namePrefix = name + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) t.setDaemon(false); // 必须是用户线程，否则主线程退出子线程直接死
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}