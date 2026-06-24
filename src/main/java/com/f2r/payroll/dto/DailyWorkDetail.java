package com.f2r.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailyWorkDetail {
    private LocalDate workDate;
    private String shift;
    private BigDecimal wage;
}
