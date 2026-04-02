package com.itgeo.service.embedding;

import cn.hutool.json.JSONUtil;
import com.itgeo.bean.embedding.BgeM3EmbedRequest;
import com.itgeo.bean.embedding.BgeM3EmbedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class BgeM3HttpEmbeddingModel implements EmbeddingModel {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final String serviceUrl;
    private final String modelName;
    private final int dimension;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingResponse(List.of());
        }

        BgeM3EmbedRequest payload = new BgeM3EmbedRequest(texts, 8, 1024);
        String json = JSONUtil.toJsonStr(payload);

        Request httpRequest = new Request.Builder()
                .url(serviceUrl + "/embed")
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("bge-m3 embedding 请求失败: " + response.code());
            }
            if (response.body() == null) {
                throw new IOException("bge-m3 embedding 响应体为空");
            }

            String body = response.body().string();
            BgeM3EmbedResponse embedResponse = JSONUtil.toBean(body, BgeM3EmbedResponse.class);

            List<Embedding> embeddings = new ArrayList<>();
            if (embedResponse.getData() != null) {
                for (BgeM3EmbedResponse.Item item : embedResponse.getData()) {
                    List<Float> vector = item.getEmbedding();
                    float[] output = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        output[i] = vector.get(i);
                    }
                    embeddings.add(new Embedding(output, item.getIndex()));
                }
            }

            return new EmbeddingResponse(embeddings);
        } catch (IOException e) {
            log.error("调用 bge-m3 embedding 服务失败, serviceUrl={}", serviceUrl, e);
            throw new RuntimeException("调用 bge-m3 embedding 服务失败", e);
        }
    }

    @Override
    public float[] embed(Document document) {
        if (document == null || document.getText() == null) {
            return new float[0];
        }
        return this.embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        EmbeddingResponse response = this.call(new EmbeddingRequest(List.of(text), null));
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return new float[0];
        }
        return response.getResults().get(0).getOutput();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        EmbeddingResponse response = this.call(new EmbeddingRequest(texts, null));
        List<float[]> outputs = new ArrayList<>();
        if (response == null || response.getResults() == null) {
            return outputs;
        }
        for (Embedding embedding : response.getResults()) {
            outputs.add(embedding.getOutput());
        }
        return outputs;
    }

    @Override
    public int dimensions() {
        return dimension;
    }
}