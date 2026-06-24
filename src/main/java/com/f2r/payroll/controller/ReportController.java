package com.f2r.payroll.controller;

import com.f2r.payroll.dto.EmployeeOverviewItem;
import com.f2r.payroll.dto.MonthlyOverviewResponse;
import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyPayrollResponse> getMonthlyPayroll(
            @RequestParam String employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(reportService.calculateMonthlyPayroll(employeeId, month, year));
    }

    @GetMapping("/monthly-overview")
    public ResponseEntity<MonthlyOverviewResponse> getMonthlyOverview(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(reportService.getMonthlyOverview(month, year));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeOverviewItem>> getEmployeeOverviews(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(reportService.getAllEmployeeOverviews(month, year));
    }
}
