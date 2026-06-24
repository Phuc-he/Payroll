package com.f2r.payroll.repository;

import com.f2r.payroll.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    @Query("SELECT SUM(t.wage) FROM Timesheet t WHERE t.employee.id = :employeeId AND YEAR(t.workSchedule.workDate) = :year AND MONTH(t.workSchedule.workDate) = :month")
    BigDecimal sumWageByEmployeeAndMonth(@Param("employeeId") String employeeId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT t FROM Timesheet t JOIN FETCH t.workSchedule WHERE t.employee.id = :employeeId AND YEAR(t.workSchedule.workDate) = :year AND MONTH(t.workSchedule.workDate) = :month ORDER BY t.workSchedule.workDate ASC")
    List<Timesheet> findTimesheetsByEmployeeAndMonth(@Param("employeeId") String employeeId, @Param("month") int month, @Param("year") int year);
}
