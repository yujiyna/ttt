//package com.example.confog;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**") // 允许所有路径
//                .allowedOrigins("http://localhost:7990") // 允许的来源
//                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的HTTP方法
//                .allowedHeaders("*") // 允许的请求头
//                .allowCredentials(true) // 是否允许带凭据
//                .maxAge(3600); // 预检请求的缓存时间
//    }
//}
