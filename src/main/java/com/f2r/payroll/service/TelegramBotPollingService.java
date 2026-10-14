package com.f2r.payroll.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f2r.payroll.dto.AdvancePaymentRequest;
import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.entity.AdvanceRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.AdvanceRequestRepository;
import com.f2r.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TelegramBotPollingService {

    private final TelegramService telegramService;
    private final EmployeeRepository employeeRepository;
    private final AdvancePaymentService advancePaymentService;
    private final AdvanceRequestRepository advanceRequestRepository;
    private final ReportService reportService;

    private long lastUpdateId = 0;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate createPollingRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(12000);
        return new RestTemplate(factory);
    }

    @Scheduled(fixedDelay = 1500)
    public void pollTelegramUpdates() {
        String botToken = telegramService.getBotToken();
        String adminChatId = telegramService.getChatId();

        if (botToken == null || botToken.isEmpty() || "REPLACE_ME".equals(botToken)) {
            return;
        }

        try {
            long offset = lastUpdateId > 0 ? (lastUpdateId + 1) : 0;
            String url = String.format("https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=3", botToken, offset);

            RestTemplate restTemplate = createPollingRestTemplate();
            String responseStr = restTemplate.getForObject(url, String.class);

            if (responseStr == null || responseStr.isEmpty()) return;

            JsonNode root = objectMapper.readTree(responseStr);
            if (!root.path("ok").asBoolean(false)) return;

            JsonNode results = root.path("result");
            if (results.isArray()) {
                for (JsonNode update : results) {
                    long updateId = update.path("update_id").asLong();
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId;
                    }

                    // 1. Xử lý nút bấm tương tác (Inline Keyboard Callback)
                    JsonNode callbackQuery = update.path("callback_query");
                    if (!callbackQuery.isMissingNode() && !callbackQuery.isNull()) {
                        handleCallbackQuery(callbackQuery, adminChatId);
                        continue;
                    }

                    // 2. Xử lý tin nhắn văn bản thông thường
                    JsonNode message = update.path("message");
                    if (!message.isMissingNode() && !message.isNull()) {
                        long fromChatId = message.path("chat").path("id").asLong();
                        String text = message.path("text").asText("").trim();

                        if (!text.isEmpty()) {
                            processTelegramMessage(fromChatId, text, adminChatId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log quietly
        }
    }

    private void handleCallbackQuery(JsonNode callbackQuery, String adminChatId) {
        String callbackId = callbackQuery.path("id").asText();
        String data = callbackQuery.path("data").asText("");
        long fromChatId = callbackQuery.path("message").path("chat").path("id").asLong();
        long messageId = callbackQuery.path("message").path("message_id").asLong();
        String chatIdStr = String.valueOf(fromChatId);

        // Security check
        if (adminChatId != null && !adminChatId.isEmpty() && !chatIdStr.equals(adminChatId)) {
            telegramService.answerCallbackQuery(callbackId, "⛔ Bạn không có quyền quản trị!");
            return;
        }

        try {
            if (data.startsWith("PAY_REQ:")) {
                Long reqId = Long.parseLong(data.substring("PAY_REQ:".length()));
                approveRequest(reqId, chatIdStr, messageId, callbackId);
            } else if (data.startsWith("REJ_REQ:")) {
                Long reqId = Long.parseLong(data.substring("REJ_REQ:".length()));
                rejectRequest(reqId, chatIdStr, messageId, callbackId);
            }
        } catch (Exception e) {
            telegramService.answerCallbackQuery(callbackId, "Lỗi: " + e.getMessage());
        }
    }

    private void processTelegramMessage(long chatId, String text, String adminChatId) {
        String chatIdStr = String.valueOf(chatId);

        // Security check
        if (adminChatId != null && !adminChatId.isEmpty() && !chatIdStr.equals(adminChatId)) {
            telegramService.sendMessageToChat(chatIdStr, 
                "⛔ <b>Từ chối truy cập:</b> Bạn không có quyền quản trị hệ thống Phúc Quân HR.");
            return;
        }

        String lower = text.toLowerCase().trim();

        // 1. Phê duyệt nhanh yêu cầu gần nhất: "đã ứng", "da ung", "ok", "duyệt", "duyet"
        if (lower.equals("đã ứng") || lower.equals("da ung") || lower.equals("đã ung") || lower.equals("da ứng")
                || lower.equals("ok") || lower.equals("duyệt") || lower.equals("duyet") || lower.equals("da ung roi")) {
            handleQuickApproveLatest(chatIdStr);
            return;
        }

        // 2. Từ chối nhanh yêu cầu gần nhất: "từ chối", "tu choi", "tc", "reject", "hủy", "huy"
        if (lower.equals("từ chối") || lower.equals("tu choi") || lower.equals("tc") || lower.equals("reject")
                || lower.equals("hủy") || lower.equals("huy")) {
            handleQuickRejectLatest(chatIdStr);
            return;
        }

        // 3. Trợ giúp & Bắt đầu
        if (lower.equals("/start") || lower.equals("help") || lower.equals("/help") || lower.equals("huong dan")) {
            sendHelpMessage(chatIdStr);
            return;
        }

        // 4. Danh sách nhân viên
        if (lower.equals("ds") || lower.equals("ds nv") || lower.equals("/ds") || lower.equals("danh sach")) {
            sendEmployeeListMessage(chatIdStr);
            return;
        }

        // 5. Tra cứu lương
        if (lower.startsWith("luong ") || lower.startsWith("lương ") || lower.startsWith("/luong ")) {
            handleCheckSalaryCommand(chatIdStr, text);
            return;
        }

        // 6. Lệnh ứng tiền có cú pháp: "ung <mã_nv> <số_tiền>" hoặc "ứng <mã_nv> <số_tiền>"
        if (lower.startsWith("ung ") || lower.startsWith("ứng ") || lower.startsWith("/ung ")) {
            handleAdvancePaymentCommand(chatIdStr, text);
            return;
        }

        // Nếu gửi lệnh không nhận diện được
        telegramService.sendMessageToChat(chatIdStr,
            "💡 <i>Lệnh không hợp lệ.</i>\n\n" +
            "• Nhắn <b>đã ứng</b> để duyệt yêu cầu ứng gần nhất\n" +
            "• Nhắn <b>từ chối</b> để từ chối yêu cầu gần nhất\n" +
            "• Hoặc soạn: <code>ung &lt;mã_nv&gt; &lt;số_tiền&gt;</code> (VD: <code>ung phucnd 100k</code>)\n" +
            "Gõ <code>help</code> để xem toàn bộ hướng dẫn.");
    }

    private void handleQuickApproveLatest(String chatIdStr) {
        List<AdvanceRequest> allRequests = advanceRequestRepository.findAllByOrderByRequestDateDesc();
        AdvanceRequest latestPending = allRequests.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .findFirst()
                .orElse(null);

        if (latestPending == null) {
            telegramService.sendMessageToChat(chatIdStr,
                "ℹ️ <b>Hiện tại không có yêu cầu ứng lương nào đang chờ duyệt!</b>\n\n" +
                "💡 Nếu bạn muốn ứng tiền chủ động cho nhân viên, hãy soạn:\n" +
                "<code>ung &lt;mã_nv&gt; &lt;số_tiền&gt;</code>\n" +
                "<i>Ví dụ:</i> <code>ung phucnd 100k</code>");
            return;
        }

        approveRequest(latestPending.getId(), chatIdStr, null, null);
    }

    private void handleQuickRejectLatest(String chatIdStr) {
        List<AdvanceRequest> allRequests = advanceRequestRepository.findAllByOrderByRequestDateDesc();
        AdvanceRequest latestPending = allRequests.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .findFirst()
                .orElse(null);

        if (latestPending == null) {
            telegramService.sendMessageToChat(chatIdStr, "ℹ️ Hiện không có yêu cầu ứng lương nào đang chờ xử lý.");
            return;
        }

        rejectRequest(latestPending.getId(), chatIdStr, null, null);
    }

    private void approveRequest(Long requestId, String chatIdStr, Long messageIdToEdit, String callbackQueryId) {
        Optional<AdvanceRequest> optReq = advanceRequestRepository.findById(requestId);
        if (optReq.isEmpty()) {
            if (callbackQueryId != null) telegramService.answerCallbackQuery(callbackQueryId, "Không tìm thấy yêu cầu này!");
            telegramService.sendMessageToChat(chatIdStr, "⚠️ Không tìm thấy yêu cầu ứng lương #" + requestId);
            return;
        }

        AdvanceRequest req = optReq.get();
        if ("PAID".equals(req.getStatus())) {
            if (callbackQueryId != null) telegramService.answerCallbackQuery(callbackQueryId, "Yêu cầu đã được thanh toán trước đó!");
            telegramService.sendMessageToChat(chatIdStr, "ℹ️ Yêu cầu #" + requestId + " đã được thanh toán trước đó rồi.");
            return;
        }

        LocalDate today = LocalDate.now();
        AdvancePaymentRequest payReq = new AdvancePaymentRequest();
        payReq.setEmployeeId(req.getEmployee().getId());
        payReq.setAmount(BigDecimal.valueOf(req.getAmount()));
        payReq.setAdvanceDate(today);
        payReq.setNotes("Giải ngân ứng lương: " + (req.getReason() != null ? req.getReason() : ""));

        advancePaymentService.createAdvancePayment(payReq);

        req.setStatus("PAID");
        req.setProcessDate(LocalDateTime.now());
        advanceRequestRepository.save(req);

        if (callbackQueryId != null) {
            telegramService.answerCallbackQuery(callbackQueryId, "✅ Đã duyệt và thanh toán thành công!");
        }

        MonthlyPayrollResponse stats = reportService.calculateMonthlyPayroll(req.getEmployee().getId(), today.getMonthValue(), today.getYear());

        String resultText = String.format(
            "✅ <b>ĐÃ DUYỆT & THANH TOÁN ỨNG LƯƠNG!</b>\n\n" +
            "👤 <b>Nhân viên:</b> %s (<code>%s</code>)\n" +
            "💰 <b>Số tiền ứng:</b> <b>%s</b>\n" +
            "📝 <b>Lý do:</b> %s\n" +
            "🌐 <b>Trạng thái trên Web:</b> <b>ĐÃ THANH TOÁN</b>\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n" +
            "📊 <b>Bảng Lương Tháng %d/%d:</b>\n" +
            "• Tổng lương các ca: <b>%s</b>\n" +
            "• Đã tạm ứng: <b>%s</b>\n" +
            "👉 <b>Thực nhận còn lại: <u>%s</u></b>",
            req.getEmployee().getFullName(), req.getEmployee().getId(),
            formatVnd(BigDecimal.valueOf(req.getAmount())),
            req.getReason() != null ? req.getReason() : "Không có",
            today.getMonthValue(), today.getYear(),
            formatVnd(stats.getTotalWage()),
            formatVnd(stats.getTotalAdvance()),
            formatVnd(stats.getActualReceived())
        );

        if (messageIdToEdit != null) {
            telegramService.editMessageText(chatIdStr, messageIdToEdit, resultText);
        } else {
            telegramService.sendMessageToChat(chatIdStr, resultText);
        }
    }

    private void rejectRequest(Long requestId, String chatIdStr, Long messageIdToEdit, String callbackQueryId) {
        Optional<AdvanceRequest> optReq = advanceRequestRepository.findById(requestId);
        if (optReq.isEmpty()) {
            if (callbackQueryId != null) telegramService.answerCallbackQuery(callbackQueryId, "Không tìm thấy yêu cầu này!");
            telegramService.sendMessageToChat(chatIdStr, "⚠️ Không tìm thấy yêu cầu ứng lương #" + requestId);
            return;
        }

        AdvanceRequest req = optReq.get();
        req.setStatus("REJECTED");
        req.setProcessDate(LocalDateTime.now());
        advanceRequestRepository.save(req);

        if (callbackQueryId != null) {
            telegramService.answerCallbackQuery(callbackQueryId, "❌ Đã từ chối yêu cầu!");
        }

        String resultText = String.format(
            "❌ <b>ĐÃ TỪ CHỐI YÊU CẦU ỨNG LƯƠNG!</b>\n\n" +
            "👤 <b>Nhân viên:</b> %s (<code>%s</code>)\n" +
            "💰 <b>Số tiền:</b> %s\n" +
            "🌐 <b>Trạng thái trên Web:</b> <b>ĐÃ TỪ CHỐI</b>",
            req.getEmployee().getFullName(), req.getEmployee().getId(),
            formatVnd(BigDecimal.valueOf(req.getAmount()))
        );

        if (messageIdToEdit != null) {
            telegramService.editMessageText(chatIdStr, messageIdToEdit, resultText);
        } else {
            telegramService.sendMessageToChat(chatIdStr, resultText);
        }
    }

    private void handleAdvancePaymentCommand(String chatIdStr, String text) {
        String clean = text;
        if (clean.toLowerCase().startsWith("/ung ")) clean = clean.substring(5).trim();
        else if (clean.toLowerCase().startsWith("ung ")) clean = clean.substring(4).trim();
        else if (clean.toLowerCase().startsWith("ứng ")) clean = clean.substring(4).trim();

        if (clean.toLowerCase().startsWith("cho ")) clean = clean.substring(4).trim();

        String[] tokens = clean.split("\\s+");
        if (tokens.length < 2) {
            telegramService.sendMessageToChat(chatIdStr,
                "⚠️ <b>Thiếu thông tin ứng lương!</b>\n\n" +
                "Cú pháp đúng: <code>ung &lt;mã_nv&gt; &lt;số_tiền&gt; [ghi_chú]</code>\n\n" +
                "<i>Ví dụ:</i>\n" +
                "• <code>ung phucnd 100k</code>\n" +
                "• <code>ung phucnd 500k đổ xăng</code>\n" +
                "• <code>ung quantv 1tr</code>");
            return;
        }

        String empQuery = null;
        BigDecimal amount = null;
        int noteStartIndex = 2;

        BigDecimal amountFrom0 = parseAmount(tokens[0]);
        BigDecimal amountFrom1 = parseAmount(tokens[1]);

        if (amountFrom0 != null && amountFrom1 == null) {
            amount = amountFrom0;
            empQuery = tokens[1];
            noteStartIndex = 2;
        } else if (amountFrom1 != null) {
            empQuery = tokens[0];
            amount = amountFrom1;
            noteStartIndex = 2;
        } else {
            telegramService.sendMessageToChat(chatIdStr,
                "⚠️ <b>Không nhận diện được số tiền hợp lệ!</b>\n\n" +
                "Ví dụ: <code>100k</code>, <code>500k</code>, <code>1tr</code>, <code>1.5tr</code> hoặc <code>500000</code>.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            telegramService.sendMessageToChat(chatIdStr, "⚠️ Số tiền ứng phải lớn hơn 0!");
            return;
        }

        StringBuilder notesBuilder = new StringBuilder();
        for (int i = noteStartIndex; i < tokens.length; i++) {
            if (notesBuilder.length() > 0) notesBuilder.append(" ");
            notesBuilder.append(tokens[i]);
        }
        String notes = notesBuilder.toString().trim();
        if (notes.isEmpty()) {
            notes = "Ứng lương qua Telegram";
        }

        Employee employee = findEmployee(empQuery);
        if (employee == null) {
            telegramService.sendMessageToChat(chatIdStr,
                String.format("⚠️ <b>Không tìm thấy nhân viên:</b> <code>%s</code>\n\nGõ <code>ds nv</code> để xem danh sách mã nhân viên.", empQuery));
            return;
        }

        try {
            LocalDate today = LocalDate.now();

            AdvancePaymentRequest payReq = new AdvancePaymentRequest();
            payReq.setEmployeeId(employee.getId());
            payReq.setAmount(amount);
            payReq.setAdvanceDate(today);
            payReq.setNotes(notes);

            advancePaymentService.createAdvancePayment(payReq);

            // Tự động duyệt yêu cầu đang chờ nếu có
            List<AdvanceRequest> pendingRequests = advanceRequestRepository.findByEmployeeIdAndStatus(employee.getId(), "PENDING");
            if (pendingRequests != null && !pendingRequests.isEmpty()) {
                for (AdvanceRequest req : pendingRequests) {
                    req.setStatus("PAID");
                    req.setProcessDate(LocalDateTime.now());
                    advanceRequestRepository.save(req);
                }
            }

            int month = today.getMonthValue();
            int year = today.getYear();
            MonthlyPayrollResponse stats = reportService.calculateMonthlyPayroll(employee.getId(), month, year);

            String formattedDate = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String reply = String.format(
                "✅ <b>ĐÃ GHI NHẬN ỨNG LƯƠNG THÀNH CÔNG!</b>\n\n" +
                "👤 <b>Nhân viên:</b> %s (<code>%s</code>)\n" +
                "💰 <b>Số tiền ứng:</b> <b>%s</b>\n" +
                "📅 <b>Ngày ghi nhận:</b> %s\n" +
                "📝 <b>Ghi chú:</b> %s\n" +
                "🌐 <b>Trạng thái trên Web:</b> <b>ĐÃ THANH TOÁN</b>\n\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📊 <b>Bảng Lương Tháng %d/%d:</b>\n" +
                "• Tổng lương các ca: <b>%s</b>\n" +
                "• Đã tạm ứng (tất cả): <b>%s</b>\n" +
                "👉 <b>Thực nhận còn lại: <u>%s</u></b>",
                employee.getFullName(), employee.getId(),
                formatVnd(amount),
                formattedDate,
                notes,
                month, year,
                formatVnd(stats.getTotalWage()),
                formatVnd(stats.getTotalAdvance()),
                formatVnd(stats.getActualReceived())
            );

            telegramService.sendMessageToChat(chatIdStr, reply);

        } catch (Exception e) {
            telegramService.sendMessageToChat(chatIdStr, "❌ <b>Lỗi xử lý:</b> " + e.getMessage());
        }
    }

    private void handleCheckSalaryCommand(String chatIdStr, String text) {
        String clean = text;
        if (clean.toLowerCase().startsWith("/luong ")) clean = clean.substring(7).trim();
        else if (clean.toLowerCase().startsWith("luong ")) clean = clean.substring(6).trim();
        else if (clean.toLowerCase().startsWith("lương ")) clean = clean.substring(6).trim();

        if (clean.isEmpty()) {
            telegramService.sendMessageToChat(chatIdStr, "⚠️ Cú pháp: <code>luong &lt;mã_nv&gt;</code>\nVí dụ: <code>luong phucnd</code>");
            return;
        }

        Employee employee = findEmployee(clean);
        if (employee == null) {
            telegramService.sendMessageToChat(chatIdStr, String.format("⚠️ Không tìm thấy nhân viên: <code>%s</code>", clean));
            return;
        }

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        MonthlyPayrollResponse stats = reportService.calculateMonthlyPayroll(employee.getId(), month, year);

        String bankInfo = (employee.getBankName() != null && employee.getBankAccountNumber() != null)
                ? String.format("%s - %s", employee.getBankName(), employee.getBankAccountNumber())
                : "Chưa cập nhật";

        int shiftCount = stats.getWorkDetails() != null ? (int) stats.getWorkDetails().stream().filter(d -> !"TỔNG TIỀN CẮT THÁNG".equals(d.getShift())).count() : 0;

        String reply = String.format(
            "📊 <b>BẢNG LƯƠNG THÁNG %d/%d</b>\n\n" +
            "👤 <b>Nhân viên:</b> %s (<code>%s</code>)\n" +
            "📞 <b>SĐT:</b> %s\n" +
            "🏦 <b>Ngân hàng:</b> %s\n\n" +
            "• <b>Số ca đã làm:</b> %d ca\n" +
            "• <b>Tổng tiền lương:</b> %s\n" +
            "• <b>Đã tạm ứng:</b> %s\n" +
            "━━━━━━━━━━━━━━━━━━━━\n" +
            "👉 <b>Thực nhận còn lại: <u>%s</u></b>",
            month, year,
            employee.getFullName(), employee.getId(),
            employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "Chưa có",
            bankInfo,
            shiftCount,
            formatVnd(stats.getTotalWage()),
            formatVnd(stats.getTotalAdvance()),
            formatVnd(stats.getActualReceived())
        );

        telegramService.sendMessageToChat(chatIdStr, reply);
    }

    private void sendEmployeeListMessage(String chatIdStr) {
        List<Employee> list = employeeRepository.findByIsDeletedFalseOrIsDeletedIsNull();
        if (list.isEmpty()) {
            telegramService.sendMessageToChat(chatIdStr, "👥 Hiện chưa có nhân viên nào trong hệ thống.");
            return;
        }

        StringBuilder sb = new StringBuilder("👥 <b>DANH SÁCH NHÂN VIÊN HỆ THỐNG:</b>\n\n");
        for (Employee emp : list) {
            sb.append("• <code>").append(emp.getId()).append("</code> : <b>").append(emp.getFullName()).append("</b>");
            if (emp.getPhoneNumber() != null && !emp.getPhoneNumber().isEmpty()) {
                sb.append(" (").append(emp.getPhoneNumber()).append(")");
            }
            sb.append("\n");
        }
        sb.append("\n<i>💡 Nhắn <code>ung &lt;mã_nv&gt; &lt;số_tiền&gt;</code> để ứng lương nhanh.</i>");

        telegramService.sendMessageToChat(chatIdStr, sb.toString());
    }

    private void sendHelpMessage(String chatIdStr) {
        String msg = "🤖 <b>TRỢ LÝ TÍNH LƯƠNG PHÚC QUÂN HR</b>\n\n" +
                "Bạn có thể nhắn tin trực tiếp để ứng lương hoặc duyệt yêu cầu mà không cần vào Web:\n\n" +
                "⚡ <b>1. Duyệt / Từ chối yêu cầu ứng gần nhất:</b>\n" +
                "• Nhắn: <code>đã ứng</code> hoặc <code>duyệt</code> (hoặc bấm nút dưới tin báo)\n" +
                "• Nhắn: <code>từ chối</code> để từ chối yêu cầu\n\n" +
                "💰 <b>2. Chủ động ứng tiền cho nhân viên bất kỳ:</b>\n" +
                "• Cú pháp: <code>ung &lt;mã_nv&gt; &lt;số_tiền&gt; [lý_do]</code>\n" +
                "<i>Ví dụ:</i>\n" +
                "- <code>ung phucnd 100k</code>\n" +
                "- <code>ung phucnd 500k đổ xăng</code>\n" +
                "- <code>ung quantv 1tr</code>\n\n" +
                "📊 <b>3. Tra cứu lương tháng này:</b>\n" +
                "• Cú pháp: <code>luong &lt;mã_nv&gt;</code>\n" +
                "<i>Ví dụ:</i> <code>luong phucnd</code>\n\n" +
                "👥 <b>4. Xem danh sách nhân viên:</b>\n" +
                "• Cú pháp: <code>ds nv</code>";

        telegramService.sendMessageToChat(chatIdStr, msg);
    }

    private Employee findEmployee(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim();

        Employee direct = employeeRepository.findById(q).orElse(null);
        if (direct != null && !Boolean.TRUE.equals(direct.getIsDeleted())) return direct;

        List<Employee> all = employeeRepository.findByIsDeletedFalseOrIsDeletedIsNull();
        for (Employee emp : all) {
            if (emp.getId().equalsIgnoreCase(q)) return emp;
        }

        String normQ = removeDiacritics(q).toLowerCase();
        List<Employee> matched = new ArrayList<>();
        for (Employee emp : all) {
            if (emp.getFullName() != null) {
                String normName = removeDiacritics(emp.getFullName()).toLowerCase();
                if (normName.contains(normQ)) {
                    matched.add(emp);
                }
            }
        }

        if (matched.size() == 1) {
            return matched.get(0);
        }

        return null;
    }

    private BigDecimal parseAmount(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String s = input.trim().toLowerCase()
                .replace(",", ".")
                .replace("₫", "")
                .replace("vnd", "")
                .replace("vnđ", "");

        try {
            if (s.endsWith("tr") || s.endsWith("trieu") || s.endsWith("triệu") || s.endsWith("m")) {
                String num = s.replaceAll("[^0-9.]", "");
                double val = Double.parseDouble(num);
                return BigDecimal.valueOf((long) (val * 1_000_000));
            } else if (s.endsWith("k")) {
                String num = s.replaceAll("[^0-9.]", "");
                double val = Double.parseDouble(num);
                return BigDecimal.valueOf((long) (val * 1_000));
            } else {
                String num = s.replace(".", "");
                long val = Long.parseLong(num);
                return BigDecimal.valueOf(val);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,.0f ₫", amount);
    }

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
