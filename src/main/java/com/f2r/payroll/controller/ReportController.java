package com.f2r.payroll.controller;

import com.f2r.payroll.dto.EmployeeOverviewItem;
import com.f2r.payroll.dto.MonthlyOverviewResponse;
import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUsername = auth != null ? auth.getName() : null;

        // Non-admin can only view their own payroll report
        if (!isAdmin && (currentUsername == null || !currentUsername.equals(employeeId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reportService.calculateMonthlyPayroll(employeeId, month, year));
    }

    @GetMapping("/monthly-overview")
    public ResponseEntity<MonthlyOverviewResponse> getMonthlyOverview(
            @RequestParam int month,
            @RequestParam int year) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.getMonthlyOverview(month, year));
    }

    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeOverviewItem>> getEmployeeOverviews(
            @RequestParam int month,
            @RequestParam int year) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reportService.getAllEmployeeOverviews(month, year));
    }
}
