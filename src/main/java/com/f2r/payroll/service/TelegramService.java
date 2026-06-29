package com.f2r.payroll.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    public void sendMessage(String message) {
        if (chatId == null || chatId.isEmpty() || "REPLACE_ME".equals(chatId)) {
            System.out.println("Telegram chat ID is not configured. Message: " + message);
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();
            
            Map<String, String> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", message);
            payload.put("parse_mode", "HTML");

            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
