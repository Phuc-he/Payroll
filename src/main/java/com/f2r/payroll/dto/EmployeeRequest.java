package com.f2r.payroll.dto;

import lombok.Data;

@Data
public class EmployeeRequest {
    private String id;
    private String fullName;
    private String phoneNumber;
    private Boolean isActive;
    private String bankName;
    private String bankAccountNumber;
}
