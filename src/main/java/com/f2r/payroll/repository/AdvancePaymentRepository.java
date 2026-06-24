package com.f2r.payroll.repository;

import com.f2r.payroll.entity.AdvancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {
    @Query("SELECT SUM(a.amount) FROM AdvancePayment a WHERE a.employee.id = :employeeId AND YEAR(a.advanceDate) = :year AND MONTH(a.advanceDate) = :month")
    BigDecimal sumAmountByEmployeeAndMonth(@Param("employeeId") String employeeId, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(a.amount) FROM AdvancePayment a WHERE YEAR(a.advanceDate) = :year AND MONTH(a.advanceDate) = :month")
    BigDecimal sumTotalAdvanceByMonth(@Param("month") int month, @Param("year") int year);

    @Query("SELECT a FROM AdvancePayment a WHERE a.employee.id = :employeeId AND YEAR(a.advanceDate) = :year AND MONTH(a.advanceDate) = :month ORDER BY a.advanceDate ASC")
    java.util.List<AdvancePayment> findByEmployeeAndMonth(@Param("employeeId") String employeeId, @Param("month") int month, @Param("year") int year);
}
