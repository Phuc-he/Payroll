package com.f2r.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPayrollResponse {
    private String employeeId;
    private String fullName;
    private String phoneNumber;
    private int month;
    private int year;
    private BigDecimal totalWage;
    private BigDecimal totalAdvance;
    private BigDecimal actualReceived;
    private String bankName;
    private String bankAccountNumber;
    private List<DailyWorkDetail> workDetails;
    private List<AdvancePaymentDetail> advanceDetails;
}
