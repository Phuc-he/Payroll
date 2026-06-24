package com.f2r.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate workDate;

    private String shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal mealAllowance;
    private BigDecimal casualWage;
    private BigDecimal prePaymentAmount;

    private String paymentStatus;

    @OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Timesheet> timesheets = new ArrayList<>();

    public void addTimesheet(Timesheet timesheet) {
        timesheets.add(timesheet);
        timesheet.setWorkSchedule(this);
    }
}
