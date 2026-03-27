package com.itgeo.bean;

import com.itgeo.auth.AuthenticatedUserContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentExecuteContext {
    private Long runId;
    private Long chatSessionId;
    private Long assistantMessageId;
    private String lockKey;
    private AuthenticatedUserContext authenticatedUser;
    private ChatEntity chatEntity;
}
