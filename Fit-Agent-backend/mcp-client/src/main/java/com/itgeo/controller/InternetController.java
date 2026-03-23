package com.itgeo.controller;

import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.SearchResult;
import com.itgeo.service.ChatService;
import com.itgeo.service.DocumentService;
import com.itgeo.service.SearXngService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("internet")
public class InternetController {

    @Resource
    private SearXngService searXngService;

    @Resource
    private ChatService chatService;


    @GetMapping("/test")
    public Object test(@RequestParam("query") String query) {
        return LeeResult.ok(searXngService.search(query));
    }

    @PostMapping("/search")
    public void search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        chatService.doInternetSearch(chatEntity);
    }


}
