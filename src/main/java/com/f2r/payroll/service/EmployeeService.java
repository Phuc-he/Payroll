package com.f2r.payroll.service;

import com.f2r.payroll.dto.EmployeeRequest;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public Employee createEmployee(EmployeeRequest request) {
        Employee employee = Employee.builder()
                .id(request.getId())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }
}
