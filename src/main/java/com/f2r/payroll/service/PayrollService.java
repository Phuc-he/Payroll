package com.f2r.payroll.service;

import com.f2r.payroll.dto.EmployeeTimesheetRequest;
import com.f2r.payroll.dto.MonthlyPayrollResponse;
import com.f2r.payroll.dto.WorkScheduleRequest;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.dto.DailyWorkDetail;
import com.f2r.payroll.dto.AdvancePaymentDetail;
import com.f2r.payroll.entity.*;
import java.util.List;
import java.util.stream.Collectors;
import com.f2r.payroll.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PayrollService {

        private final WorkScheduleRepository workScheduleRepository;
        private final EmployeeRepository employeeRepository;
        private final LocationRepository locationRepository;
        private final TimesheetRepository timesheetRepository;
        private final AdvancePaymentRepository advancePaymentRepository;

        @Transactional
        public WorkSchedule createWorkSchedule(WorkScheduleRequest request) {
                Location location = locationRepository.findById(request.getLocationId())
                                .orElseThrow(() -> new IllegalArgumentException("Location not found"));

                WorkSchedule workSchedule = WorkSchedule.builder()
                                .workDate(request.getWorkDate())
                                .shift(request.getShift())
                                .location(location)
                                .unitPrice(request.getUnitPrice())
                                .quantity(request.getQuantity())
                                .mealAllowance(request.getMealAllowance())
                                .casualWage(request.getCasualWage())
                                .paymentStatus(request.getPaymentStatus())
                                .build();

                if (request.getEmployees() != null) {
                        for (EmployeeTimesheetRequest empReq : request.getEmployees()) {
                                Employee employee = employeeRepository.findById(empReq.getEmployeeId())
                                                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

                                Timesheet timesheet = Timesheet.builder()
                                                .employee(employee)
                                                .wage(empReq.getWage())
                                                .build();

                                workSchedule.addTimesheet(timesheet);
                        }
                }

                return workScheduleRepository.save(workSchedule);
        }

        @Transactional
        public void updatePaymentStatus(Long scheduleId, String newStatus) {
                WorkSchedule ws = workScheduleRepository.findById(scheduleId)
                                .orElseThrow(() -> new IllegalArgumentException("WorkSchedule not found"));
                ws.setPaymentStatus(newStatus);
                workScheduleRepository.save(ws);
        }

        @Transactional
        public void deleteWorkSchedule(Long scheduleId) {
                if (!workScheduleRepository.existsById(scheduleId)) {
                        throw new IllegalArgumentException("WorkSchedule not found");
                }
                workScheduleRepository.deleteById(scheduleId);
        }

        @Transactional
        public WorkSchedule updateWorkSchedule(Long scheduleId, WorkScheduleRequest request) {
                WorkSchedule workSchedule = workScheduleRepository.findById(scheduleId)
                                .orElseThrow(() -> new IllegalArgumentException("WorkSchedule not found"));

                Location location = locationRepository.findById(request.getLocationId())
                                .orElseThrow(() -> new IllegalArgumentException("Location not found"));

                workSchedule.setWorkDate(request.getWorkDate());
                workSchedule.setShift(request.getShift());
                workSchedule.setLocation(location);
                workSchedule.setUnitPrice(request.getUnitPrice());
                workSchedule.setQuantity(request.getQuantity());
                workSchedule.setMealAllowance(request.getMealAllowance());
                workSchedule.setCasualWage(request.getCasualWage());
                workSchedule.setPaymentStatus(request.getPaymentStatus());

                workSchedule.getTimesheets().clear();

                if (request.getEmployees() != null) {
                        for (EmployeeTimesheetRequest empReq : request.getEmployees()) {
                                Employee employee = employeeRepository.findById(empReq.getEmployeeId())
                                                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

                                Timesheet timesheet = Timesheet.builder()
                                                .employee(employee)
                                                .wage(empReq.getWage())
                                                .build();

                                workSchedule.addTimesheet(timesheet);
                        }
                }

                return workScheduleRepository.save(workSchedule);
        }

        @Transactional
        public AdvancePayment createAdvancePayment(com.f2r.payroll.dto.AdvancePaymentRequest request) {
                Employee employee = employeeRepository.findById(request.getEmployeeId())
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
                
                AdvancePayment payment = AdvancePayment.builder()
                                .employee(employee)
                                .amount(request.getAmount())
                                .advanceDate(request.getAdvanceDate())
                                .notes(request.getNotes())
                                .build();
                                
                return advancePaymentRepository.save(payment);
        }

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
                                WorkScheduleSummaryResponse summary = mapToSummaryResponse(ws);
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
        public WorkScheduleSummaryResponse getWorkScheduleSummary(Long scheduleId) {
                WorkSchedule ws = workScheduleRepository.findById(scheduleId)
                                .orElseThrow(() -> new IllegalArgumentException("WorkSchedule not found"));
                return mapToSummaryResponse(ws);
        }

        @Transactional(readOnly = true)
        public com.f2r.payroll.dto.MonthlyOverviewResponse getMonthlyOverview(int month, int year) {
                List<WorkSchedule> schedules = workScheduleRepository.findByMonthAndYear(month, year);
                List<WorkScheduleSummaryResponse> mappedSchedules = schedules.stream()
                        .map(this::mapToSummaryResponse)
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

                return com.f2r.payroll.dto.MonthlyOverviewResponse.builder()
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

        private WorkScheduleSummaryResponse mapToSummaryResponse(WorkSchedule ws) {
                BigDecimal thanhTien = ws.getUnitPrice().multiply(BigDecimal.valueOf(ws.getQuantity()));

                BigDecimal luongNhanVien = ws.getTimesheets().stream()
                                .map(Timesheet::getWage)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal mealAllowance = ws.getMealAllowance() != null ? ws.getMealAllowance() : BigDecimal.ZERO;
                BigDecimal casualWage = ws.getCasualWage() != null ? ws.getCasualWage() : 
                                       (ws.getPrePaymentAmount() != null ? ws.getPrePaymentAmount() : BigDecimal.ZERO);
                
                // Tiền cắt = Doanh thu - Lương NV (cố định) - Lương NV (thuê ngoài) - Tiền ăn
                BigDecimal tienCat = thanhTien.subtract(luongNhanVien).subtract(casualWage).subtract(mealAllowance);

                List<com.f2r.payroll.dto.EmployeeShiftDetail> employees = ws.getTimesheets().stream()
                                .map(t -> com.f2r.payroll.dto.EmployeeShiftDetail.builder()
                                        .employeeId(t.getEmployee().getId())
                                        .fullName(t.getEmployee().getFullName())
                                        .wage(t.getWage())
                                        .build())
                                .collect(Collectors.toList());

                return WorkScheduleSummaryResponse.builder()
                                .id(ws.getId())
                                .workDate(ws.getWorkDate())
                                .shift(ws.getShift())
                                .locationName(ws.getLocation() != null ? ws.getLocation().getName() : "")
                                .unitPrice(ws.getUnitPrice())
                                .quantity(ws.getQuantity())
                                .mealAllowance(mealAllowance)
                                .casualWage(casualWage)
                                .casualWorkerCount(Math.max(0, ws.getQuantity() - employees.size()))
                                .paymentStatus(ws.getPaymentStatus())
                                .thanhTien(thanhTien)
                                .luongNhanVien(luongNhanVien)
                                .tienCat(tienCat)
                                .employees(employees)
                                .build();
        }
        @Transactional(readOnly = true)
        public List<com.f2r.payroll.dto.EmployeeOverviewItem> getAllEmployeeOverviews(int month, int year) {
                List<Employee> allEmployees = employeeRepository.findAll();
                return allEmployees.stream().map(emp -> {
                        MonthlyPayrollResponse stats = calculateMonthlyPayroll(emp.getId(), month, year);
                        return com.f2r.payroll.dto.EmployeeOverviewItem.builder()
                                .id(emp.getId())
                                .fullName(emp.getFullName())
                                .totalWage(stats.getTotalWage())
                                .totalAdvance(stats.getTotalAdvance())
                                .actualReceived(stats.getActualReceived())
                                .build();
                }).collect(Collectors.toList());
        }

        @Transactional
        public Employee createEmployee(com.f2r.payroll.dto.EmployeeRequest request) {
                Employee employee = Employee.builder()
                                .id(request.getId())
                                .fullName(request.getFullName())
                                .phoneNumber(request.getPhoneNumber())
                                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                                .build();
                return employeeRepository.save(employee);
        }

        @Transactional(readOnly = true)
        public List<Location> getAllLocations() {
                return locationRepository.findAll();
        }

        @Transactional(readOnly = true)
        public List<Employee> getAllEmployees() {
                return employeeRepository.findAll();
        }
}
