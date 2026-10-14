package com.f2r.payroll.service.impl;

import com.f2r.payroll.dto.AdvancePaymentRequest;
import com.f2r.payroll.entity.AdvancePayment;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.AdvancePaymentRepository;
import com.f2r.payroll.repository.EmployeeRepository;
import com.f2r.payroll.service.AdvancePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdvancePaymentServiceImpl implements AdvancePaymentService {

    private final AdvancePaymentRepository advancePaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final com.f2r.payroll.service.ZaloNotificationService zaloNotificationService;

    @Override
    @Transactional
    public AdvancePayment createAdvancePayment(AdvancePaymentRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        
        AdvancePayment payment = AdvancePayment.builder()
                .employee(employee)
                .amount(request.getAmount())
                .advanceDate(request.getAdvanceDate())
                .notes(request.getNotes())
                .build();
                
        AdvancePayment saved = advancePaymentRepository.save(payment);

        // Tự động gửi thông báo qua Zalo cho nhân viên (tự động bỏ qua Admin)
        zaloNotificationService.sendPaymentNotification(employee, saved.getAmount(), saved.getNotes(), saved.getAdvanceDate());

        return saved;
    }

    @Override
    @Transactional
    public AdvancePayment updateAdvancePayment(Long id, AdvancePaymentRequest request) {
        AdvancePayment payment = advancePaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu ứng lương #" + id));

        if (request.getAmount() != null) {
            payment.setAmount(request.getAmount());
        }
        if (request.getAdvanceDate() != null) {
            payment.setAdvanceDate(request.getAdvanceDate());
        }
        payment.setNotes(request.getNotes());

        if (request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + request.getEmployeeId()));
            payment.setEmployee(employee);
        }

        return advancePaymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void deleteAdvancePayment(Long id) {
        if (!advancePaymentRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy phiếu ứng lương #" + id);
        }
        advancePaymentRepository.deleteById(id);
    }
}
