package com.f2r.payroll.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmployeeTimesheetRequest {
    private String employeeId;
    private BigDecimal wage;
}
