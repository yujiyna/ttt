package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.config.JwtUtils;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/")
@CrossOrigin
public class GoogleLoginController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;
    @GetMapping("/Callback")
    public Map<String,String> handleGoogleCallback(@RequestParam(name = "code") String code) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");

        // 发送请求到 Google OAuth 服务器以获取访问令牌
        Map<String, String> response = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                params,
                Map.class
        );

        // 处理响应，提取访问令牌
        String accessToken = response.get("access_token");

        // 使用 Access Token 获取用户信息
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v1/userinfo?alt=json";
        String userInfoUrl = userInfoEndpoint + "&access_token=" + accessToken;

        Map<String, Object> userInfo = restTemplate.getForObject(userInfoUrl, Map.class);

        String jwtToken = jwtUtils.generateJwtToken(userInfo.get("name").toString());
        redisTemplate.opsForValue().set(userInfo.get("name").toString(),jwtToken);
        String username=jwtUtils.getUserNameFromJwtToken(jwtToken);
        User user=userMapper.selectOne(new QueryWrapper<User>().eq("username",username));
        HashMap<String, String> map = new HashMap<>();
        if(user!=null){
            map.put("name",user.getUsername());
            map.put("userid",user.getUserid());
            map.put("jwtToken",jwtToken);
            return map;
        }
        String uuid=UUID.randomUUID().toString();
        map.put("name",username);
        map.put("userid",uuid);
        map.put("jwtToken",jwtToken);
        User user1=new User(uuid,username,"123456");
        userMapper.insert(user1);
        return map;
    }


    @GetMapping("/test")
    public String test(){
        return "success";
    }
}
