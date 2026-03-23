package com.itgeo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${website.domain}")
    private String domain;

    /**
     * 配置跨域请求
     * @param registry 跨域注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        String[] allowedOrigins = Arrays.stream(domain.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry.addMapping("/**")        // 当前所有路径都支持跨域
                .allowedOriginPatterns(allowedOrigins)             // 允许指定域名跨域调用
                .allowedMethods("*")                // 允许所有HTTP方法跨域调用
                .allowedHeaders("*")                // 允许所有请求头跨域调用
                .allowCredentials(true)             // 允许携带认证信息跨域调用
                .maxAge(60 * 60);
    }
}
