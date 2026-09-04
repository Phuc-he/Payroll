package com.f2r.payroll.repository;

import com.f2r.payroll.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findByIsDeletedFalseOrIsDeletedIsNull();
}
