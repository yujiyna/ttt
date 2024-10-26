package com.example.filter;

import com.example.config.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@CrossOrigin
public class JwtFilter implements WebFilter {

    @Value("${jwt.data.SECRET}")
    private String jwtSecret;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // 处理 CORS 的 OPTIONS 预检请求
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponse().getHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        List<String> authHeader = exchange.getRequest().getHeaders().getOrEmpty("Authorization");
        String token = authHeader.get(0).substring(7);
        if (authHeader.isEmpty() || !authHeader.get(0).startsWith("Bearer ")||!jwtUtils.validateJwtToken(token)) {
            log.error("登陆失败");
            // 没有JWT或格式不正确，返回401 Unauthorized
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        String redisToken = (String) redisTemplate.opsForValue().get(username);

        if (redisToken == null || !redisToken.equals(token)) {
            // 如果Redis中没有找到JWT或JWT不匹配，返回401 Unauthorized
            log.error("登陆失败");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 网关签名，只能通过网关访问模块
        exchange.getRequest().mutate().header("X-Custom-Signature", "gateway-secret");

        // 如果JWT匹配，则继续处理请求
        return chain.filter(exchange);
    }
}
