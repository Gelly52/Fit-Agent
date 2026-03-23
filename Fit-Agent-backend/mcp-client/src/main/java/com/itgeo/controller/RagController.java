package com.itgeo.controller;

import com.itgeo.bean.ChatEntity;
import com.itgeo.service.ChatService;
import com.itgeo.service.DocumentService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("rag")
public class RagController {

    @Resource
    private DocumentService documentService;
    @Resource
    private ChatService chatService;

    @PostMapping("/uploadRagDoc")
    public LeeResult uploadRagDoc(@RequestParam("file") MultipartFile file) {
        List<Document> documentList = documentService.loadText(file.getResource(), file.getOriginalFilename());
        return LeeResult.ok(documentList);
    }

    @GetMapping("/doSearch")
    public LeeResult doSearch(@RequestParam String question) {
        return LeeResult.ok(documentService.doSearch(question));
    }

    @PostMapping("/search")
    public void search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        List<Document> list = documentService.doSearch(chatEntity.getMessage());
        response.setCharacterEncoding("UTF-8");
        chatService.doChatRagSearch(chatEntity, list);
    }

}
