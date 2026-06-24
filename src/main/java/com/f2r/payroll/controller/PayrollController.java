package com.f2r.payroll.controller;

import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.dto.WorkScheduleRequest;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/work-schedules")
    public ResponseEntity<Long> createWorkSchedule(@RequestBody WorkScheduleRequest request) {
        return new ResponseEntity<>(payrollService.createWorkSchedule(request).getId(), HttpStatus.CREATED);
    }

    @GetMapping("/work-schedules/{id}/summary")
    public ResponseEntity<WorkScheduleSummaryResponse> getWorkScheduleSummary(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getWorkScheduleSummary(id));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<MonthlyPayrollResponse> getMonthlyPayroll(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.calculateMonthlyPayroll(employeeId, month, year));
    }

    @GetMapping("/reports/monthly-overview")
    public ResponseEntity<com.f2r.payroll.dto.MonthlyOverviewResponse> getMonthlyOverview(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.getMonthlyOverview(month, year));
    }

    @GetMapping("/reports/employees")
    public ResponseEntity<java.util.List<com.f2r.payroll.dto.EmployeeOverviewItem>> getEmployeeOverviews(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(payrollService.getAllEmployeeOverviews(month, year));
    }

    @PutMapping("/work-schedules/{id}/status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        payrollService.updatePaymentStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/work-schedules/{id}")
    public ResponseEntity<Void> deleteWorkSchedule(@PathVariable Long id) {
        payrollService.deleteWorkSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/work-schedules/{id}")
    public ResponseEntity<Void> updateWorkSchedule(
            @PathVariable Long id,
            @RequestBody com.f2r.payroll.dto.WorkScheduleRequest request) {
        payrollService.updateWorkSchedule(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/advance-payments")
    public ResponseEntity<Long> createAdvancePayment(@RequestBody com.f2r.payroll.dto.AdvancePaymentRequest request) {
        return ResponseEntity.ok(payrollService.createAdvancePayment(request).getId());
    }

    @PostMapping("/employees")
    public ResponseEntity<String> createEmployee(@RequestBody com.f2r.payroll.dto.EmployeeRequest request) {
        return new ResponseEntity<>(payrollService.createEmployee(request).getId(), HttpStatus.CREATED);
    }

    @GetMapping("/locations")
    public ResponseEntity<java.util.List<com.f2r.payroll.entity.Location>> getAllLocations() {
        return ResponseEntity.ok(payrollService.getAllLocations());
    }

    @GetMapping("/employees")
    public ResponseEntity<java.util.List<com.f2r.payroll.entity.Employee>> getAllEmployees() {
        return ResponseEntity.ok(payrollService.getAllEmployees());
    }
}
