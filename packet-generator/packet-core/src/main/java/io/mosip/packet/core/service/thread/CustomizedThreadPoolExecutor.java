package io.mosip.packet.core.service.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomizedThreadPoolExecutor {
    Map<String, ThreadPoolExecutor> poolMap = new HashMap<>();
    private int MAX_THREAD_EXE_COUNT=10;
    private Long DELAY_SECONDS = 10000L;
    private int maxThreadCount;
    private boolean noSlotAvailable=false;
    private long totalTaskCount = 0;
    private long totalCompletedTaskCount = 0;

    public CustomizedThreadPoolExecutor(Integer threadPoolCount, Integer maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        for(int i = 1; i <= threadPoolCount; i++)
            poolMap.put("ThreadPool" + i, (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));

        Timer monitor = new Timer("ThreadPool_Monitor");
        monitor.schedule(new TimerTask() {
            @Override
            public void run() {
                if(noSlotAvailable) {
                    boolean isSuccess = false;
                    for(Map.Entry<String, ThreadPoolExecutor> entry : poolMap.entrySet()) {
                        if(entry.getValue().getActiveCount() ==0) {
                            totalTaskCount += entry.getValue().getTaskCount();
                            totalCompletedTaskCount += entry.getValue().getCompletedTaskCount();
                            entry.getValue().shutdown();
                            entry.getValue().purge();
                            poolMap.put(entry.getKey(), (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));
                            isSuccess=true;
                        }
                    }

                    if(isSuccess)
                        noSlotAvailable=false;
                }
            }
        }, 0, DELAY_SECONDS);

        Timer watch = new Timer("ThreadPool_Wathcer");
        watch.schedule(new TimerTask() {
            @Override
            public void run() {
                Long totalCount = 0L;
                Long activeCount = 0L;
                Long completedCount = 0L;

                for(Map.Entry<String, ThreadPoolExecutor> entry : poolMap.entrySet()) {
                    totalCount += entry.getValue().getTaskCount();
                    activeCount+= entry.getValue().getActiveCount();
                    completedCount+= entry.getValue().getCompletedTaskCount();
                }
                System.out.println("Total Task : " + (totalTaskCount +totalCount)  + ", Active Task : " + activeCount + ", Completed Task : " + (totalCompletedTaskCount+completedCount));
            }
        }, 0, DELAY_SECONDS);
    }

    public void ExecuteTask(Runnable task) throws InterruptedException {
        int threadCount = 0;
        boolean taskAdded = false;

        do {
            if(!noSlotAvailable)
                for(Map.Entry<String, ThreadPoolExecutor> entry : poolMap.entrySet()) {
                    threadCount++;
                    if(entry.getValue().getTaskCount() < maxThreadCount) {
                        entry.getValue().execute(task);
                        taskAdded=true;
                        break;
                    } else if(threadCount == maxThreadCount){
                        noSlotAvailable = true;
                    }
                }
            TimeUnit.SECONDS.sleep(10);
        } while (noSlotAvailable || !taskAdded);
    }

    public boolean isTaskCompleted() throws InterruptedException {
        boolean isCompleted = false;
        while(!isCompleted) {
            isCompleted = true;
            for(Map.Entry<String, ThreadPoolExecutor> entry : poolMap.entrySet()) {
                if(entry.getValue().getActiveCount() > 0) {
                    isCompleted = false;
                    break;
                }
            }
            TimeUnit.SECONDS.sleep(10);
        }
        return isCompleted;
    }
}
