package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.bean.RagBenchmarkEvaluateRequest;
import com.itgeo.service.ChatService;
import com.itgeo.service.DocumentService;
import com.itgeo.service.RagBenchmarkService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 手动 RAG 文档上传与检索控制器。
 *
 * 说明：
 * 1. 当前仅开放手动上传、手动检索、手动带上下文问答；
 * 2. Phase 1 不会自动把 RAG 接入 /chat/doChat 或 /agent/execute。
 */
@Slf4j
@RestController
@RequestMapping("rag")
public class RagController {

    @Resource
    private DocumentService documentService;

    @Resource
    private ChatService chatService;

    @Resource
    private RagBenchmarkService ragBenchmarkService;

    /**
     * 上传用户自己的 RAG 文档。
     */
    @PostMapping("/uploadRagDoc")
    public LeeResult uploadRagDoc(@RequestParam("file") MultipartFile file) {
        Long userId = UserContextHolder.getRequired().getUserId();
        List<Document> documentList = documentService.loadText(
                file.getResource(),
                file.getOriginalFilename(),
                userId
        );
        return LeeResult.ok(documentList);
    }

    /**
     * 手动检索当前用户的 RAG 文档片段。
     */
    @GetMapping("/doSearch")
    public LeeResult doSearch(@RequestParam String question) {
        Long userId = UserContextHolder.getRequired().getUserId();
        return LeeResult.ok(documentService.doSearch(question, userId, 4));
    }

    /**
     * 手动 RAG 问答入口。
     */
    @PostMapping("/search")
    public LeeResult search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();
        List<Document> documents = documentService.doSearch(
                chatEntity.getMessage(),
                authenticatedUser.getUserId(),
                4
        );
        response.setCharacterEncoding("UTF-8");
        chatEntity.setCurrentUserName(authenticatedUser.getUserKey());
        ChatResponseEntity result = chatService.doChatRagSearch(
                chatEntity,
                documents,
                authenticatedUser
        );
        return LeeResult.ok(result);
    }

    /**
     * 查询当前用户已上传的 RAG 文档列表。
     */
    @GetMapping("/docs")
    public LeeResult getUploadedDocs() {
        // 1.获取当前用户：
        Long userId = UserContextHolder.getRequired().getUserId();

        // 2.调用 service：
        return LeeResult.ok(documentService.listUserDocuments(userId));
    }

    /**
     * 兼容旧调用方式的 RAG 配置读取入口。
     */
    @PostMapping("/config")
    public LeeResult ragConfig() {
        return LeeResult.ok(documentService.getRagConfig());
    }

    /**
     * 查询当前手动 RAG 配置。
     */
    @GetMapping("/config")
    public LeeResult getRagConfig() {
        return LeeResult.ok(documentService.getRagConfig());
    }

    @PostMapping("/benchmark/evaluate")
    public LeeResult benchmarkEvaluate(@RequestBody RagBenchmarkEvaluateRequest request) {
        try {
            Long userId = UserContextHolder.getRequired().getUserId();
            return LeeResult.ok(ragBenchmarkService.evaluate(userId, request));
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("RAG benchmark 评测失败", e);
            return LeeResult.errorException("RAG benchmark 评测失败");
        }
    }
}
