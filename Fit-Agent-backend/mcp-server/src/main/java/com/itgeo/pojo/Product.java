package com.itgeo.pojo;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class Product {
    private String productId;
    private String productName;
    private String brand;
    private String description;

    private Integer price;
    private Integer stock;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
