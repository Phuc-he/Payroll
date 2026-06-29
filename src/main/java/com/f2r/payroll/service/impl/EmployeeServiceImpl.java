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
            employee.setPassword("$2a$10$WkL2I5/qE2Mv3K62U1Z5sueF4J2D/yE8.O.Jv.3K5.0G9T.L/2M6a"); // 123456
            employee.setRole("ROLE_USER");
            employee.setIsFirstLogin(true);
        }

        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    @Transactional
    public void updateBankInfo(String employeeId, String bankName, String bankAccountNumber) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        employee.setBankName(bankName);
        employee.setBankAccountNumber(bankAccountNumber);
        employeeRepository.save(employee);
    }
}
