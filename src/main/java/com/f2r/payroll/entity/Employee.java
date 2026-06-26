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
    
    private String password;
    private String role; // "ROLE_ADMIN" or "ROLE_USER"
    
    @Column(name = "is_first_login", columnDefinition = "boolean default true")
    private Boolean isFirstLogin;
}
