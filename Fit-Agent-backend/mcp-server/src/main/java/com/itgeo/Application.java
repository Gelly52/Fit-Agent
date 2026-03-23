package com.itgeo;

import com.itgeo.mcp.tool.DateTool;
import com.itgeo.mcp.tool.EmailTool;
import com.itgeo.mcp.tool.ProductTool;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author gzx
 * &#064;description:  应用入口
 * &#064;date  2024-05-20 10:00:00
 */
@MapperScan("com.itgeo.mapper")
@SpringBootApplication
public class Application {

    //http://localhost:9060/sse

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // 注册MCP工具回调提供器
    @Bean
    public ToolCallbackProvider registMCPTools(DateTool dateTool, EmailTool emailTool, ProductTool productTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(dateTool, emailTool, productTool)
                .build();

    }

}
