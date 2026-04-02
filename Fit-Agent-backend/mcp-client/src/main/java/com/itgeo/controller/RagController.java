package com.itgeo.controller;

import com.itgeo.auth.AuthenticatedUserContext;
import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.ChatEntity;
import com.itgeo.bean.ChatResponseEntity;
import com.itgeo.bean.rag.RagBenchmarkEvaluateRequest;
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
 * <p>
 * 提供手动上传文档、手动检索片段、手动触发带 RAG 上下文的问答等入口。
 * `/rag/search` 仅作为手动入口使用：控制器会先检索文档片段，再显式调用聊天服务完成回答。
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
     * 上传当前用户自己的 RAG 文档。
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
     * <p>
     * 步骤：
     * 1. 先按当前问题手动检索知识库片段；
     * 2. 再把检索结果显式传给聊天服务生成回答；
     * 3. 该入口不会自动改写其他聊天接口的执行路径。
     */
    @PostMapping("/search")
    public LeeResult search(@RequestBody ChatEntity chatEntity, HttpServletResponse response) {
        // 1. 获取当前登录用户，用于限定检索范围
        AuthenticatedUserContext authenticatedUser = UserContextHolder.getRequired();

        // 2. 先手动检索当前用户知识库中的相关片段
        List<Document> documents = documentService.doSearch(
                chatEntity.getMessage(),
                authenticatedUser.getUserId(),
                4
        );

        // 3. 再把手动检索出的上下文显式交给聊天服务生成回答
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
        // 1. 获取当前登录用户
        Long userId = UserContextHolder.getRequired().getUserId();

        // 2. 返回当前用户已上传文档列表
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

    /**
     * 执行 RAG benchmark 评测。
     */
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
