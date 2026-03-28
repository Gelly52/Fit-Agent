package com.itgeo.utils;

import com.itgeo.enums.SSEMsgType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器。
 *
 * 说明：
 * 1. key 实际使用 sseClientId，而不是数据库 userId；
 * 2. 同一 sseClientId 重连时，会先替换成新 emitter，再关闭旧 emitter；
 * 3. 旧 emitter 的 completion / timeout / error 回调只会移除自己，避免误删新连接。
 */
@Slf4j
public class SSEServer {

    private static final Map<String, SseEmitter> SSE_CLIENTS = new ConcurrentHashMap<>();

    private SSEServer() {
    }

    /**
     * 建立或替换一个 SSE 连接。
     *
     * @param clientId SSE 客户端标识
     * @return SSE 发射器
     */
    public static SseEmitter connect(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("sseClientId不能为空");
        }

        SseEmitter newEmitter = new SseEmitter(0L);
        SseEmitter oldEmitter = SSE_CLIENTS.put(clientId, newEmitter);

        newEmitter.onTimeout(() -> {
            log.info("SSE连接超时, clientId={}", clientId);
            removeIfSame(clientId, newEmitter);
        });
        newEmitter.onCompletion(() -> {
            log.info("SSE连接完成, clientId={}", clientId);
            removeIfSame(clientId, newEmitter);
        });
        newEmitter.onError(error -> {
            log.warn("SSE连接异常, clientId={}, message={}", clientId, error == null ? null : error.getMessage());
            removeIfSame(clientId, newEmitter);
        });

        if (oldEmitter != null && oldEmitter != newEmitter) {
            closeQuietly(oldEmitter);
        }

        log.info("SSE连接建立成功, clientId={}", clientId);
        return newEmitter;
    }

    /**
     * 发送单个 SSE 消息。
     */
    public static void sendMsg(String clientId, String message, SSEMsgType msgType) {
        if (clientId == null || clientId.isBlank() || msgType == null || SSE_CLIENTS.isEmpty()) {
            return;
        }

        SseEmitter emitter = SSE_CLIENTS.get(clientId);
        if (emitter != null) {
            sendEmitterMessage(emitter, clientId, message, msgType);
        }
    }

    /**
     * 广播 SSE 消息给所有在线连接。
     */
    public static void sendMsgToAllUsers(String message) {
        if (SSE_CLIENTS.isEmpty()) {
            return;
        }
        SSE_CLIENTS.forEach((clientId, emitter) -> sendEmitterMessage(emitter, clientId, message, SSEMsgType.MESSAGE));
    }

    /**
     * 实际发送 SSE 消息。
     */
    private static void sendEmitterMessage(SseEmitter emitter,
                                           String clientId,
                                           String message,
                                           SSEMsgType msgType) {
        if (emitter == null) {
            return;
        }

        try {
            SseEmitter.SseEventBuilder msgEvent = SseEmitter.event()
                    .id(clientId)
                    .data(message)
                    .name(msgType.type);
            emitter.send(msgEvent);
        } catch (IOException e) {
            log.info("SSE发送失败, clientId={}, message={}", clientId, e.getMessage());
            removeIfSame(clientId, emitter);
            closeQuietly(emitter);
        }
    }

    /**
     * 手动移除一个 SSE 连接。
     */
    public static void remove(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return;
        }
        log.info("SSE连接被移除, clientId={}", clientId);
        SSE_CLIENTS.remove(clientId);
    }

    /**
     * 判断指定 SSE 客户端是否在线。
     */
    public static boolean isConnected(String clientId) {
        return clientId != null && !clientId.isBlank() && SSE_CLIENTS.containsKey(clientId);
    }

    /**
     * 仅当当前 map 中仍然是该 emitter 时才移除，避免重连时误删新连接。
     */
    private static void removeIfSame(String clientId, SseEmitter emitter) {
        SSE_CLIENTS.computeIfPresent(clientId, (key, current) -> current == emitter ? null : current);
    }

    /**
     * 安静关闭旧连接。
     */
    private static void closeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
