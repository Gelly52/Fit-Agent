package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单条聊天消息记录项。
 *
 * 说明：
 * 1. sessionId / sessionCode / sceneType 描述所属会话；
 * 2. role / messageType / sourceType 描述消息类型；
 * 3. sourcesJson 保存 assistant 回复来源的原始 JSON 字符串。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRecordItem {

    private Long sessionId;
    private String sessionCode;
    private String sceneType;
    private Long messageId;
    private Integer seqNo;
    private String role;
    private String messageType;
    private String sourceType;
    private String content;
    private String botMsgId;
    private String sourcesJson;
    private LocalDateTime createdAt;

}
