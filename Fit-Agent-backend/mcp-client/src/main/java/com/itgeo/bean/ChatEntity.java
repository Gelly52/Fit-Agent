package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 聊天与 Agent 请求体。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatEntity {
    /** 兼容旧字段，会被当前登录用户覆盖。 */
    private String currentUserName;
    /** 用户输入消息。 */
    private String message;
    /** 机器人消息标识。 */
    private String botMsgId;
    /** 前端会话编码。 */
    private String sessionCode;
    /** 客户端请求标识，当前仅透传。 */
    private String clientRequestId;
    /** 是否开启知识库增强。 */
    private Boolean ragEnabled;
    /** 是否开启联网补充。 */
    private Boolean internetEnabled;
}
