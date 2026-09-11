package com.civicresolve.service;

import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Default Administrator
        if (!userRepository.existsByEmail("admin@civicresolve.gov")) {
            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .email("admin@civicresolve.gov")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .department("Central Grievance Commission")
                    .badgeNumber("ADM-HQ-001")
                    .phone("+91 11 2309 2001")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info(">>> Seeded default Administrator: admin@civicresolve.gov / Admin@123");
        }

        // 2. Seed Default Government Officer
        if (!userRepository.existsByEmail("officer.verma@civicresolve.gov")) {
            User officer = User.builder()
                    .firstName("Er. Ramesh")
                    .lastName("Verma")
                    .email("officer.verma@civicresolve.gov")
                    .password(passwordEncoder.encode("Officer@123"))
                    .role(Role.ROLE_OFFICER)
                    .department("Roads & Transport")
                    .badgeNumber("OFF-PWD-2564")
                    .phone("+91 94150 25640")
                    .enabled(true)
                    .build();
            userRepository.save(officer);
            log.info(">>> Seeded default Government Officer: officer.verma@civicresolve.gov / Officer@123");
        }

        // 3. Seed Default Citizen
        if (!userRepository.existsByEmail("citizen@civicresolve.in")) {
            User citizen = User.builder()
                    .firstName("Aarav")
                    .lastName("Sharma")
                    .email("citizen@civicresolve.in")
                    .password(passwordEncoder.encode("Citizen@123"))
                    .role(Role.ROLE_CITIZEN)
                    .phone("+91 98765 43210")
                    .enabled(true)
                    .build();
            userRepository.save(citizen);
            log.info(">>> Seeded default Citizen: citizen@civicresolve.in / Citizen@123");
        }
    }
}
