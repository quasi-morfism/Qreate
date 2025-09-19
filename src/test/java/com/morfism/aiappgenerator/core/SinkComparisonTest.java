package com.morfism.aiappgenerator.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 比较不同Sink实现方式的测试
 * 验证 Flux.create() vs Sinks.many() 的差异
 */
public class SinkComparisonTest {
    
    public static void main(String[] args) throws Exception {
        System.out.println("🔬 Sink Implementation Comparison Test");
        System.out.println("=====================================\n");
        
        // 测试1: Flux.create() 方式 (冷流)
        testFluxCreateApproach();
        
        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 测试2: Sinks.many() 方式 (热流)
        testSinksManyApproach();
        
        Thread.sleep(1000);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 测试3: 多订阅者行为对比
        testMultipleSubscribers();
    }
    
    /**
     * 测试 Flux.create() 方式 (类似您提供的代码)
     */
    private static void testFluxCreateApproach() throws Exception {
        System.out.println("📝 Test 1: Flux.create() Approach (Cold Stream)");
        
        Flux<String> coldStream = Flux.<String>create(sink -> {
            System.out.println("🔄 Flux.create: Starting emission for new subscriber");
            
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 5; i++) {
                        String message = "Cold-" + i;
                        System.out.println("📤 Flux.create: Emitting " + message);
                        sink.next(message);
                        Thread.sleep(100);
                    }
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            }).start();
        }).share(); // 转换为热流
        
        AtomicInteger messageCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        coldStream.subscribe(
            message -> {
                int count = messageCount.incrementAndGet();
                System.out.println("📥 Flux.create: [" + count + "] Received: " + message);
            },
            error -> {
                System.err.println("❌ Flux.create: Error: " + error.getMessage());
                latch.countDown();
            },
            () -> {
                System.out.println("✅ Flux.create: Stream completed!");
                latch.countDown();
            }
        );
        
        latch.await(3, TimeUnit.SECONDS);
        System.out.println("📊 Flux.create: Total messages: " + messageCount.get());
    }
    
    /**
     * 测试 Sinks.many() 方式 (类似我最初的实现)
     */
    private static void testSinksManyApproach() throws Exception {
        System.out.println("📝 Test 2: Sinks.many() Approach (Hot Stream)");
        
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<String> hotStream = sink.asFlux();
        
        AtomicInteger messageCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        
        // 先订阅
        hotStream.subscribe(
            message -> {
                int count = messageCount.incrementAndGet();
                System.out.println("📥 Sinks.many: [" + count + "] Received: " + message);
            },
            error -> {
                System.err.println("❌ Sinks.many: Error: " + error.getMessage());
                latch.countDown();
            },
            () -> {
                System.out.println("✅ Sinks.many: Stream completed!");
                latch.countDown();
            }
        );
        
        // 然后发射数据
        new Thread(() -> {
            try {
                System.out.println("🔄 Sinks.many: Starting emission");
                for (int i = 1; i <= 5; i++) {
                    String message = "Hot-" + i;
                    System.out.println("📤 Sinks.many: Emitting " + message);
                    
                    // 使用 tryEmitNext (非阻塞)
                    Sinks.EmitResult result = sink.tryEmitNext(message);
                    if (result.isFailure()) {
                        System.out.println("⚠️ Sinks.many: Failed to emit " + message + ": " + result);
                    }
                    Thread.sleep(100);
                }
                sink.tryEmitComplete();
            } catch (Exception e) {
                sink.tryEmitError(e);
            }
        }).start();
        
        latch.await(3, TimeUnit.SECONDS);
        System.out.println("📊 Sinks.many: Total messages: " + messageCount.get());
    }
    
    /**
     * 测试多订阅者行为
     */
    private static void testMultipleSubscribers() throws Exception {
        System.out.println("📝 Test 3: Multiple Subscribers Behavior");
        
        // Flux.create + share 方式
        System.out.println("\n🔄 Flux.create().share() with multiple subscribers:");
        Flux<String> sharedColdStream = Flux.<String>create(sink -> {
            System.out.println("🎯 Flux.create: New execution started");
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 3; i++) {
                        String message = "Shared-" + i;
                        System.out.println("📤 Flux.create: Emitting " + message);
                        sink.next(message);
                        Thread.sleep(200);
                    }
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            }).start();
        }).share();
        
        CountDownLatch coldLatch = new CountDownLatch(2);
        
        // 订阅者1
        sharedColdStream.subscribe(
            message -> System.out.println("📥 Subscriber-1: " + message),
            error -> coldLatch.countDown(),
            () -> {
                System.out.println("✅ Subscriber-1: Completed");
                coldLatch.countDown();
            }
        );
        
        // 订阅者2 (稍晚订阅)
        Thread.sleep(100);
        sharedColdStream.subscribe(
            message -> System.out.println("📥 Subscriber-2: " + message),
            error -> coldLatch.countDown(),
            () -> {
                System.out.println("✅ Subscriber-2: Completed");
                coldLatch.countDown();
            }
        );
        
        coldLatch.await(5, TimeUnit.SECONDS);
        
        Thread.sleep(500);
        
        // Sinks.many 方式
        System.out.println("\n🔄 Sinks.many() with multiple subscribers:");
        Sinks.Many<String> multiSink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<String> hotMultiStream = multiSink.asFlux();
        
        CountDownLatch hotLatch = new CountDownLatch(2);
        
        // 订阅者1
        hotMultiStream.subscribe(
            message -> System.out.println("📥 Hot-Subscriber-1: " + message),
            error -> hotLatch.countDown(),
            () -> {
                System.out.println("✅ Hot-Subscriber-1: Completed");
                hotLatch.countDown();
            }
        );
        
        // 订阅者2
        hotMultiStream.subscribe(
            message -> System.out.println("📥 Hot-Subscriber-2: " + message),
            error -> hotLatch.countDown(),
            () -> {
                System.out.println("✅ Hot-Subscriber-2: Completed");
                hotLatch.countDown();
            }
        );
        
        // 发射数据
        new Thread(() -> {
            try {
                Thread.sleep(100);
                for (int i = 1; i <= 3; i++) {
                    String message = "Multi-" + i;
                    System.out.println("📤 Sinks.many: Emitting " + message);
                    multiSink.tryEmitNext(message);
                    Thread.sleep(200);
                }
                multiSink.tryEmitComplete();
            } catch (Exception e) {
                multiSink.tryEmitError(e);
            }
        }).start();
        
        hotLatch.await(5, TimeUnit.SECONDS);
        
        System.out.println("\n🎉 All tests completed!");
    }
}