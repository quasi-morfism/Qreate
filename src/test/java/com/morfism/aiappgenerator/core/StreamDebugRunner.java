package com.morfism.aiappgenerator.core;

import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于调试Stream实现的Runner
 * 可以直接运行来测试流式传输的行为
 */
public class StreamDebugRunner {
    
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Starting Stream Debug Test...");
        
        // 创建模拟的服务
        AiCodeGeneratorFacade facade = createMockFacade();
        
        // 测试参数
        String userMessage = "Create a Vue component with file operations";
        Long appId = 1L;
        Long userId = 1L;
        
        System.out.println("📋 Test Parameters:");
        System.out.println("  Message: " + userMessage);
        System.out.println("  AppId: " + appId);
        System.out.println("  UserId: " + userId);
        System.out.println();
        
        // 执行流式传输
        System.out.println("🌊 Starting stream...");
        Flux<String> stream = facade.generateAndSaveCodeStream(
            userMessage, 
            CodeGenTypeEnum.VUE_PROJECT, 
            appId, 
            userId
        );
        
        // 订阅流并打印结果
        AtomicInteger messageCount = new AtomicInteger(0);
        CountDownLatch completeLatch = new CountDownLatch(1);
        
        stream.subscribe(
            message -> {
                int count = messageCount.incrementAndGet();
                System.out.println("📥 [" + count + "] Received: " + message);
                
                // 模拟前端处理延迟
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            error -> {
                System.err.println("❌ Stream error: " + error.getMessage());
                completeLatch.countDown();
            },
            () -> {
                System.out.println("✅ Stream completed!");
                completeLatch.countDown();
            }
        );
        
        // 等待流完成
        System.out.println("⏳ Waiting for stream to complete...");
        boolean completed = completeLatch.await(10, TimeUnit.SECONDS);
        
        if (completed) {
            System.out.println("🎉 Test completed successfully!");
            System.out.println("📊 Total messages received: " + messageCount.get());
        } else {
            System.out.println("⚠️ Test timed out!");
        }
        
        // 测试前端断开场景
        testFrontendDisconnection(facade, userMessage, appId, userId);
    }
    
    /**
     * 测试前端断开连接的场景
     */
    private static void testFrontendDisconnection(AiCodeGeneratorFacade facade, String userMessage, Long appId, Long userId) throws Exception {
        System.out.println("\n🔌 Testing frontend disconnection scenario...");
        
        Flux<String> stream = facade.generateAndSaveCodeStream(userMessage, CodeGenTypeEnum.VUE_PROJECT, appId, userId);
        
        // 模拟前端连接一会儿就断开
        stream.take(Duration.ofMillis(500))  // 只接收500ms的数据
              .subscribe(
                  message -> System.out.println("📱 Frontend received: " + message),
                  error -> System.err.println("📱 Frontend error: " + error.getMessage()),
                  () -> System.out.println("📱 Frontend disconnected!")
              );
        
        // 等待后台继续执行
        System.out.println("⏳ Waiting for backend to continue after frontend disconnect...");
        Thread.sleep(3000);
        
        System.out.println("✅ Frontend disconnection test completed!");
    }
    
    /**
     * 创建模拟的Facade用于测试
     */
    private static AiCodeGeneratorFacade createMockFacade() {
        return new AiCodeGeneratorFacade() {
            @Override
            public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
                System.out.println("🎯 Mock facade: generating stream for " + codeGenTypeEnum);
                
                return Flux.<String>create(sink -> {
                    // 模拟TokenStream的行为
                    new Thread(() -> {
                        try {
                            // 1. 发送部分响应
                            sink.next("Generating Vue component...");
                            Thread.sleep(100);
                            
                            sink.next("Setting up component structure...");
                            Thread.sleep(100);
                            
                            // 2. 模拟工具执行
                            sink.next("\n[TOOL_EXECUTED:writeFile:tool-001]");
                            Thread.sleep(100);
                            
                            // 3. 模拟文件写入成功
                            sink.next("\n[FILE_WRITE_SUCCESS:App.vue]");
                            Thread.sleep(100);
                            
                            sink.next("Adding component logic...");
                            Thread.sleep(100);
                            
                            // 4. 另一个文件写入
                            sink.next("\n[TOOL_EXECUTED:writeFile:tool-002]");
                            sink.next("\n[FILE_WRITE_SUCCESS:AppStyle.css]");
                            Thread.sleep(100);
                            
                            // 5. 完成
                            sink.next("Vue component generated successfully!");
                            sink.next("\n[GENERATION_COMPLETE]");
                            
                            // 模拟后台保存操作
                            System.out.println("💾 Mock: Saving to chat history...");
                            Thread.sleep(200);
                            System.out.println("💾 Mock: Chat history saved for appId: " + appId);
                            
                            sink.complete();
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            sink.error(e);
                        } catch (Exception e) {
                            sink.error(e);
                        }
                    }).start();
                }).share(); // 转换为热流
            }
        };
    }
}