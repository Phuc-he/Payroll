package com.f2r.payroll.service;

import com.f2r.payroll.dto.AdvancePaymentDetail;
import com.f2r.payroll.dto.DailyWorkDetail;
import com.f2r.payroll.dto.EmployeeOverviewItem;
import com.f2r.payroll.dto.MonthlyOverviewResponse;
import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.entity.AdvancePayment;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.entity.Timesheet;
import com.f2r.payroll.entity.WorkSchedule;
import com.f2r.payroll.repository.AdvancePaymentRepository;
import com.f2r.payroll.repository.EmployeeRepository;
import com.f2r.payroll.repository.TimesheetRepository;
import com.f2r.payroll.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final TimesheetRepository timesheetRepository;
    private final AdvancePaymentRepository advancePaymentRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final WorkScheduleService workScheduleService;

    @Transactional(readOnly = true)
    public MonthlyPayrollResponse calculateMonthlyPayroll(String employeeId, int month, int year) {
        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        BigDecimal totalWage = timesheetRepository.sumWageByEmployeeAndMonth(employeeId, month, year);
        if (totalWage == null)
            totalWage = BigDecimal.ZERO;

        List<AdvancePayment> advances = advancePaymentRepository.findByEmployeeAndMonth(employeeId, month, year);
        BigDecimal totalAdvance = advances.stream()
                .map(AdvancePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal actualReceived = totalWage.subtract(totalAdvance);

        List<Timesheet> timesheets = timesheetRepository.findTimesheetsByEmployeeAndMonth(employeeId, month, year);
        List<DailyWorkDetail> workDetails = timesheets.stream()
                .map(t -> DailyWorkDetail.builder()
                        .workDate(t.getWorkSchedule().getWorkDate())
                        .shift(t.getWorkSchedule().getShift())
                        .wage(t.getWage())
                        .build())
                .collect(Collectors.toList());

        List<AdvancePaymentDetail> advanceDetails = advances.stream()
                .map(a -> AdvancePaymentDetail.builder()
                        .id(a.getId())
                        .amount(a.getAmount())
                        .advanceDate(a.getAdvanceDate())
                        .notes(a.getNotes())
                        .build())
                .collect(Collectors.toList());

        // Tự động chia đôi tiền cắt cho phucnd và quantv
        if ("phucnd".equals(employeeId) || "quantv".equals(employeeId)) {
            List<WorkSchedule> allSchedules = workScheduleRepository.findByMonthAndYear(month, year);
            BigDecimal totalHalfTienCat = BigDecimal.ZERO;
            
            for (WorkSchedule ws : allSchedules) {
                WorkScheduleSummaryResponse summary = workScheduleService.mapToSummaryResponse(ws);
                if (summary.getTienCat().compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal halfTienCat = summary.getTienCat().divide(new BigDecimal(2), java.math.RoundingMode.HALF_UP);
                    totalHalfTienCat = totalHalfTienCat.add(halfTienCat);
                }
            }
            
            if (totalHalfTienCat.compareTo(BigDecimal.ZERO) > 0) {
                totalWage = totalWage.add(totalHalfTienCat);
                actualReceived = actualReceived.add(totalHalfTienCat);
                
                workDetails.add(DailyWorkDetail.builder()
                        .workDate(LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)) // Cuối tháng
                        .shift("TỔNG TIỀN CẮT THÁNG")
                        .wage(totalHalfTienCat)
                        .build());
            }
            
            // Sắp xếp lại theo ngày
            workDetails.sort(java.util.Comparator.comparing(DailyWorkDetail::getWorkDate));
        }

        return MonthlyPayrollResponse.builder()
                .employeeId(employeeId)
                .month(month)
                .year(year)
                .totalWage(totalWage)
                .totalAdvance(totalAdvance)
                .actualReceived(actualReceived)
                .workDetails(workDetails)
                .advanceDetails(advanceDetails)
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyOverviewResponse getMonthlyOverview(int month, int year) {
        List<WorkSchedule> schedules = workScheduleRepository.findByMonthAndYear(month, year);
        List<WorkScheduleSummaryResponse> mappedSchedules = schedules.stream()
                .map(workScheduleService::mapToSummaryResponse)
                .collect(Collectors.toList());

        BigDecimal tongTienDam = mappedSchedules.stream().map(WorkScheduleSummaryResponse::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongTienTraNhanVien = mappedSchedules.stream().map(WorkScheduleSummaryResponse::getLuongNhanVien).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tongTienCat = mappedSchedules.stream().map(WorkScheduleSummaryResponse::getTienCat).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Tổng lương nhân viên thuê ngoài
        BigDecimal tongLuongThueNgoai = mappedSchedules.stream()
                .map(s -> s.getCasualWage() != null ? s.getCasualWage() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Công nợ = Toàn bộ doanh thu của các đám CHƯA NHẬN (không trừ tiền ứng)
        BigDecimal congNo = mappedSchedules.stream()
                .filter(s -> "CHƯA NHẬN".equals(s.getPaymentStatus()))
                .map(WorkScheduleSummaryResponse::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Số tiền đã nhận = Tổng doanh thu - Công nợ
        BigDecimal soTienDaNhan = tongTienDam.subtract(congNo);

        // Tiền thực tế đã trả/ứng cho nhân viên cố định (từ AdvancePayment)
        BigDecimal tongDaTraNhanVien = advancePaymentRepository.sumTotalAdvanceByMonth(month, year);
        if (tongDaTraNhanVien == null) tongDaTraNhanVien = BigDecimal.ZERO;

        // Các khoản chi phí THỰC TẾ = Lương Thuê Ngoài (trừ luôn) + Tiền ĐÃ ỨNG/TRẢ cho NV cố định
        BigDecimal tongChi = tongLuongThueNgoai.add(tongDaTraNhanVien);

        // Tồn cuối kì = Tiền đã nhận - Tổng chi thực tế
        BigDecimal tonCuoiKi = soTienDaNhan.subtract(tongChi);

        return MonthlyOverviewResponse.builder()
                .month(month)
                .year(year)
                .schedules(mappedSchedules)
                .tongTienDam(tongTienDam)
                .tongTienTraNhanVien(tongTienTraNhanVien)
                .tongTienCat(tongTienCat)
                .tongLuongThueNgoai(tongLuongThueNgoai)
                .congNo(congNo)
                .soTienDaNhan(soTienDaNhan)
                .tongDaTraNhanVien(tongDaTraNhanVien)
                .tonCuoiKi(tonCuoiKi)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EmployeeOverviewItem> getAllEmployeeOverviews(int month, int year) {
        List<Employee> allEmployees = employeeRepository.findAll();
        return allEmployees.stream().map(emp -> {
            MonthlyPayrollResponse stats = calculateMonthlyPayroll(emp.getId(), month, year);
            return EmployeeOverviewItem.builder()
                    .id(emp.getId())
                    .fullName(emp.getFullName())
                    .totalWage(stats.getTotalWage())
                    .totalAdvance(stats.getTotalAdvance())
                    .actualReceived(stats.getActualReceived())
                    .build();
        }).collect(Collectors.toList());
    }
}
