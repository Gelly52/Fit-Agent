package com.itgeo;

import com.itgeo.auth.TokenAuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {

    @Resource
    private TokenAuthInterceptor tokenAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/user/**", "/chat/**", "/rag/**", "/internet/**", "/sse/**")
                .excludePathPatterns("/user/code", "/user/login", "/hello/**", "/sse/connect", "/error");
    }
}
