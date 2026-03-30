package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天历史查询响应。
 *
 * 说明：
 * 1. userId 为当前登录用户主键；
 * 2. totalSessions 表示本次返回的会话条数；
 * 3. sessions 按最近更新时间倒序返回。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRecordsResponse {
    private Long userId;
    private Integer totalSessions;
    private List<ChatSessionRecordItem> sessions;
}
