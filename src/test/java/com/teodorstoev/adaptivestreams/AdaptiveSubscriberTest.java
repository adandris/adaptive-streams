package com.teodorstoev.adaptivestreams;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class AdaptiveSubscriberTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveSubscriberTest.class);

    private static final int TEST_DURATION = 10;

    private AtomicInteger taskCounter = new AtomicInteger();

    private boolean stopTest = false;

    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        taskCounter.set(0);

        countDownLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() throws InterruptedException {
        stopTest = true;
        countDownLatch.await();
    }

    @Test
    public void quickTask_defaultPrefetchCount() throws InterruptedException {
        createTestStreamAndSubscribe(new AdaptiveSubscriber<>(this::newQuickTaskSubscriber));

        TimeUnit.SECONDS.sleep(TEST_DURATION);

        assertMinThroughput(1500);
    }

    @Test
    public void quickTask_largePrefetchCount() throws InterruptedException {
        createTestStreamAndSubscribe(new AdaptiveSubscriber<>(this::newQuickTaskSubscriber, 100));

        TimeUnit.SECONDS.sleep(TEST_DURATION);

        assertMinThroughput(10000);
    }

    @Test
    public void threadBlockingTask_defaultPrefetchCount() throws InterruptedException {
        createTestStreamAndSubscribe(new AdaptiveSubscriber<>(this::newThreadBlockingTaskSubscriber));

        TimeUnit.SECONDS.sleep(TEST_DURATION);

        assertMinThroughput(600);
    }

    @Test
    public void threadBlockingTask_largePrefetchCount() throws InterruptedException {
        createTestStreamAndSubscribe(new AdaptiveSubscriber<>(this::newThreadBlockingTaskSubscriber, 100));

        TimeUnit.SECONDS.sleep(TEST_DURATION);

        assertMinThroughput(900);
    }

    private void createTestStreamAndSubscribe(AdaptiveSubscriber<Object> adaptiveSubscriber) {
        Flowable.generate(this::getTestGenerator)
                .subscribeOn(Schedulers.newThread())
                .subscribe(adaptiveSubscriber);
    }

    private void getTestGenerator(Emitter<Object> emitter) {
        if (stopTest) {
            emitter.onComplete();
            countDownLatch.countDown();
        } else {
            emitter.onNext("foo");
        }
    }

    private <T> FlowableSubscriber<T> newQuickTaskSubscriber() {
        return new DefaultSubscriber<T>() {

            @Override
            public void onNext(T t) {
                UUID uuid = UUID.randomUUID();
                LOGGER.debug("{}", uuid.toString());

                taskCounter.incrementAndGet();

                request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("Error in subscriber", throwable);
            }

            @Override
            public void onComplete() {
                LOGGER.debug("Subscriber completed");
            }
        };
    }

    private <T> FlowableSubscriber<T> newThreadBlockingTaskSubscriber() {
        return new DefaultSubscriber<T>() {

            @Override
            public void onNext(T t) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    LOGGER.error("Task interrupted", e);
                }

                taskCounter.incrementAndGet();

                request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("Error in subscriber", throwable);
            }

            @Override
            public void onComplete() {
                LOGGER.debug("Subscriber completed");
            }
        };
    }

    private void assertMinThroughput(int minThroughput) {
        int processedTasksCount = taskCounter.get();
        LOGGER.info("Tasks processed: {}", processedTasksCount);

        double throughput = processedTasksCount * 1.0 / TEST_DURATION;
        LOGGER.info("Task throughput: {} tasks/s", throughput);

        assertTrue(throughput > minThroughput);
    }
}