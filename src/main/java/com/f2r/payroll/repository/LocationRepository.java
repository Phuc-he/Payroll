package com.f2r.payroll.repository;

import com.f2r.payroll.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}
