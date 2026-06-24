package com.f2r.payroll.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    private String id;

    private String fullName;
    private String phoneNumber;
    private Boolean isActive;
}
