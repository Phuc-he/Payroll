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
        sendMessageToChat(this.chatId, message);
    }

    public void sendMessageToChat(String targetChatId, String message) {
        if (targetChatId == null || targetChatId.isEmpty() || "REPLACE_ME".equals(targetChatId)) {
            System.out.println("Telegram chat ID is not configured. Message: " + message);
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();
            
            Map<String, String> payload = new HashMap<>();
            payload.put("chat_id", targetChatId);
            payload.put("text", message);
            payload.put("parse_mode", "HTML");

            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            System.err.println("Lỗi gửi tin nhắn Telegram: " + e.getMessage());
        }
    }

    public void sendAdvanceRequestNotification(Long requestId, String fullName, String empId, Double amount, String reason) {
        if (chatId == null || chatId.isEmpty() || "REPLACE_ME".equals(chatId)) {
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();

            String message = String.format(
                "🚨 <b>YÊU CẦU ỨNG LƯƠNG MỚI</b> 🚨\n\n" +
                "👤 <b>Nhân viên:</b> %s (<code>%s</code>)\n" +
                "💰 <b>Số tiền:</b> %,.0f VNĐ\n" +
                "📝 <b>Lý do:</b> %s\n\n" +
                "<i>Bấm nút bên dưới hoặc nhắn <b>đã ứng</b> / <b>từ chối</b> để xử lý:</i>",
                fullName, empId, amount, reason != null ? reason : "Không có"
            );

            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId);
            payload.put("text", message);
            payload.put("parse_mode", "HTML");

            // Tạo nút bấm tương tác
            java.util.List<java.util.List<Map<String, String>>> inlineKeyboard = new java.util.ArrayList<>();
            java.util.List<Map<String, String>> row = new java.util.ArrayList<>();

            Map<String, String> btnPay = new HashMap<>();
            btnPay.put("text", "✅ Đã ứng (Duyệt)");
            btnPay.put("callback_data", "PAY_REQ:" + requestId);

            Map<String, String> btnReject = new HashMap<>();
            btnReject.put("text", "❌ Từ chối");
            btnReject.put("callback_data", "REJ_REQ:" + requestId);

            row.add(btnPay);
            row.add(btnReject);
            inlineKeyboard.add(row);

            Map<String, Object> replyMarkup = new HashMap<>();
            replyMarkup.put("inline_keyboard", inlineKeyboard);
            payload.put("reply_markup", replyMarkup);

            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo có nút bấm: " + e.getMessage());
        }
    }

    public void editMessageText(String targetChatId, Long messageId, String newText) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", targetChatId);
            payload.put("message_id", messageId);
            payload.put("text", newText);
            payload.put("parse_mode", "HTML");

            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            System.err.println("Lỗi cập nhật tin nhắn Telegram: " + e.getMessage());
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> payload = new HashMap<>();
            payload.put("callback_query_id", callbackQueryId);
            if (text != null && !text.isEmpty()) {
                payload.put("text", text);
            }

            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            System.err.println("Lỗi phản hồi callback query: " + e.getMessage());
        }
    }

    public String getBotToken() {
        return botToken;
    }

    public String getChatId() {
        return chatId;
    }
}
