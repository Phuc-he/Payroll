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

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && (
            "admin".equals(authentication.getName()) ||
            authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
        );
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(Authentication authentication, @RequestBody EmployeeRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ quản trị viên mới có quyền tạo nhân viên");
        }
        return new ResponseEntity<>(employeeService.createEmployee(request).getId(), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(Authentication authentication, @PathVariable String id) {
        if (!isAdmin(authentication) && (authentication == null || !authentication.getName().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Không có quyền truy cập thông tin nhân viên này");
        }
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody EmployeeRequest request) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ quản trị viên mới có quyền cập nhật thông tin nhân viên");
        }
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            Authentication authentication,
            @PathVariable String id) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ quản trị viên mới có quyền đặt lại mật khẩu nhân viên");
        }
        employeeService.resetPassword(id);
        return ResponseEntity.ok(Map.of("message", "Đã đặt lại mật khẩu của nhân viên " + id + " về mặc định (123456)"));
    }

    @PutMapping("/me/bank")
    public ResponseEntity<?> updateMyBankInfo(Authentication authentication, @RequestBody Map<String, String> payload) {
        String employeeId = authentication.getName();
        employeeService.updateProfile(employeeId, payload.get("phoneNumber"), payload.get("bankName"), payload.get("bankAccountNumber"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(Authentication authentication, @PathVariable String id) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ quản trị viên mới có quyền xóa nhân viên");
        }
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok().build();
    }
}
