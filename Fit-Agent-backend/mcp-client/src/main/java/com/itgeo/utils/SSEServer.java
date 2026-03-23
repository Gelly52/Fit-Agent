package com.itgeo.utils;

import com.itgeo.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author gzx
 * @description: SSE服务器
 * @date 2024-05-20 10:00:00
 */
@Slf4j
public class SSEServer {

    // 存放所有用户
    private static final Map<String, SseEmitter> sseClients = new ConcurrentHashMap<>();

    /**
     * 连接SSE
     *
     * @param userId 用户ID
     * @return SSE发射器
     */
    public static SseEmitter connect(String userId) {
        // 设置超时时间，0表示不超时，永不过期（默认超时时间为30秒，未完成任务抛出异常）
        SseEmitter sseEmitter = new SseEmitter(0L);

        // 注册回调函数
        sseEmitter.onTimeout(timeoutCallback(userId));
        sseEmitter.onCompletion(completionCallback(userId));
        sseEmitter.onError(errorCallback(userId));

        log.info("SSE连接成功，连接的用户ID为：{}", userId);

        sseClients.put(userId, sseEmitter);

        return sseEmitter;
    }

    /**
     * 发送单个SSE消息
     *
     * @param userId  用户ID
     * @param message 消息内容
     * @param msgType 消息类型
     */
    public static void sendMsg(String userId, String message, SSEMsgType msgType) {
        if (CollectionUtils.isEmpty(sseClients)) {
            return;
        }

        if (sseClients.containsKey(userId)) {
            SseEmitter sseEmitter = sseClients.get(userId);
            sendEmitterMessage(sseEmitter, userId, message, msgType);
        }
    }

    /**
     * 发送SSE消息给所有用户
     *
     * @param message 消息内容
     */
    public static void sendMsgToAllUsers(String message) {
        if (CollectionUtils.isEmpty(sseClients)) {
            return;
        }
        sseClients.forEach((userId, sseEmitter) -> {
            sendEmitterMessage(sseEmitter, userId, message, SSEMsgType.MESSAGE);
        });
    }

    /*
     * 通用发送SSE消息方法
     * @param sseEmitter SSE发射器
     * @param userId     用户ID
     * @param message    消息内容
     * @param msgType    消息类型
     */
    private static void sendEmitterMessage(SseEmitter sseEmitter,
                                           String userId,
                                           String message,
                                           SSEMsgType msgType) {
        try {
            SseEmitter.SseEventBuilder msgEvent = SseEmitter.event()
                    .id(userId)
                    .data(message)
                    .name(msgType.type);
            sseEmitter.send(msgEvent);
        } catch (IOException e) {
//            throw new RuntimeException(e);
            log.info("SSE异常...{}", e.getMessage());
            remove(userId);
        }

    }

    /**
     * 超时回调函数
     *
     * @param userId 用户ID
     * @return 超时回调函数
     */
    public static Runnable timeoutCallback(String userId) {
        return () -> {
            log.info("{} 连接超时...", userId);
            // 移除用户连接
            remove(userId);
        };
    }

    /**
     * 完成回调函数
     *
     * @param userId 用户ID
     * @return 完成回调函数
     */
    public static Runnable completionCallback(String userId) {
        return () -> {
            log.info("{} 连接完成...", userId);
            // 移除用户连接
            remove(userId);
        };
    }

    /**
     * 错误回调函数
     *
     * @param userId 用户ID
     * @return 错误回调函数
     */
    public static Consumer<Throwable> errorCallback(String userId) {
        return throwable -> {
            log.error("{} 连接错误：{}", userId, throwable.getMessage());
            // 移除用户连接
            remove(userId);
        };
    }

    /**
     * 移除用户连接
     *
     * @param userId 用户ID
     */
    public static void remove(String userId) {
        // 移除用户
        log.info("SSE连接被移除，移除的用户ID为：{}", userId);
        sseClients.remove(userId);
    }

}
