package com.f2r.payroll.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvancePaymentRequest {
    private String employeeId;
    private BigDecimal amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate advanceDate;
    
    private String notes;
}
