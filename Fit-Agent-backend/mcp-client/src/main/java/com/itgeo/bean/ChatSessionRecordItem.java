package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个会话的历史记录项。
 *
 * 说明：
 * 1. sessionId / sessionCode / sceneType 描述会话身份；
 * 2. title / lastBotMsgId 用于前端列表展示与高亮；
 * 3. messages 保存当前会话下按 seqNo 正序排列的消息列表。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionRecordItem {
    private Long sessionId;
    private String sessionCode;
    private String sceneType;
    private String title;
    private String lastBotMsgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatRecordItem> messages;
}
