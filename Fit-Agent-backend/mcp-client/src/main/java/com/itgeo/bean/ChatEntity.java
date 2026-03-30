package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ChatEntity {
    private String currentUserName;
    private String message;
    private String botMsgId;
    private String sessionCode;
    private String clientRequestId;
    private Boolean ragEnabled;
    private Boolean internetEnabled;
}
