package com.f2r.payroll.controller;

import com.f2r.payroll.service.ZaloNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payroll/zalo")
@RequiredArgsConstructor
public class ZaloController {

    private final ZaloNotificationService zaloNotificationService;

    /**
     * Lấy trạng thái Zalo Gateway và mã QR hiện tại (nếu đang chờ đăng nhập)
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Map<String, Object> status = zaloNotificationService.getGatewayStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Yêu cầu đăng xuất tài khoản Zalo Bot và làm mới mã QR
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Map<String, Object> result = zaloNotificationService.logoutGateway();
        return ResponseEntity.ok(result);
    }

    /**
     * Làm mới mã QR nếu mã cũ bị hết hạn
     */
    @PostMapping("/refresh-qr")
    public ResponseEntity<?> refreshQR(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        Map<String, Object> result = zaloNotificationService.refreshGatewayQR();
        return ResponseEntity.ok(result);
    }
}
