package com.f2r.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EmployeeShiftDetail {
    private String employeeId;
    private String fullName;
    private BigDecimal wage;
}
