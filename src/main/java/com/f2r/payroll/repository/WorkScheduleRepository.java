package com.f2r.payroll.repository;

import com.f2r.payroll.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    @Query("SELECT w FROM WorkSchedule w WHERE YEAR(w.workDate) = :year AND MONTH(w.workDate) = :month ORDER BY w.workDate ASC")
    List<WorkSchedule> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
