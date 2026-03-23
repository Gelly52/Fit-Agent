package com.itgeo.enums;

/**
 * @author gzx
 * @description: SSE消息类型
 * @date 2024-05-20 10:00:00
 */
public enum SSEMsgType {
    /**
     * 普通消息
     */
    MESSAGE("message", "单词发送的普通类型消息"),
    /**
     * 添加消息
     */
    ADD("add", "消息追加，适用于流式stream推送"),
    /**
     * 完成消息
     */
    FINISH("finish", "消息完成"),
    /**
     * 自定义事件
     */
    CUSTOM_EVENT("custom_event", "自定义事件"),
    DONE("done", "消息完成"); //ChatGLM

    public final String type;
    public final String value;

    SSEMsgType(String type, String value) {
        this.type = type;
        this.value = value;
    }

}
