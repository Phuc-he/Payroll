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

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAdvancePayment(@PathVariable Long id, @RequestBody AdvancePaymentRequest request) {
        advancePaymentService.updateAdvancePayment(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvancePayment(@PathVariable Long id) {
        advancePaymentService.deleteAdvancePayment(id);
        return ResponseEntity.noContent().build();
    }
}
