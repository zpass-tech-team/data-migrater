package io.mosip.packet.core.service.thread;

import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.mosip.packet.core.constant.GlobalConfig.TIMECONSUPTIONQUEUE;
import static io.mosip.packet.core.constant.GlobalConfig.TOTAL_RECORDS_FOR_PROCESS;

public class CustomizedThreadPoolExecutor {
    Map<String, ThreadPoolExecutor> poolMap = new HashMap<>();
    private int MAX_THREAD_EXE_COUNT;
    private Long DELAY_SECONDS = 10000L;
    private int maxThreadCount;
    private boolean noSlotAvailable=false;
    private long totalTaskCount = 0;
    private long totalCompletedTaskCount = 0;
    private Timer monitor = null;
    private Timer watch = null;
    private String NAME;

    public CustomizedThreadPoolExecutor(Integer threadPoolCount, Integer maxThreadCount, Integer maxThreadExecCount, String poolName) {
        this.NAME = poolName;
        this.maxThreadCount = maxThreadCount;
        this.MAX_THREAD_EXE_COUNT = maxThreadExecCount;
        for(int i = 1; i <= threadPoolCount; i++)
            poolMap.put("ThreadPool" + i, (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));

        monitor = new Timer("ThreadPool_Monitor");
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

        watch = new Timer("ThreadPool_Wathcer");
        watch.schedule(new TimerTask() {
            @Override
            public void run() {
                Long totalCount = 0L;
                Long activeCount = 0L;
                Long completedCount = 0L;
                int totalDays = 0;
                int totalHours = 0;
                int remainingMinutes =0;
                Long avgTime = 0l;

                for(Map.Entry<String, ThreadPoolExecutor> entry : poolMap.entrySet()) {
                    totalCount += entry.getValue().getTaskCount();
                    activeCount+= entry.getValue().getActiveCount();
                    completedCount+= entry.getValue().getCompletedTaskCount();

        // Calculating Estimated Time of Process Completion
                    if(TIMECONSUPTIONQUEUE != null && TIMECONSUPTIONQUEUE.size() > 0) {
                        Long[] consumedTimeList = TIMECONSUPTIONQUEUE.toArray(new Long[TIMECONSUPTIONQUEUE.size()]);
                        System.out.println((new Gson()).toJson(consumedTimeList));
                        Long totalRecords = TOTAL_RECORDS_FOR_PROCESS;
                        Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                        System.out.println("TotalSum" + TotalSum);
                        int noOfRecords = consumedTimeList.length;
                        System.out.println("noOfRecords" + noOfRecords);
                        Long remainingRecords = totalRecords - completedCount;
                        System.out.println("remainingRecords" + remainingRecords);
                        avgTime = TotalSum / noOfRecords;
                        Long timeRequired = avgTime * remainingRecords;
                        System.out.println("timeRequired" + timeRequired);
                        long convert = TimeUnit.MINUTES.convert(timeRequired, TimeUnit.NANOSECONDS);
                        System.out.println("convert" + convert);
                        totalHours = (int) (convert / 60);
                        totalDays = (int) totalHours / 24;
                        totalHours = (int) (totalHours % 24);
                        remainingMinutes = (int) (convert % 60);
                    }
                }

                if(totalTaskCount > 0 || totalCount > 0) {
                    System.out.println("Pool Name : " + NAME + " Avg Time : " + TimeUnit.SECONDS.convert(avgTime, TimeUnit.NANOSECONDS) + "S  Estimate Time of Completion : " + totalDays + "D " + totalHours + "H " + remainingMinutes + "M" +"  Total Records for Process : " + TOTAL_RECORDS_FOR_PROCESS + "  Total Task : " + (totalTaskCount +totalCount)  + ", Active Task : " + activeCount + ", Completed Task : " + (totalCompletedTaskCount+completedCount));
                }
            }
        }, 0, DELAY_SECONDS);
    }

    public void ExecuteTask(Runnable task) throws InterruptedException {
        int threadCount = 0;
        boolean taskAdded = false;

        do {
            if(!noSlotAvailable) {
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
            } else {
                TimeUnit.SECONDS.sleep(10);
            }
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

        if(isCompleted) {
            monitor.cancel();
            watch.cancel();
        }
        return isCompleted;
    }
}
