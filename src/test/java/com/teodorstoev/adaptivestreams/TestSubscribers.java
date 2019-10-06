package com.teodorstoev.adaptivestreams;

import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class TestSubscribers {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSubscribers.class);

    private static final String RANDOM_STRING = newRandomString();

    static <T> FlowableSubscriber<T> newQuickTaskSubscriber(final AtomicInteger taskCounter) {
        return new TestSubscriber<T>() {

            @Override
            public void onNext(T t) {
                UUID uuid = UUID.randomUUID();
                LOGGER.debug("{}", uuid.toString());

                taskCounter.incrementAndGet();

                request(1);
            }
        };
    }

    static <T> FlowableSubscriber<T> newThreadBlockingTaskSubscriber(final AtomicInteger taskCounter) {
        return new TestSubscriber<T>() {

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
        };
    }

    static <T> FlowableSubscriber<T> newMemoryConsumingTaskSubscriber(final AtomicInteger taskCounter) {
        return new TestSubscriber<T>() {

            @Override
            public void onNext(T t) {
                String randomString = new String(Arrays.copyOf(RANDOM_STRING.getBytes(), RANDOM_STRING.length()));

                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    LOGGER.error("Task interrupted", e);
                }

                LOGGER.debug(randomString);

                taskCounter.incrementAndGet();

                request(1);
            }
        };
    }

    private static String newRandomString() {
        byte[] array = new byte[20 * 1024 * 1024]; // 20 MB
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    private static abstract class TestSubscriber<T> extends DefaultSubscriber<T> {

        @Override
        public void onError(Throwable throwable) {
            LOGGER.error("Error in subscriber", throwable);
        }

        @Override
        public void onComplete() {
            LOGGER.debug("Subscriber completed");
        }
    }
}
