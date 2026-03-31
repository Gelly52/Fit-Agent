package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_rag_benchmark_run")
public class RagBenchmarkRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("dataset_name")
    private String datasetName;

    @TableField("question_count")
    private Integer questionCount;

    private String status;

    @TableField("config_snapshot_json")
    private String configSnapshotJson;

    @TableField("result_json")
    private String resultJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
