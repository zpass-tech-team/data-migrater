package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.util.FixedListQueue;
import org.apache.commons.collections.ArrayStack;

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
                    FixedListQueue<Long> listQueue = (FixedListQueue<Long>) TIMECONSUPTIONQUEUE.clone();
                    Long avgTime = 0l;
                    Long[] consumedTimeList = listQueue.toArray(new Long[listQueue.size()]);
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
                System.out.println("Entering ThreadPool_Wathcer");
                if(noSlotAvailable) {
                    System.out.println("NO Slot Available is True");
                    boolean isSuccess = false;
                    List<ThreadPoolExecutor> poolMap1 = new ArrayList<>();
                    List<Integer> removeIndex = new ArrayList<>();
                    for(int i=0; i < poolMap.size(); i++) {
                        ThreadPoolExecutor entry = poolMap.get(i);
                        if(entry.getActiveCount() ==0) {
                            System.out.println("Initializing Thread Pool" );
                            totalTaskCount += entry.getTaskCount();
                            totalCompletedTaskCount += entry.getCompletedTaskCount();
                            removeIndex.add(i);
                            poolMap1.add((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));
                            isSuccess=true;
                        }
                    }

                    for(Integer i : removeIndex) {
                        System.out.println("Removing Pool" + i );
                        ThreadPoolExecutor entry = poolMap.get(i);
                        entry.shutdown();
                        entry.purge();
                        poolMap.remove(i);
                    }

                    System.out.println("Adding New Pol" + poolMap1.toArray() );
                    if(poolMap1.size() > 0)
                        poolMap.addAll(poolMap1);

                    System.out.println("After Adding New Pol" + poolMap.toArray() );
                    System.out.println("Is Success" + isSuccess );

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
                        FixedListQueue<Long> listQueue = (FixedListQueue<Long>) timeConsumptionPerMin.clone();
                        FixedListQueue<Integer> countQueue = (FixedListQueue<Integer>)countOfProcessPerMin.clone();

                        Long[] consumedTimeList = listQueue.toArray(new Long[listQueue.size()]);
                        System.out.println("Queue List Size " + consumedTimeList.length);
                        Long totalRecords = TOTAL_RECORDS_FOR_PROCESS;
                        Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                        int noOfRecords = consumedTimeList.length;

                        Integer[] consumedCountList = countQueue.toArray(new Integer[countQueue.size()]);
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

    public synchronized void ExecuteTask(Runnable task) throws InterruptedException {
        System.out.println("Entering Execute Taxk");
        if(!noSlotAvailable) {
            System.out.println("Slot Available");

            for(ThreadPoolExecutor entry : poolMap) {
                System.out.println("entry.getTaskCount()" + entry.getTaskCount());
                System.out.println("maxThreadCount" + maxThreadCount );
                if(entry.getTaskCount() < maxThreadCount) {
                    entry.execute(task);
                    break;
                }
            }

            boolean slotAvailable = false;
            for(ThreadPoolExecutor entry : poolMap) {
                if(entry.getTaskCount() < maxThreadCount)
                    slotAvailable = true;
            }

            if(!slotAvailable)
                    noSlotAvailable = true;
        }
    }

    public boolean isTaskCompleted() throws InterruptedException {
        boolean isCompleted = true;

        for(ThreadPoolExecutor entry : poolMap) {
            if(entry.getActiveCount() > 0) {
                isCompleted = false;
                break;
            }
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
