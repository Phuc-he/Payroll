package com.f2r.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EmployeeOverviewItem {
    private String id;
    private String fullName;
    private BigDecimal totalWage;
    private BigDecimal totalAdvance;
    private BigDecimal actualReceived;
}
