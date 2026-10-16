package com.f2r.payroll.service;

import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.entity.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ZaloNotificationService {

    @Value("${zalo.gateway.url:http://127.0.0.1:5001}")
    private String gatewayUrl;

    @Value("${zalo.admin.id:phucnd}")
    private String adminId;

    private final ReportService reportService;
    private final TelegramService telegramService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DecimalFormat VND_FORMAT = new DecimalFormat("###,###,### VNĐ");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public ZaloNotificationService(
            @Lazy @Autowired(required = false) ReportService reportService,
            @Lazy @Autowired(required = false) TelegramService telegramService) {
        this.reportService = reportService;
        this.telegramService = telegramService;
    }

    public boolean sendPaymentNotification(Employee employee, BigDecimal amount, String notes) {
        return sendPaymentNotification(employee, amount, notes, LocalDate.now());
    }

    /**
     * Gửi tin nhắn thông báo thanh toán qua Zalo cho nhân viên, kèm số dư tài khoản hiện tại.
     * Tự động bỏ qua nếu là chính Admin hoặc nhân viên chưa cấu hình SĐT.
     */
    public boolean sendPaymentNotification(Employee employee, BigDecimal amount, String notes, LocalDate paymentDate) {
        if (employee == null) {
            log.warn("[Zalo Service] Không thể gửi thông báo: Đối tượng nhân viên null");
            return false;
        }

        // 1. Tự động bỏ qua Admin để tránh tự spam chính mình
        if (adminId != null && adminId.equalsIgnoreCase(employee.getId())) {
            log.info("[Zalo Service] Bỏ qua gửi Zalo cho chính Admin ({})", employee.getId());
            return true;
        }

        // 2. Kiểm tra số điện thoại
        String phone = employee.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("[Zalo Service] Nhân viên {} ({}) chưa cài đặt Số điện thoại, không thể gửi Zalo.",
                    employee.getFullName(), employee.getId());
            return false;
        }

        // 3. Chuẩn bị nội dung tin nhắn thông minh
        String formattedMoney = amount != null ? VND_FORMAT.format(amount) : "0 VNĐ";
        String timeStr = LocalDateTime.now().format(TIME_FORMAT);
        String safeNotes = (notes != null && !notes.trim().isEmpty()) ? notes.trim() : "Thanh toán lương";

        String titleHeader = "🔔 [PHÚC QUÂN HR - XÁC NHẬN THANH TOÁN]";
        String lowerNotes = safeNotes.toLowerCase();
        if (lowerNotes.contains("ứng") || lowerNotes.contains("ung")) {
            titleHeader = "🔔 [PHÚC QUÂN HR - THÔNG BÁO TẠM ỨNG]";
        } else if (lowerNotes.contains("thưởng") || lowerNotes.contains("thuong")) {
            titleHeader = "🔔 [PHÚC QUÂN HR - THÔNG BÁO CHI THƯỞNG]";
        } else if (lowerNotes.contains("lương") || lowerNotes.contains("luong")) {
            titleHeader = "🔔 [PHÚC QUÂN HR - XÁC NHẬN THANH TOÁN LƯƠNG]";
        }

        // 4. Lấy số dư hiện tại của tài khoản (Thực nhận còn lại trong tháng)
        LocalDate targetDate = paymentDate != null ? paymentDate : LocalDate.now();
        int month = targetDate.getMonthValue();
        int year = targetDate.getYear();

        BigDecimal balance = null;
        BigDecimal totalWage = null;
        BigDecimal totalAdvance = null;

        try {
            if (reportService != null) {
                MonthlyPayrollResponse stats = reportService.calculateMonthlyPayroll(employee.getId(), month, year);
                if (stats != null) {
                    balance = stats.getActualReceived();
                    totalWage = stats.getTotalWage();
                    totalAdvance = stats.getTotalAdvance();
                }
            }
        } catch (Exception ex) {
            log.warn("[Zalo Service] Không thể tính số dư tài khoản cho {}: {}", employee.getId(), ex.getMessage());
        }

        StringBuilder msg = new StringBuilder();
        msg.append(titleHeader).append("\n\n");
        msg.append("Kính gửi Anh/Chị: ").append(employee.getFullName()).append("\n");
        msg.append("━━━━━━━━━━━━━━━━━━━\n");
        msg.append("💰 Số tiền thanh toán: ").append(formattedMoney).append("\n");
        msg.append("📝 Nội dung: ").append(safeNotes).append("\n");
        msg.append("📅 Thời gian: ").append(timeStr).append("\n");
        msg.append("💳 Trạng thái: Đã thanh toán thành công\n");

        if (balance != null) {
            msg.append("━━━━━━━━━━━━━━━━━━━\n");
            msg.append("📊 THÔNG TIN LƯƠNG THÁNG ").append(month).append("/").append(year).append(":\n");
            if (totalWage != null) {
                msg.append("• Tổng công kiếm được: ").append(VND_FORMAT.format(totalWage)).append("\n");
            }
            if (totalAdvance != null) {
                msg.append("• Đã tạm ứng/thanh toán: ").append(VND_FORMAT.format(totalAdvance)).append("\n");
            }
            msg.append("👉 SỐ DƯ CÒN LẠI HIỆN TẠI: ").append(VND_FORMAT.format(balance)).append("\n");
        }

        msg.append("━━━━━━━━━━━━━━━━━━━\n");
        msg.append("Anh/Chị vui lòng kiểm tra tài khoản nhận. Chúc Anh/Chị một ngày làm việc hiệu quả!");

        // 5. Gửi request sang Zalo Gateway
        try {
            String url = gatewayUrl.replaceAll("/+$", "") + "/api/send-zalo";
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone.trim());
            payload.put("employeeId", employee.getId());
            payload.put("employeeName", employee.getFullName());
            payload.put("message", msg.toString());

            restTemplate.postForObject(url, payload, Map.class);
            log.info("[Zalo Service] ✅ Đã kích hoạt gửi tin nhắn Zalo tới {} ({}) thành công!",
                    employee.getFullName(), phone);
            return true;
        } catch (Exception e) {
            log.error("[Zalo Service] ❌ Lỗi khi gọi Zalo Gateway: {}", e.getMessage());

            // Tự động bắn cảnh báo khẩn cấp qua Telegram cho Admin
            notifyAdminZaloError(employee, formattedMoney, safeNotes, e.getMessage());
            return false;
        }
    }

    /**
     * Bắn thông báo khẩn qua Telegram khi Zalo Gateway gặp sự cố hoặc session bị văng
     */
    private void notifyAdminZaloError(Employee employee, String formattedMoney, String notes, String errorDetail) {
        if (telegramService == null) return;
        try {
            String alert = String.format(
                "🚨 <b>[CẢNH BÁO ZALO BOT - PHÚC QUÂN HR]</b> 🚨\n\n" +
                "❌ <b>KHÔNG THỂ GỬI TIN NHẮN ZALO CHO NHÂN VIÊN!</b>\n\n" +
                "👤 <b>Nhân viên:</b> %s (SĐT: <code>%s</code>)\n" +
                "💰 <b>Số tiền:</b> %s\n" +
                "📝 <b>Nội dung:</b> %s\n" +
                "⚠️ <b>Lỗi:</b> %s\n\n" +
                "👉 <i>Zalo Bot có thể đã hết hạn đăng nhập hoặc chưa được bật. Vui lòng mở trang Quản trị để quét lại mã QR ngay!</i>",
                employee.getFullName(),
                employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "Chưa có",
                formattedMoney,
                notes,
                errorDetail != null ? errorDetail : "Gateway không phản hồi"
            );
            telegramService.sendMessage(alert);
            log.info("[Zalo Service] Đã gửi cảnh báo lỗi gửi Zalo tới Telegram Admin thành công.");
        } catch (Exception ex) {
            log.warn("[Zalo Service] Không thể gửi cảnh báo qua Telegram: {}", ex.getMessage());
        }
    }

    /**
     * Lấy trạng thái của Zalo Gateway để hiển thị trên Web Admin
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getGatewayStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = gatewayUrl.replaceAll("/+$", "") + "/api/status";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                result.putAll(response);
                result.put("serviceOnline", true);
            } else {
                result.put("serviceOnline", false);
                result.put("loggedIn", false);
            }
        } catch (Exception e) {
            result.put("serviceOnline", false);
            result.put("loggedIn", false);
            result.put("error", "Không thể kết nối tới Zalo Gateway (" + e.getMessage() + ")");
        }
        return result;
    }

    /**
     * Yêu cầu Zalo Gateway đăng xuất và tạo mã QR mới
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> logoutGateway() {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = gatewayUrl.replaceAll("/+$", "") + "/api/logout";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            if (response != null) {
                result.putAll(response);
            }
            result.put("serviceOnline", true);
        } catch (Exception e) {
            result.put("serviceOnline", false);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * Làm mới mã QR
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshGatewayQR() {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = gatewayUrl.replaceAll("/+$", "") + "/api/refresh-qr";
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
            if (response != null) {
                result.putAll(response);
            }
            result.put("serviceOnline", true);
        } catch (Exception e) {
            result.put("serviceOnline", false);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
