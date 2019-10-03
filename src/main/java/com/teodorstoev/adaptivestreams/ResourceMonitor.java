package com.teodorstoev.adaptivestreams;

public interface ResourceMonitor {

    boolean isEnoughCpuAvailable();

    boolean isEnoughMemoryAvailable();
}
