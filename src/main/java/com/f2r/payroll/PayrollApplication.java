package com.f2r.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class PayrollApplication {

    @PostConstruct
    public void init() {
        // Đặt múi giờ mặc định cho toàn bộ ứng dụng là giờ Việt Nam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(PayrollApplication.class, args);
    }

}
