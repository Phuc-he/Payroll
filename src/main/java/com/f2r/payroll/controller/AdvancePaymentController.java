package com.f2r.payroll.controller;

import com.f2r.payroll.dto.AdvancePaymentRequest;
import com.f2r.payroll.service.AdvancePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll/advance-payments")
@RequiredArgsConstructor
public class AdvancePaymentController {

    private final AdvancePaymentService advancePaymentService;

    @PostMapping
    public ResponseEntity<Long> createAdvancePayment(@RequestBody AdvancePaymentRequest request) {
        return ResponseEntity.ok(advancePaymentService.createAdvancePayment(request).getId());
    }
}
