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

    /** 会话主键。 */
    private Long sessionId;
    /** 前端会话编码。 */
    private String sessionCode;
    /** 会话场景类型。 */
    private String sceneType;
    /** 消息主键。 */
    private Long messageId;
    /** 会话内顺序号。 */
    private Integer seqNo;
    /** 消息角色。 */
    private String role;
    /** 消息类型。 */
    private String messageType;
    /** 当前轮来源类型。 */
    private String sourceType;
    /** 消息正文。 */
    private String content;
    /** 机器人消息标识。 */
    private String botMsgId;
    /** 来源信息原始 JSON。 */
    private String sourcesJson;
    /** 消息创建时间。 */
    private LocalDateTime createdAt;

}
