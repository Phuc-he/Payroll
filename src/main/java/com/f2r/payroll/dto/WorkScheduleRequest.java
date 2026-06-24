package com.f2r.payroll.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class WorkScheduleRequest {
    private LocalDate workDate;
    private String shift;
    private Long locationId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal mealAllowance;
    private BigDecimal casualWage;
    private String paymentStatus;
    private List<EmployeeTimesheetRequest> employees;
}
