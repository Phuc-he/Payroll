package com.f2r.payroll.service;

import com.f2r.payroll.dto.EmployeeOverviewItem;
import com.f2r.payroll.dto.MonthlyOverviewResponse;
import com.f2r.payroll.dto.MonthlyPayrollResponse;

import java.util.List;

public interface ReportService {
    MonthlyPayrollResponse calculateMonthlyPayroll(String employeeId, int month, int year);
    MonthlyOverviewResponse getMonthlyOverview(int month, int year);
    List<EmployeeOverviewItem> getAllEmployeeOverviews(int month, int year);
}
