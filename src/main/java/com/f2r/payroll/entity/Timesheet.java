package com.f2r.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "timesheet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timesheet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_schedule_id")
    private WorkSchedule workSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private BigDecimal wage;
}
