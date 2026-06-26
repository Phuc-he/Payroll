package com.f2r.payroll.security;

import com.f2r.payroll.entity.Employee;
import com.f2r.payroll.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

import org.springframework.security.crypto.password.PasswordEncoder;
// ...
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("admin".equals(username)) {
            return new User("admin", passwordEncoder.encode("Anhday99"), Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        Employee employee = employeeRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = employee.getRole() != null ? employee.getRole() : "ROLE_USER";
        
        if (employee.getPassword() == null) {
            employee.setPassword(passwordEncoder.encode("123456"));
            employee.setIsFirstLogin(true);
            employeeRepository.save(employee);
        }

        return new User(employee.getId(), employee.getPassword(), Collections.singletonList(new SimpleGrantedAuthority(role)));
    }
}
