package com.teodorstoev.adaptivestreams;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * The default implementation of a resource monitor, based on the metrics provided by
 * {@link com.sun.management.OperatingSystemMXBean} and {@link java.lang.management.MemoryMXBean}.
 */
public class DefaultResourceMonitor implements ResourceMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultResourceMonitor.class);

    private double usageThreshold;

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    DefaultResourceMonitor(double usageThreshold) {
        this.usageThreshold = usageThreshold;

        osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public boolean isEnoughCpuAvailable() {
        return getCpuLoad() < usageThreshold;
    }

    @Override
    public boolean isEnoughMemoryAvailable() {
        if (getUsedHeapSize() < usageThreshold * getTotalHeapSize()) {
            return true;
        } else {
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
}
