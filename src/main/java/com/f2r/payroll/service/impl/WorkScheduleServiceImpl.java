package com.f2r.payroll.service.impl;

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
import com.f2r.payroll.service.WorkScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkScheduleServiceImpl implements WorkScheduleService {

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
            java.util.Set<String> empIds = new java.util.HashSet<>();
            for (EmployeeTimesheetRequest empReq : request.getEmployees()) {
                if (!empIds.add(empReq.getEmployeeId())) {
                    throw new IllegalArgumentException("Duplicate employee in work schedule: " + empReq.getEmployeeId());
                }
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
            java.util.Set<String> empIds = new java.util.HashSet<>();
            for (EmployeeTimesheetRequest empReq : request.getEmployees()) {
                if (!empIds.add(empReq.getEmployeeId())) {
                    throw new IllegalArgumentException("Duplicate employee in work schedule: " + empReq.getEmployeeId());
                }
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUsername = auth != null ? auth.getName() : null;

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
                        .wage(isAdmin || (currentUsername != null && currentUsername.equals(t.getEmployee().getId())) ? t.getWage() : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        return WorkScheduleSummaryResponse.builder()
                .id(ws.getId())
                .workDate(ws.getWorkDate())
                .shift(ws.getShift())
                .locationName(ws.getLocation() != null ? ws.getLocation().getName() : "")
                .unitPrice(isAdmin ? ws.getUnitPrice() : BigDecimal.ZERO)
                .quantity(ws.getQuantity())
                .mealAllowance(isAdmin ? mealAllowance : BigDecimal.ZERO)
                .casualWage(isAdmin ? casualWage : BigDecimal.ZERO)
                .casualWorkerCount(Math.max(0, ws.getQuantity() - employees.size()))
                .paymentStatus(isAdmin ? ws.getPaymentStatus() : "")
                .thanhTien(isAdmin ? thanhTien : BigDecimal.ZERO)
                .luongNhanVien(isAdmin ? luongNhanVien : BigDecimal.ZERO)
                .tienCat(isAdmin ? tienCat : BigDecimal.ZERO)
                .employees(employees)
                .build();
    }
}
