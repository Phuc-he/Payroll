package com.f2r.payroll.service;

import com.f2r.payroll.dto.EmployeeTimesheetRequest;
import com.f2r.payroll.dto.WorkScheduleRequest;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.entity.Location;
import com.f2r.payroll.entity.Timesheet;
import com.f2r.payroll.entity.WorkSchedule;
import com.f2r.payroll.repository.EmployeeRepository;
import com.f2r.payroll.repository.LocationRepository;
import com.f2r.payroll.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final LocationRepository locationRepository;
    private final EmployeeRepository employeeRepository;

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

    @Transactional(readOnly = true)
    public WorkScheduleSummaryResponse getWorkScheduleSummary(Long scheduleId) {
        WorkSchedule ws = workScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("WorkSchedule not found"));
        return mapToSummaryResponse(ws);
    }

    public WorkScheduleSummaryResponse mapToSummaryResponse(WorkSchedule ws) {
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
}
