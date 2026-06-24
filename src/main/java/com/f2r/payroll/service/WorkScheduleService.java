package com.f2r.payroll.service;

import com.f2r.payroll.dto.WorkScheduleRequest;
import com.f2r.payroll.dto.WorkScheduleSummaryResponse;
import com.f2r.payroll.entity.WorkSchedule;

public interface WorkScheduleService {
    WorkSchedule createWorkSchedule(WorkScheduleRequest request);
    void updatePaymentStatus(Long scheduleId, String newStatus);
    void deleteWorkSchedule(Long scheduleId);
    WorkSchedule updateWorkSchedule(Long scheduleId, WorkScheduleRequest request);
    WorkScheduleSummaryResponse getWorkScheduleSummary(Long scheduleId);
    WorkScheduleSummaryResponse mapToSummaryResponse(WorkSchedule ws);
}
