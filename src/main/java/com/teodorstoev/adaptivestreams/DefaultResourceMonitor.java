package com.teodorstoev.adaptivestreams;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class DefaultResourceMonitor implements ResourceMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResourceMonitor.class);

    /**
     * The minimum amount of memory necessary to start new publishers: 100 MB.
     */
    private static final int MINIMUM_MEMORY = 100 * 1024 * 1024;

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    DefaultResourceMonitor() {
        osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public boolean isEnoughCpuAvailable() {
        return getCpuLoad() < 0.7;
    }

    @Override
    public boolean isEnoughMemoryAvailable() {
        long freeHeapSize = getTotalHeapSize() - getUsedHeapSize();
        if (freeHeapSize > MINIMUM_MEMORY) {
            return true;
        } else {
            double heapSizeGrowthCapacity = getMaxHeapSize() - getTotalHeapSize();
            if (heapSizeGrowthCapacity > MINIMUM_MEMORY && getFreeMemory() > MINIMUM_MEMORY) {
                return true;
            }

            System.gc();
            LOGGER.info("Garbage collection triggered");
            return false;
        }
    }

    private double getCpuLoad() {
        double cpuLoad = osBean.getProcessCpuLoad();
        LOGGER.debug("CPU load: {}", cpuLoad);

        return cpuLoad;
    }

    private long getFreeMemory() {
        long freeMemory = osBean.getFreePhysicalMemorySize();
        LOGGER.debug("Free memory: {}", freeMemory);

        return freeMemory;
    }

    private long getTotalHeapSize() {
        MemoryUsage memoryUsage = memoryBean.getHeapMemoryUsage();
        long totalHeapSize = memoryUsage.getCommitted();
        LOGGER.debug("Total heap size: {}", totalHeapSize);

        return totalHeapSize;
    }

    private long getUsedHeapSize() {
        MemoryUsage memoryUsage = memoryBean.getHeapMemoryUsage();
        long usedHeapSize = memoryUsage.getUsed();
        LOGGER.debug("Total heap size: {}", usedHeapSize);

        return usedHeapSize;
    }

    private double getMaxHeapSize() {
        MemoryUsage memoryUsage = memoryBean.getHeapMemoryUsage();
        long maxHeapSize = memoryUsage.getMax();
        LOGGER.debug("Max heap size: {}", maxHeapSize);

        return maxHeapSize;
    }
}
