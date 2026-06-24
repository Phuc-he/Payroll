package com.f2r.payroll.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlyOverviewResponse {
    private int month;
    private int year;
    private BigDecimal tongTienDam;
    private BigDecimal tongTienTraNhanVien;
    private BigDecimal tongTienCat;
    private BigDecimal tongLuongThueNgoai;
    
    // Thêm các trường mới theo yêu cầu
    private BigDecimal congNo;
    private BigDecimal soTienDaNhan;
    private BigDecimal tongDaTraNhanVien;
    private BigDecimal tonCuoiKi;
    
    private List<WorkScheduleSummaryResponse> schedules;
}
