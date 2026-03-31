package com.itgeo.service;

import com.itgeo.bean.RagBenchmarkEvaluateRequest;
import com.itgeo.bean.RagBenchmarkEvaluateResponse;

public interface RagBenchmarkService {
    RagBenchmarkEvaluateResponse evaluate(Long userId, RagBenchmarkEvaluateRequest request);
}
