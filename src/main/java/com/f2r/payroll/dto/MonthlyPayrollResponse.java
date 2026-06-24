package com.f2r.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MonthlyPayrollResponse {
    private String employeeId;
    private int month;
    private int year;
    private BigDecimal totalWage;
    private BigDecimal totalAdvance;
    private BigDecimal actualReceived;
    private List<DailyWorkDetail> workDetails;
    private List<AdvancePaymentDetail> advanceDetails;
}
