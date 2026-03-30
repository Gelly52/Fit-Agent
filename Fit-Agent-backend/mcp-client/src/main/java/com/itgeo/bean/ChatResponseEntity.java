package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 聊天链路结束响应。
 *
 * 说明：
 * 1. message 为最终拼接后的完整回复；
 * 2. chatSessionId / sessionCode 用于前端恢复当前会话；
 * 3. sceneType / sourceType 用于前端判断续聊模式；
 * 4. sources 用于承载 RAG / 联网问答的来源信息。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponseEntity {
    private String message;
    private String botMsgId;
    private Long chatSessionId;
    private String sessionCode;
    private String sourceType;
    private Object sources;
    private String sceneType;
}
