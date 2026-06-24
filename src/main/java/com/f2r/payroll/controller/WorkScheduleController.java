package com.f2r.payroll.controller;

import com.f2r.payroll.dto.WorkScheduleRequest;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;

    @PostMapping
    public ResponseEntity<Long> createWorkSchedule(@RequestBody WorkScheduleRequest request) {
        return new ResponseEntity<>(workScheduleService.createWorkSchedule(request).getId(), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<WorkScheduleSummaryResponse> getWorkScheduleSummary(@PathVariable Long id) {
        return ResponseEntity.ok(workScheduleService.getWorkScheduleSummary(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        workScheduleService.updatePaymentStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkSchedule(@PathVariable Long id) {
        workScheduleService.deleteWorkSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateWorkSchedule(
            @PathVariable Long id,
            @RequestBody WorkScheduleRequest request) {
        workScheduleService.updateWorkSchedule(id, request);
        return ResponseEntity.ok().build();
    }
}
