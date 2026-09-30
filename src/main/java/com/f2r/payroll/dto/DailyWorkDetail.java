package com.f2r.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyWorkDetail {
    private Long scheduleId;
    private LocalDate workDate;
    private String shift;
    private BigDecimal wage;
}
