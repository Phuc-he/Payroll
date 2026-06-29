package com.f2r.payroll.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.f2r.payroll.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payroll/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        Map<String, Object> userDetails = new HashMap<>();
        if (authentication != null && authentication.isAuthenticated()) {
            userDetails.put("username", authentication.getName());
            userDetails.put("roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
                    
            com.f2r.payroll.entity.Employee emp = employeeRepository.findById(authentication.getName()).orElse(null);
            if (emp != null) {
                userDetails.put("isFirstLogin", emp.getIsFirstLogin());
                userDetails.put("bankName", emp.getBankName());
                userDetails.put("bankAccountNumber", emp.getBankAccountNumber());
            }
        } else {
            userDetails.put("error", "Not authenticated");
        }
        return userDetails;
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(Authentication authentication, @RequestBody Map<String, String> payload) {
        if (authentication == null || !authentication.isAuthenticated() || "admin".equals(authentication.getName())) {
            throw new RuntimeException("Unauthorized");
        }
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
        
        com.f2r.payroll.entity.Employee emp = employeeRepository.findById(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        emp.setPassword(passwordEncoder.encode(newPassword));
        emp.setIsFirstLogin(false);
        employeeRepository.save(emp);
        
        Map<String, String> res = new HashMap<>();
        res.put("message", "Password changed successfully");
        return res;
    }
}
