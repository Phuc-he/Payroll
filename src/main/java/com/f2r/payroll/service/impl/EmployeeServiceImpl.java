package com.f2r.payroll.service.impl;

import com.f2r.payroll.dto.EmployeeRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.EmployeeRepository;
import com.f2r.payroll.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = employeeRepository.findById(request.getId()).orElse(new Employee());
        employee.setId(request.getId());
        employee.setFullName(request.getFullName());
        employee.setPhoneNumber(request.getPhoneNumber());
        employee.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        if (request.getBankName() != null) employee.setBankName(request.getBankName());
        if (request.getBankAccountNumber() != null) employee.setBankAccountNumber(request.getBankAccountNumber());

        // Default credentials for new employee
        if (employee.getPassword() == null) {
            employee.setPassword(passwordEncoder.encode("123456"));
            employee.setRole("ROLE_USER");
            employee.setIsFirstLogin(true);
        }

        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findByIsDeletedFalseOrIsDeletedIsNull();
    }

    @Override
    @Transactional
    public void updateBankInfo(String employeeId, String bankName, String bankAccountNumber) {
        updateProfile(employeeId, null, bankName, bankAccountNumber);
    }

    @Override
    @Transactional
    public void updateProfile(String employeeId, String phoneNumber, String bankName, String bankAccountNumber) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        if (phoneNumber != null) {
            employee.setPhoneNumber(phoneNumber.trim().isEmpty() ? null : phoneNumber.trim());
        }
        if (bankName != null) {
            employee.setBankName(bankName.trim().isEmpty() ? null : bankName.trim());
        }
        if (bankAccountNumber != null) {
            employee.setBankAccountNumber(bankAccountNumber.trim().isEmpty() ? null : bankAccountNumber.trim());
        }
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void deleteEmployee(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        employee.setIsDeleted(true);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public Employee updateEmployee(String id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + id));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            employee.setFullName(request.getFullName().trim());
        }
        if (request.getPhoneNumber() != null) {
            employee.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getBankName() != null) {
            employee.setBankName(request.getBankName().trim());
        }
        if (request.getBankAccountNumber() != null) {
            employee.setBankAccountNumber(request.getBankAccountNumber().trim());
        }
        if (request.getIsActive() != null) {
            employee.setIsActive(request.getIsActive());
        }

        return employeeRepository.save(employee);
    }

    @Override
    @Transactional
    public void resetPassword(String employeeId) {
        if ("admin".equalsIgnoreCase(employeeId)) {
            throw new IllegalArgumentException("Không thể reset mật khẩu của tài khoản Admin hệ thống!");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + employeeId));

        employee.setPassword(passwordEncoder.encode("123456"));
        employee.setIsFirstLogin(true);
        employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên: " + id));
    }
}
