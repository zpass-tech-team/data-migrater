package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.util.FixedListQueue;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.mosip.packet.core.constant.GlobalConfig.TIMECONSUPTIONQUEUE;
import static io.mosip.packet.core.constant.GlobalConfig.TOTAL_RECORDS_FOR_PROCESS;

public class CustomizedThreadPoolExecutor {
    List<ThreadPoolExecutor> poolMap = new ArrayList<>();
    private int MAX_THREAD_EXE_COUNT;
    private Long DELAY_SECONDS = 30000L;
    private int maxThreadCount;
    private boolean noSlotAvailable=false;
    private long totalTaskCount = 0;
    private long totalCompletedTaskCount = 0;
    private Timer watch = null;
    private Timer estimateTimer = null;
    private String NAME;
    private FixedListQueue<Long> timeConsumptionPerMin = new FixedListQueue<>(100);
    private FixedListQueue<Integer> countOfProcessPerMin = new FixedListQueue<>(100);

    public CustomizedThreadPoolExecutor(Integer threadPoolCount, Integer maxThreadCount, Integer maxThreadExecCount, String poolName) {
        this.NAME = poolName;
        this.maxThreadCount = maxThreadCount;
        this.MAX_THREAD_EXE_COUNT = maxThreadExecCount;
        for(int i = 1; i <= threadPoolCount; i++)
            poolMap.add((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));

        estimateTimer = new Timer("Estimate Time Calculator");
        estimateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(TIMECONSUPTIONQUEUE != null && TIMECONSUPTIONQUEUE.size() > 0) {
                    Long avgTime = 0l;
                    Long[] consumedTimeList = TIMECONSUPTIONQUEUE.toArray(new Long[TIMECONSUPTIONQUEUE.size()]);
                    Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                    int noOfRecords = consumedTimeList.length;
                    avgTime = TotalSum / noOfRecords;
                    timeConsumptionPerMin.add(avgTime);
                    countOfProcessPerMin.add(noOfRecords);
                    TIMECONSUPTIONQUEUE.clear();
                }
            }
        }, 0, 60000L);

        watch = new Timer("ThreadPool_Wathcer");
        watch.schedule(new TimerTask() {
            @Override
            public void run() {
                if(noSlotAvailable) {
                    boolean isSuccess = false;
                    for(ThreadPoolExecutor entry : poolMap) {
                        if(entry.getActiveCount() ==0) {
                            totalTaskCount += entry.getTaskCount();
                            totalCompletedTaskCount += entry.getCompletedTaskCount();
                            entry.shutdown();
                            entry.purge();
                            poolMap.remove(entry);
                            poolMap.add((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));
                            isSuccess=true;
                        }
                    }

                    if(isSuccess)
                        noSlotAvailable=false;
                }

                try {
                    Collections.sort(poolMap, new SortbyCount());
                } catch (ConcurrentModificationException e){}

                Long totalCount = 0L;
                Long activeCount = 0L;
                Long completedCount = 0L;
                int totalDays = 0;
                int totalHours = 0;
                int remainingMinutes =0;
                Long avgTime = 0l;

                for(ThreadPoolExecutor entry : poolMap) {
                    totalCount += entry.getTaskCount();
                    activeCount+= entry.getActiveCount();
                    completedCount+= entry.getCompletedTaskCount();
                }

                if(totalTaskCount > 0 || totalCount > 0) {
                    // Calculating Estimated Time of Process Completion
                    if(timeConsumptionPerMin != null && timeConsumptionPerMin.size() > 0) {
                        Long[] consumedTimeList = timeConsumptionPerMin.toArray(new Long[timeConsumptionPerMin.size()]);
                        Long totalRecords = TOTAL_RECORDS_FOR_PROCESS;
                        Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                        int noOfRecords = consumedTimeList.length;

                        Integer[] consumedCountList = countOfProcessPerMin.toArray(new Integer[countOfProcessPerMin.size()]);
                        Integer TotalCountSum = Arrays.stream(consumedCountList).mapToInt(Integer::intValue).sum();
                        int noOfCountRecords = consumedCountList.length;
                        int avgCount = TotalCountSum/noOfCountRecords;

                        Long remainingRecords = totalRecords - completedCount;
                        avgTime = TotalSum / noOfRecords;
                        Long totalTimeRequired = (remainingRecords / avgCount);

                        totalHours = (int) (totalTimeRequired / 60);
                        totalDays = (int) totalHours / 24;
                        totalHours = (int) (totalHours % 24);
                        remainingMinutes = (int) (totalTimeRequired % 60);
                    }

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
                for(ThreadPoolExecutor entry : poolMap) {
                    threadCount++;
                    if(entry.getTaskCount() < maxThreadCount) {
                        entry.execute(task);
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
            for(ThreadPoolExecutor entry : poolMap) {
                if(entry.getActiveCount() > 0) {
                    isCompleted = false;
                    break;
                }
            }
            TimeUnit.SECONDS.sleep(10);
        }

        if(isCompleted) {
            watch.cancel();
        }
        return isCompleted;
    }

    class SortbyCount implements Comparator<ThreadPoolExecutor> {
        public int compare(ThreadPoolExecutor a, ThreadPoolExecutor b)
        {
            return Long.valueOf(a.getTaskCount()).compareTo(Long.valueOf(b.getTaskCount()));
        }
    }

    public boolean isBatchAcceptRequest() {
        return !noSlotAvailable;
    }
}
