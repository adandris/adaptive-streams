package com.teodorstoev.adaptivestreams;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class AdaptiveSubscriber<T> extends DefaultSubscriber<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveSubscriber.class);

    private ThreadPoolExecutor threadPoolExecutor;
    private final ResourceMonitor resourceMonitor;

    private final Supplier<Subscriber<T>> subscriberSupplier;
    private final BlockingQueue<T> queue;

    private Flowable<T> publisher;

    @SuppressWarnings("WeakerAccess")
    public AdaptiveSubscriber(Supplier<Subscriber<T>> subscriberSupplier) {
        this(subscriberSupplier, 10);
    }

    @SuppressWarnings("WeakerAccess")
    public AdaptiveSubscriber(Supplier<Subscriber<T>> subscriberSupplier, int prefetchCount) {
        this(subscriberSupplier, prefetchCount, new ThreadPoolExecutor(10, 2000, 5L, TimeUnit.SECONDS,
                new SynchronousQueue<>()), new DefaultResourceMonitor());
    }

    @SuppressWarnings("WeakerAccess")
    public AdaptiveSubscriber(Supplier<Subscriber<T>> subscriberSupplier, int prefetchCount,
                              ThreadPoolExecutor threadPoolExecutor, ResourceMonitor resourceMonitor) {
        this.subscriberSupplier = subscriberSupplier;
        this.queue = new ArrayBlockingQueue<>(prefetchCount);
        this.threadPoolExecutor = threadPoolExecutor;
        this.resourceMonitor = resourceMonitor;
    }

    @Override
    public void onStart() {
        publisher = Flowable.generate(this::nextOrComplete)
                            .subscribeOn(Schedulers.from(threadPoolExecutor));

        IntStream.range(0, threadPoolExecutor.getCorePoolSize())
                 .forEach(value -> publisher.subscribe(subscriberSupplier.get()));

        request(threadPoolExecutor.getCorePoolSize());
    }

    @Override
    public void onNext(T t) {
        while (!queue.offer(t)) {
            if (areFreeThreadsAvailable()) {
                if (resourceMonitor.isEnoughCpuAvailable() && resourceMonitor.isEnoughMemoryAvailable()) {
                    publisher.subscribe(subscriberSupplier.get());

                    LOGGER.debug("Additional publisher subscribed");
                } else {
                    wait("Resources at the limit. Waiting for resources to become available...");
                }
            } else {
                wait("Thread pool exhausted. Waiting for threads to become available...");
            }
        }

        request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Error in adaptive subscriber", throwable);
    }

    @Override
    public void onComplete() {
        LOGGER.debug("Adaptive subscriber completed. Max thread count reached: {}",
                threadPoolExecutor.getLargestPoolSize());

        threadPoolExecutor.shutdown();
        try {
            threadPoolExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for tasks to complete", e);

            Thread.currentThread().interrupt();
        }
    }

    private void nextOrComplete(Emitter<T> emitter) throws InterruptedException {
        T task = queue.poll(5, TimeUnit.SECONDS);
        if (task == null) {
            emitter.onComplete();
        }
        emitter.onNext(task);
    }

    private boolean areFreeThreadsAvailable() {
        return threadPoolExecutor.getPoolSize() < threadPoolExecutor.getMaximumPoolSize();
    }

    private void wait(String message) {
        LOGGER.info(message);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            onComplete();
        }
    }
}
