package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_rag_config")
public class RagConfigEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String scopeType;
    private Long scopeId;
    private String configKey;
    private String configValueJson;
    private String description;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}