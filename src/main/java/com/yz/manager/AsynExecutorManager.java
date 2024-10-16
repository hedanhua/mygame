package com.yz.manager;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsynExecutorManager {

	private static final int MAX_QUEUE_SIZE = 300000;
	private static final AtomicLong sequence = new AtomicLong(0); // 顺序计数器
    private static ThreadPoolExecutor sqlExecutor = createSQLThreadPoolExecutor();

    private static ThreadPoolExecutor createSQLThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                2, // 核心线程数
                Runtime.getRuntime().availableProcessors() * 4,
                60, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(MAX_QUEUE_SIZE, (r1, r2) -> {
                    if (r1 instanceof OrderedRunnable && r2 instanceof OrderedRunnable) {
                        return ((OrderedRunnable) r1).compareTo((OrderedRunnable) r2);
                    }
                    return 0;
                })); // 使用 PriorityBlockingQueue 保持任务的顺序性
    }

    public static ThreadPoolExecutor getAsynSqlExecutor() {
        return sqlExecutor;
    }

    public static void executeSqlTask(Runnable runnable) {
        if (sqlExecutor.isShutdown()) {
            return;
        }
        OrderedRunnable orderedRunnable = new OrderedRunnable(runnable);
        sqlExecutor.execute(orderedRunnable);
    }

    private static class OrderedRunnable implements Runnable, Comparable<OrderedRunnable> {
        private final Runnable task;
        private final long order;

        public OrderedRunnable(Runnable task) {
            this.task = task;
            this.order = sequence.getAndIncrement();
        }

        @Override
        public void run() {
            task.run();
        }

        @Override
        public int compareTo(OrderedRunnable other) {
            return Long.compare(this.order, other.order);
        }
    }
    
    
    public static void destroy() {
    	try {
    		// 停止接受新任务
    		sqlExecutor.shutdown();
	        while(!sqlExecutor.isTerminated()){
				Thread.sleep(10);
	        }
	        sqlExecutor.shutdownNow();
	        log.warn("=============AsynExecutorManager destroy============");
    	} catch (Exception e) {
			log.error("AsynExecutorManager destroy异常",e);
		}
    }

}
