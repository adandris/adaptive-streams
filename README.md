# Adaptive Streams

An extension to RxJava 3 that enables automatic vertical scaling of reactive streams.

## Getting started

### Dependency

In a Maven project you only need to add the following dependency:
```
<dependency>
    <groupId>com.teodorstoev</groupId>
    <artifactId>adaptive-streams</artifactId>
    <version>1.0</version>
<dependency>
```

### Usage

The following example creates a reactive stream of random strings being consumed by an adaptive subscriber, which scales up to the available CPU and heap size:

```java
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

import java.util.Random;

public class HelloWorld {
    public static void main(String[] args) {
        Flowable.<String>generate(
                emitter -> {
                    byte[] randomBytes = new byte[10];
                    new Random().nextBytes(randomBytes);
                    emitter.onNext(new String(randomBytes));
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        new AdaptiveSubscriber<>(() -> new DefaultSubscriber<String>() {
                            @Override
                            public void onNext(String input) {
                                System.out.println(input);

                                request(1);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                            }

                            @Override
                            public void onComplete() {
                            }
                        })
                );
    }
}
```
