package com.f2r.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AdvancePaymentDetail {
    private Long id;
    private BigDecimal amount;
    private LocalDate advanceDate;
    private String notes;
}
