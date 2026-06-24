package com.f2r.payroll.dto;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class WorkScheduleSummaryResponse {
    private Long id;
    private LocalDate workDate;
    private String shift;
    private String locationName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal mealAllowance;
    private BigDecimal casualWage;
    private Integer casualWorkerCount;
    private String paymentStatus;

    // Computed fields
    private BigDecimal thanhTien;
    private BigDecimal luongNhanVien;
    private BigDecimal tienCat;
    
    private java.util.List<EmployeeShiftDetail> employees;
}
