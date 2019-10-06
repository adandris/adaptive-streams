package com.teodorstoev.adaptivestreams;

/**
 * A resource monitor provides the key resource metrics used for scaling within an adaptive subscriber.
 */
public interface ResourceMonitor {

    boolean isEnoughCpuAvailable();

    boolean isEnoughMemoryAvailable();
}
