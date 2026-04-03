package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_rag_benchmark_run")
public class RagBenchmarkRun {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String datasetName;
    private Integer questionCount;
    private String status;
    private String configSnapshotJson;
    private String resultJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
