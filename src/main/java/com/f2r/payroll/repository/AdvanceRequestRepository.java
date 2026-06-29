package com.f2r.payroll.repository;

import com.f2r.payroll.entity.AdvanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvanceRequestRepository extends JpaRepository<AdvanceRequest, Long> {
    List<AdvanceRequest> findByEmployeeIdOrderByRequestDateDesc(String employeeId);
    List<AdvanceRequest> findAllByOrderByRequestDateDesc();
}
