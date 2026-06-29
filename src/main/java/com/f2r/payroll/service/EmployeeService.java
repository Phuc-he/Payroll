package com.f2r.payroll.service;

import com.f2r.payroll.dto.EmployeeRequest;
import com.f2r.payroll.entity.Employee;
import java.util.List;

public interface EmployeeService {
    Employee createEmployee(EmployeeRequest request);
    List<Employee> getAllEmployees();
    void updateBankInfo(String employeeId, String bankName, String bankAccountNumber);
}
