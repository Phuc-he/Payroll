package com.f2r.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "advance_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvancePayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private BigDecimal amount;
    private LocalDate advanceDate;
    private String notes;
}
