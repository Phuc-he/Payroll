package com.f2r.payroll.service;

import com.f2r.payroll.dto.AdvancePaymentRequest;
import com.f2r.payroll.entity.AdvancePayment;

public interface AdvancePaymentService {
    AdvancePayment createAdvancePayment(AdvancePaymentRequest request);
}
