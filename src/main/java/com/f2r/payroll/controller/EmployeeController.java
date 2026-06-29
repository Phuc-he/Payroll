package com.f2r.payroll.controller;

import com.f2r.payroll.dto.EmployeeRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<?> updateMyBankInfo(org.springframework.security.core.Authentication auth, @RequestBody java.util.Map<String, String> payload) {
        String empId = auth.getName();
        String bankName = payload.get("bankName");
        String bankAccountNumber = payload.get("bankAccountNumber");
        employeeService.updateBankInfo(empId, bankName, bankAccountNumber);
        return ResponseEntity.ok().build();
    }
}
