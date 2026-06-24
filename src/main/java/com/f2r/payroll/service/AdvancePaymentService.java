package com.f2r.payroll.service;

import com.f2r.payroll.dto.AdvancePaymentRequest;
import com.f2r.payroll.entity.AdvancePayment;
import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.AdvancePaymentRepository;
import com.f2r.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdvancePaymentService {

    private final AdvancePaymentRepository advancePaymentRepository;
    private final EmployeeRepository employeeRepository;

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
                
        return advancePaymentRepository.save(payment);
    }
}
