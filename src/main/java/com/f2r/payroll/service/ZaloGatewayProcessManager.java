package com.f2r.payroll.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.Socket;

@Slf4j
@Component
public class ZaloGatewayProcessManager {

    @Value("${zalo.gateway.auto-start:true}")
    private boolean autoStart;

    private Process gatewayProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoStart) {
            log.info("[Zalo Gateway Manager] Tính năng tự động bật Gateway đang tắt (auto-start=false)");
            return;
        }

        // Kiểm tra xem cổng 5001 đã có tiến trình nào lắng nghe chưa
        if (isPortInUse(5001)) {
            log.info("[Zalo Gateway Manager] Cổng 5001 đã có tiến trình chạy sẵn, không cần khởi động lại.");
            return;
        }

        File gatewayDir = new File("zalo-gateway");
        if (!gatewayDir.exists() || !gatewayDir.isDirectory()) {
            File parentGatewayDir = new File("../zalo-gateway");
            if (parentGatewayDir.exists() && parentGatewayDir.isDirectory()) {
                gatewayDir = parentGatewayDir;
            } else {
                log.warn("[Zalo Gateway Manager] Không tìm thấy thư mục 'zalo-gateway', bỏ qua tự động khởi chạy.");
                return;
            }
        }

        File scriptFile = new File(gatewayDir, "index.js");
        if (!scriptFile.exists()) {
            log.warn("[Zalo Gateway Manager] Không tìm thấy 'zalo-gateway/index.js'");
            return;
        }

        try {
            log.info("[Zalo Gateway Manager] Đang tự động khởi chạy Zalo Gateway (Node.js) tại {}...", gatewayDir.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder("node", "index.js");
            pb.directory(gatewayDir);
            
            // Kế thừa IO để log của Node.js xuất hiện trực tiếp trong console / tab Logs của Render
            pb.inheritIO();

            gatewayProcess = pb.start();
            log.info("[Zalo Gateway Manager] ✅ Đã khởi chạy tiến trình Zalo Gateway thành công!");
        } catch (Exception e) {
            log.warn("[Zalo Gateway Manager] Không thể tự động khởi động Zalo Gateway: {}. Bạn có thể khởi động thủ công bằng 'cd zalo-gateway && npm start'", e.getMessage());
        }
    }

    private boolean isPortInUse(int port) {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @PreDestroy
    public void stopGateway() {
        if (gatewayProcess != null && gatewayProcess.isAlive()) {
            log.info("[Zalo Gateway Manager] Đang dừng tiến trình Zalo Gateway...");
            gatewayProcess.destroy();
        }
    }
}
