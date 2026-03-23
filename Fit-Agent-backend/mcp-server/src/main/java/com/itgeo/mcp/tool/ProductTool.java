package com.itgeo.mcp.tool;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itgeo.enums.ListSortEnum;
import com.itgeo.enums.PriceCompareEnum;
import com.itgeo.mapper.ProductMapper;
import com.itgeo.pojo.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class ProductTool {

    private final ProductMapper productMapper;

    public ProductTool(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Data
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateProductRequest {
        @ToolParam(description = "商品的名称")
        private String productName;
        @ToolParam(description = "商品的品牌")
        private String brand;
        @ToolParam(description = "商品的描述（可以为空）")
        private String description;

        @ToolParam(description = "商品的价格")
        private Integer price;
        @ToolParam(description = "商品的库存数量")
        private Integer stock;
        @ToolParam(description = "商品的状态（下架的状态值为0/上架的状态值为1/预售的状态值为2）")
        private Integer status;
    }

    /**
     * 创建/新增商品信息记录
     *
     * @param createProductRequest
     * @return
     */
    @Tool(description = "创建/新增商品信息记录")
    public String createNewProduct(CreateProductRequest createProductRequest) {
        log.info("调用MCP工具：createNewProduct");
        log.info(String.format("商品信息记录请求参数 createProductRequest：%s", createProductRequest.toString()));
        log.info("End");

        Product product = new Product();
        BeanUtils.copyProperties(createProductRequest, product);

        //生成12位随机数字作为商品ID
        product.setProductId(RandomStringUtils.randomNumeric(12));
        product.setCreateTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
        product.setUpdateTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime());

        productMapper.insert(product);

        return "商品信息记录创建成功";
    }


    /**
     * 根据商品id删除商品信息记录
     *
     * @param productId
     * @return
     */
    @Transactional    // 涉及到数据库写操作，开启事务管理
    @Tool(description = "根据商品id删除商品信息记录")
    public String deleteProduct(String productId) {
        log.info("调用MCP工具：deleteProduct");
        log.info(String.format("商品ID productId：%s", productId));
        log.info("End");

        // 构建查询条件，根据商品ID删除记录
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);

        productMapper.delete(queryWrapper);

        return "商品信息记录删除成功";
    }

    @Data
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QueryProductRequest {

        // required = true 默认自动填充数据，所以查询的时候建议使用false
        @ToolParam(description = "商品ID编号", required = false)
        private String productId;
        @ToolParam(description = "商品的名称", required = false)
        private String productName;
        @ToolParam(description = "商品的品牌", required = false)
        private String brand;
        @ToolParam(description = "具体商品的价格大小", required = false)
        private Integer price;
        @ToolParam(description = "商品的状态（下架的状态值为0/上架的状态值为1/预售的状态值为2）", required = false)
        private Integer status;

        @ToolParam(description = "查询列表的排序方式（asc升序/desc降序）", required = false)
        private ListSortEnum sortEnum;
        @ToolParam(description = "根据商品价格进行比较", required = false)
        private PriceCompareEnum priceCompareEnum;
    }

    /**
     * 把排序（正序/倒序）转换为对应的枚举
     *
     * @param sort
     * @return
     */
    @Transactional    // 涉及到数据库写操作，开启事务管理
    @Tool(description = "根据商品查询请求参数查询商品信息记录")
    public ListSortEnum getSortEnum(String sort) {
        log.info("调用MCP工具：getSortEnum");
        log.info(String.format("排序参数 sort：%s", sort));
        log.info("End");

        if (sort.equalsIgnoreCase(ListSortEnum.ASC.value)) {
            return ListSortEnum.ASC;
        } else {
            return ListSortEnum.DESC;
        }
    }

    /**
     * 把排序（正序/倒序）转换为对应的枚举
     *
     * @param priceCompare
     * @return
     */
    @Transactional    // 涉及到数据库写操作，开启事务管理
    @Tool(description = "把商品价格的比较（大于/小于/大于等于/小于等于/高于/低于/不高于/不低于/等于）转换为对应的枚举")
    public PriceCompareEnum getPriceCompareEnum(String priceCompare) {
        log.info("调用MCP工具：getPriceCompareEnum");
        log.info(String.format("商品价格比较参数 priceCompare：%s", priceCompare));
        log.info("End");

        if (priceCompare.equalsIgnoreCase(PriceCompareEnum.GREATER_THAN.value)) {
            return PriceCompareEnum.GREATER_THAN;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.LESS_THAN.value)) {
            return PriceCompareEnum.LESS_THAN;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.GREATER_THAN_OR_EQUAL_TO.value)) {
            return PriceCompareEnum.GREATER_THAN_OR_EQUAL_TO;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.LESS_THAN_OR_EQUAL_TO.value)) {
            return PriceCompareEnum.LESS_THAN_OR_EQUAL_TO;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.HIGHER_THAN.value)) {
            return PriceCompareEnum.HIGHER_THAN;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.LOWER_THAN.value)) {
            return PriceCompareEnum.LOWER_THAN;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.NOT_HIGHER_THAN.value)) {
            return PriceCompareEnum.NOT_HIGHER_THAN;
        } else if (priceCompare.equalsIgnoreCase(PriceCompareEnum.NOT_LOWER_THAN.value)) {
            return PriceCompareEnum.NOT_LOWER_THAN;
        } else {
            return PriceCompareEnum.EQUAL_TO;
        }
    }

    /**
     * 根据商品查询请求参数查询商品(product)信息
     *
     * @param queryProductRequest
     * @return
     */
    @Tool(description = "根据商品查询请求参数查询商品(product)信息")
    public List<Product> queryProductListByCondition(QueryProductRequest queryProductRequest) {
        log.info("调用MCP工具：queryProductListByCondition");
        log.info(String.format("商品查询请求参数 queryProductRequest：%s", queryProductRequest.toString()));
        log.info("End");

        String productId = queryProductRequest.getProductId();
        String productName = queryProductRequest.getProductName();
        String brand = queryProductRequest.getBrand();
        Integer status = queryProductRequest.getStatus();
        ListSortEnum sortEnum = queryProductRequest.getSortEnum();

        Integer price = queryProductRequest.getPrice();
        PriceCompareEnum priceCompareEnum = queryProductRequest.getPriceCompareEnum();

        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(productId)) {
            queryWrapper.eq("product_id", productId);
        }
        if (StringUtils.isNotBlank(productName)) {
            queryWrapper.like("product_name", productName);
        }
        if (StringUtils.isNotBlank(brand)) {
            queryWrapper.like("brand", brand);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (price != null && priceCompareEnum != null) {
            if (priceCompareEnum.type.equals(PriceCompareEnum.GREATER_THAN.type)) {
                queryWrapper.gt("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.LESS_THAN.type)) {
                queryWrapper.lt("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.GREATER_THAN_OR_EQUAL_TO.type)) {
                queryWrapper.ge("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.LESS_THAN_OR_EQUAL_TO.type)) {
                queryWrapper.le("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.HIGHER_THAN.type)) {
                queryWrapper.gt("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.LOWER_THAN.type)) {
                queryWrapper.lt("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.NOT_HIGHER_THAN.type)) {
                queryWrapper.le("price", price);
            } else if (priceCompareEnum.type.equals(PriceCompareEnum.NOT_LOWER_THAN.type)) {
                queryWrapper.ge("price", price);
            } else {
                queryWrapper.eq("price", price);
            }
        }

        if (sortEnum != null && sortEnum.type.equals(ListSortEnum.ASC.type)) {
            queryWrapper.orderByAsc("price");
        } else if (sortEnum != null && sortEnum.type.equals(ListSortEnum.DESC.type)) {
            queryWrapper.orderByDesc("price");
        }

        List<Product> productList = productMapper.selectList(queryWrapper);

        return productList;
    }


    @Data
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModifyProductRequest {

        // required = true 默认自动填充数据，所以查询的时候建议使用false
        @ToolParam(description = "商品ID编号", required = false)
        private String productId;
        @ToolParam(description = "商品的名称", required = false)
        private String productName;
        @ToolParam(description = "商品的品牌", required = false)
        private String brand;
        @ToolParam(description = "商品的描述", required = false)
        private String description;
        @ToolParam(description = "具体商品的价格大小", required = false)
        private Integer price;
        @ToolParam(description = "商品的库存数量", required = false)
        private String stock;
        @ToolParam(description = "商品的状态（下架的状态值为0/上架的状态值为1/预售的状态值为2）", required = false)
        private Integer status;
    }

    @Tool(description = "根据商品的编号/ID修改商品信息")
    public String ModifyProductListByCondition(ModifyProductRequest modifyProductRequest) {
        log.info("调用MCP工具：ModifyProductListByCondition");
        log.info(String.format("商品修改请求参数 modifyProductRequest：%s", modifyProductRequest.toString()));
        log.info("End");

        Product product = new Product();
        BeanUtils.copyProperties(modifyProductRequest, product);
        product.setUpdateTime(LocalDateTime.now());

        // 构建查询条件，根据商品ID修改记录
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", product.getProductId());

        int update = productMapper.update(product, queryWrapper);
        if (update <= 0) {
            return "商品信息更新失败，或商品可能不存在";
        } else {
            return "商品信息更新成功";
        }
    }

}
