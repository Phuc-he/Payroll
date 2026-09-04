package com.f2r.payroll.controller;

import com.f2r.payroll.dto.EmployeeRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.core.Authentication;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<String> createEmployee(@RequestBody EmployeeRequest request) {
        return new ResponseEntity<>(employeeService.createEmployee(request).getId(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @PutMapping("/me/bank")
    public ResponseEntity<?> updateMyBankInfo(Authentication authentication, @RequestBody Map<String, String> payload) {
        String employeeId = authentication.getName();
        employeeService.updateBankInfo(employeeId, payload.get("bankName"), payload.get("bankAccountNumber"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok().build();
    }
}
