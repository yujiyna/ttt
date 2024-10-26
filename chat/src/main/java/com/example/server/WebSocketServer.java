package com.example.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@ServerEndpoint("/sendMessage/{uid}")
public class WebSocketServer {

    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<String> onlineUsers = new CopyOnWriteArrayList<>();

    private Session session;
    private String uid;

    @OnOpen
    public void onOpen(Session session, @PathParam("uid") String uid) {
        this.session = session;
        this.uid = uid;

        if (!onlineUsers.contains(uid)) {
            onlineUsers.add(uid);
        }

        webSocketMap.put(uid, this);
        onlineCount.incrementAndGet();

        log.info("用户连接： " + uid + ", 当前在线人数为： " + onlineCount.get());
    }

    @OnMessage
    public void onMessage(String message) {
        JSONObject jsonObject = JSON.parseObject(message);
        String txt = jsonObject.getString("message");
        String toId = jsonObject.getString("toId");

        log.info("收到来自用户 " + uid + " 的消息：" + txt + "，目标用户：" + toId);

        WebSocketServer targetUser = webSocketMap.get(toId);
        if (targetUser != null) {
            targetUser.sendMessage(message);
            log.info("消息发送给用户 " + toId);
        } else {
            log.info("用户 " + toId + " 不在线，消息未发送。");
        }
    }

    public void sendMessage(String message) {
        try {
            this.session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            log.error("消息发送失败", e);
        }
    }

    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(uid)) {
            webSocketMap.remove(uid);
            onlineCount.decrementAndGet();
            onlineUsers.remove(uid);
        }
        log.info("用户退出: " + uid + ", 当前在线人数为: " + onlineCount.get());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误: " + this.uid + ", 原因: " + error.getMessage());
        error.printStackTrace();
    }
}
