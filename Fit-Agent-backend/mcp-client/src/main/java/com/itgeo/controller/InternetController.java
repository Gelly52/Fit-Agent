package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.service.ChatService;
import com.itgeo.service.SearXngService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 联网搜索问答控制器。
 * <p>
 * 提供搜索结果调试查看入口，以及手动触发的联网增强问答入口。
 */
@RestController
@RequestMapping("internet")
public class InternetController {

    @Resource
    private SearXngService searXngService;

    @Resource
    private ChatService chatService;

    /**
     * 调试查看原始联网搜索结果。
     */
    @GetMapping("/test")
    public Object test(@RequestParam("query") String query) {
        return LeeResult.ok(searXngService.search(query));
    }

    /**
     * 发起联网搜索增强问答。
     * <p>
     * 步骤：
     * 1. 提取当前登录用户；
     * 2. 绑定当前用户名；
     * 3. 委派给聊天服务执行联网增强回答。
     */
    @PostMapping("/search")
    public LeeResult search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();
        chatEntity.setCurrentUserName(authenticatedUser.getUserKey());
        ChatResponseEntity result = chatService.doInternetSearch(chatEntity, authenticatedUser);
        return LeeResult.ok(result);
    }
}
