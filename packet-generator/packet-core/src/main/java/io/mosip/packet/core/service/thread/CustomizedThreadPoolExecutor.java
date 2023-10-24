package io.mosip.packet.core.service.thread;

import io.mosip.packet.core.util.FixedListQueue;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.mosip.packet.core.constant.GlobalConfig.*;

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
                    System.out.println("countOfProcessPerMin" + TIMECONSUPTIONQUEUE.size());
                    TIMECONSUPTIONQUEUE.clear();

                    Long avgTime = 0l;
                    Long[] consumedTimeList = listQueue.toArray(new Long[listQueue.size()]);

                    Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                    int noOfRecords = consumedTimeList.length;
                    System.out.println("Estimate TotalSum " + TotalSum);
                    System.out.println("Estimate No Of Records" + noOfRecords);

                    avgTime = TotalSum / noOfRecords;
                    System.out.println("Estimate Avg Records" + avgTime);

                    timeConsumptionPerMin.add(avgTime);
                    countOfProcessPerMin.add(noOfRecords);

                }
            }
        }, 0, 60000L);

        watch = new Timer("ThreadPool_Wathcer");
        watch.schedule(new TimerTask() {
            @Override
            public void run() {
                if(noSlotAvailable) {
                    boolean isSuccess = false;
                    List<ThreadPoolExecutor> poolMap1 = new ArrayList<>();
                    List<Integer> removeIndex = new ArrayList<>();
                    for(int i=0; i < poolMap.size(); i++) {
                        ThreadPoolExecutor entry = poolMap.get(i);
                        if(entry.getActiveCount() ==0 && entry.getTaskCount() > 0 && entry.getCompletedTaskCount() > 0 && entry.getTaskCount() == entry.getCompletedTaskCount()) {
                            totalTaskCount += entry.getTaskCount();
                            totalCompletedTaskCount += entry.getCompletedTaskCount();
                            removeIndex.add(i);
                            poolMap1.add((ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD_EXE_COUNT));
                            isSuccess=true;
                        }
                    }

                    Collections.sort(removeIndex, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer o1, Integer o2) {
                            return o2.compareTo(o1);
                        }
                    });

                    for(int i : removeIndex) {
                        System.out.println("Removing Pool" + i );
                        ThreadPoolExecutor entry = poolMap.get(i);
                        entry.shutdown();
                        entry.purge();
                        poolMap.remove(i);
                    }

                    if(poolMap1.size() > 0)
                        poolMap.addAll(poolMap1);


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
                        Long totalRecords = TOTAL_RECORDS_FOR_PROCESS;
                        Long TotalSum = Arrays.stream(consumedTimeList).mapToLong(Long::longValue).sum();
                        int noOfRecords = consumedTimeList.length;

                        Integer[] consumedCountList = countQueue.toArray(new Integer[countQueue.size()]);
                        Integer TotalCountSum = Arrays.stream(consumedCountList).mapToInt(Integer::intValue).sum();
                        int noOfCountRecords = consumedCountList.length;
                        int avgCount = TotalCountSum/noOfCountRecords;

                        Long remainingRecords = totalRecords - (totalCompletedTaskCount+ completedCount + ALREADY_PROCESSED_RECORDS);
                        avgTime = TotalSum / noOfRecords;
                        Long totalTimeRequired = (remainingRecords / avgCount);
                        System.out.println("TotalCountSum" + TotalCountSum);
                        System.out.println("noOfCountRecords" + noOfCountRecords);
                        System.out.println("remainingRecords" + remainingRecords);
                        System.out.println("avgCount" + avgCount);
                        System.out.println("Total Time Required" + totalTimeRequired);

                        totalHours = (int) (totalTimeRequired / 60);
                        totalDays = (int) totalHours / 24;
                        totalHours = (int) (totalHours % 24);
                        remainingMinutes = (int) (totalTimeRequired % 60);
                    }

                    System.out.println("Pool Name : " + NAME + " Avg Time : " + TimeUnit.SECONDS.convert(avgTime, TimeUnit.NANOSECONDS) + "S  Estimate Time of Completion : " + totalDays + "D " + totalHours + "H " + remainingMinutes + "M" +"  Total Records for Process : " + TOTAL_RECORDS_FOR_PROCESS + "  Total Task : " + (totalTaskCount +totalCount)  + ", Active Task : " + activeCount + ", Completed Task : " + (totalCompletedTaskCount+ completedCount + ALREADY_PROCESSED_RECORDS));
                }
            }
        }, 0, DELAY_SECONDS);
    }

    public synchronized void ExecuteTask(Runnable task) throws InterruptedException {
        boolean taskAdded = false;

        do {
            if(!noSlotAvailable) {

                for(ThreadPoolExecutor entry : poolMap) {
                    if(entry.getTaskCount() < maxThreadCount) {
                        entry.execute(task);
                        taskAdded=true;
                        break;
                    }
                }

                boolean slotAvailable = false;
                for(ThreadPoolExecutor entry : poolMap) {
                    if(entry.getTaskCount() < maxThreadCount)
                        slotAvailable = true;
                }

                if(!slotAvailable) {
                    noSlotAvailable = true;
                }
            }  else {
                TimeUnit.SECONDS.sleep(10);
                System.out.println("Trying to Repush the Data to Thread");
            }
        } while (noSlotAvailable || !taskAdded);

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
