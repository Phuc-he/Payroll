package com.f2r.payroll.controller;

import com.f2r.payroll.entity.AdvancePayment;
import com.f2r.payroll.entity.AdvanceRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.AdvanceRequestRepository;
import com.f2r.payroll.repository.EmployeeRepository;
import com.f2r.payroll.service.AdvancePaymentService;
import com.f2r.payroll.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payroll/advance-requests")
@RequiredArgsConstructor
public class AdvanceRequestController {

    private final AdvanceRequestRepository advanceRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final TelegramService telegramService;
    private final AdvancePaymentService advancePaymentService;

    // 1. Employee creates a request
    @PostMapping
    public ResponseEntity<?> createRequest(Authentication auth, @RequestBody Map<String, Object> payload) {
        String empId = auth.getName();
        if ("admin".equals(empId)) {
            return ResponseEntity.badRequest().body("Admin cannot request advance salary.");
        }

        Employee emp = employeeRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Double amount = Double.valueOf(payload.get("amount").toString());
        String reason = (String) payload.get("reason");

        AdvanceRequest req = AdvanceRequest.builder()
                .employee(emp)
                .amount(amount)
                .reason(reason)
                .requestDate(LocalDateTime.now())
                .status("PENDING")
                .build();

        advanceRequestRepository.save(req);

        // Send Telegram Notification
        String message = String.format("🚨 <b>YÊU CẦU ỨNG LƯƠNG MỚI</b> 🚨\n\n" +
                        "👤 <b>Nhân viên:</b> %s (%s)\n" +
                        "💰 <b>Số tiền:</b> %,.0f VNĐ\n" +
                        "📝 <b>Lý do:</b> %s\n\n" +
                        "<i>Hãy truy cập phần mềm bằng tài khoản Admin để duyệt!</i>",
                emp.getFullName(), emp.getId(), amount, reason);
        telegramService.sendMessage(message);

        return ResponseEntity.ok(toDto(req));
    }

    // 2. Employee gets their own requests
    @GetMapping("/me")
    public ResponseEntity<List<AdvanceRequestDTO>> getMyRequests(Authentication auth) {
        String empId = auth.getName();
        return ResponseEntity.ok(advanceRequestRepository.findByEmployeeIdOrderByRequestDateDesc(empId).stream().map(this::toDto).toList());
    }

    // 3. Admin gets all requests
    @GetMapping
    public ResponseEntity<List<AdvanceRequestDTO>> getAllRequests(Authentication auth) {
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(advanceRequestRepository.findAllByOrderByRequestDateDesc().stream().map(this::toDto).toList());
    }

    // 4. Admin pays the request
    @PutMapping("/{id}/pay")
    public ResponseEntity<?> payRequest(Authentication auth, @PathVariable Long id) {
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }

        Optional<AdvanceRequest> optReq = advanceRequestRepository.findById(id);
        if (optReq.isEmpty()) return ResponseEntity.notFound().build();

        AdvanceRequest req = optReq.get();
        if ("PAID".equals(req.getStatus())) {
            return ResponseEntity.badRequest().body("Yêu cầu này đã được thanh toán rồi.");
        }

        // Create Advance Payment to deduct from salary
        com.f2r.payroll.dto.AdvancePaymentRequest paymentReq = new com.f2r.payroll.dto.AdvancePaymentRequest();
        paymentReq.setEmployeeId(req.getEmployee().getId());
        paymentReq.setAmount(java.math.BigDecimal.valueOf(req.getAmount()));
        paymentReq.setAdvanceDate(LocalDate.now());
        paymentReq.setNotes("Giải ngân ứng lương: " + req.getReason());
        
        advancePaymentService.createAdvancePayment(paymentReq);

        // Update Request Status
        req.setStatus("PAID");
        req.setProcessDate(LocalDateTime.now());
        advanceRequestRepository.save(req);

        return ResponseEntity.ok(toDto(req));
    }

    // 5. Admin rejects the request
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(Authentication auth, @PathVariable Long id) {
        if (!"admin".equals(auth.getName())) {
            return ResponseEntity.status(403).build();
        }

        Optional<AdvanceRequest> optReq = advanceRequestRepository.findById(id);
        if (optReq.isEmpty()) return ResponseEntity.notFound().build();

        AdvanceRequest req = optReq.get();
        if (!"PENDING".equals(req.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ có thể từ chối yêu cầu ĐANG XỬ LÝ.");
        }

        req.setStatus("REJECTED");
        req.setProcessDate(LocalDateTime.now());
        advanceRequestRepository.save(req);

        return ResponseEntity.ok(toDto(req));
    }

    private AdvanceRequestDTO toDto(AdvanceRequest req) {
        AdvanceRequestDTO dto = new AdvanceRequestDTO();
        dto.setId(req.getId());
        dto.setAmount(req.getAmount());
        dto.setReason(req.getReason());
        dto.setRequestDate(req.getRequestDate());
        dto.setStatus(req.getStatus());
        dto.setProcessDate(req.getProcessDate());
        if (req.getEmployee() != null) {
            EmployeeDTO empDto = new EmployeeDTO();
            empDto.setId(req.getEmployee().getId());
            empDto.setFullName(req.getEmployee().getFullName());
            dto.setEmployee(empDto);
        }
        return dto;
    }

    @lombok.Data
    public static class AdvanceRequestDTO {
        private Long id;
        private Double amount;
        private String reason;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime requestDate;
        private String status;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime processDate;
        private EmployeeDTO employee;
    }

    @lombok.Data
    public static class EmployeeDTO {
        private String id;
        private String fullName;
    }
}
