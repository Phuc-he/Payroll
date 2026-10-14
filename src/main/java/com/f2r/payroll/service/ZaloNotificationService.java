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

    @Value("${zalo.gateway.url:http://localhost:5001}")
    private String gatewayUrl;

    @Value("${zalo.admin.id:phucnd}")
    private String adminId;

    private final ReportService reportService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DecimalFormat VND_FORMAT = new DecimalFormat("###,###,### VNĐ");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    public ZaloNotificationService(@Lazy @Autowired(required = false) ReportService reportService) {
        this.reportService = reportService;
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
            return false;
        }
    }
}
