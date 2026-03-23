package com.itgeo.service.impl;

import com.itgeo.service.DocumentService;
import com.itgeo.utils.CustomTextSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final RedisVectorStore redisVectorStore;

    @Override
    public List<Document> loadText(Resource resource, String fileName) {
        // 加载读取文本文件
        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("fileName", fileName);
        List<Document> documentList = reader.get();
        System.out.println("documentList = " + documentList);
//        System.out.println("documentList" + documentList);

//        对文档进行切分，TokenTextSplitter默认每个文档切分800个token，每个token最多350个字符，每个文档最少5个token，最多10000个token，保留分隔符
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
        List<Document> list = tokenTextSplitter.apply(documentList);

//        CustomTextSplitter splitter = new CustomTextSplitter();
//        List<Document> list = splitter.apply(documentList);
        System.out.println("list = " + list);

        // 保存到向量数据库
        redisVectorStore.add(list);

        return documentList;
    }

    @Override
    public List<Document> doSearch(String question) {
        // 从向量数据库中搜索
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(4)  // 返回10个结果
                .build();
        List<Document> results = redisVectorStore.similaritySearch(request);
        System.out.println("检索结果数量: " + results.size());
        for (Document doc : results) {
            System.out.println("--- 文档内容 ---");
            System.out.println(doc.getText());
            System.out.println("---------------");
        }
        return results;
    }
}
