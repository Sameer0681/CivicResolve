package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ComplaintRepository complaintRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
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

        // 4. Additive Field Officers: Sanitation & Water
        if (!userRepository.existsByEmail("officer.sanitation@civicresolve.gov")) {
            User officer = User.builder()
                    .firstName("Sunil")
                    .lastName("Yadav")
                    .email("officer.sanitation@civicresolve.gov")
                    .password(passwordEncoder.encode("Officer@123"))
                    .role(Role.ROLE_OFFICER)
                    .department("Municipal Solid Waste Management")
                    .badgeNumber("OFF-SWM-202")
                    .phone("+91 94150 25641")
                    .enabled(true)
                    .build();
            userRepository.save(officer);
            log.info(">>> Seeded Sanitation Field Officer: officer.sanitation@civicresolve.gov / Officer@123");
        }

        if (!userRepository.existsByEmail("officer.water@civicresolve.gov")) {
            User officer = User.builder()
                    .firstName("Anita")
                    .lastName("Deshmukh")
                    .email("officer.water@civicresolve.gov")
                    .password(passwordEncoder.encode("Officer@123"))
                    .role(Role.ROLE_OFFICER)
                    .department("Water Supply & Drainage")
                    .badgeNumber("OFF-WTR-303")
                    .phone("+91 94150 25642")
                    .enabled(true)
                    .build();
            userRepository.save(officer);
            log.info(">>> Seeded Water Supply Field Officer: officer.water@civicresolve.gov / Officer@123");
        }

        // 5. Additive Citizen: Priya Sharma
        if (!userRepository.existsByEmail("priya.sharma@civicresolve.in")) {
            User citizen = User.builder()
                    .firstName("Priya")
                    .lastName("Sharma")
                    .email("priya.sharma@civicresolve.in")
                    .password(passwordEncoder.encode("Citizen@123"))
                    .role(Role.ROLE_CITIZEN)
                    .phone("+91 98765 43211")
                    .enabled(true)
                    .build();
            userRepository.save(citizen);
            log.info(">>> Seeded secondary Citizen: priya.sharma@civicresolve.in / Citizen@123");
        }

        // 6. Additive Sample Complaints (CR-2026-0001 to 0005) - strictly idempotent, NO Escalation rows seeded
        User citizen1 = userRepository.findByEmail("citizen@civicresolve.in").orElse(null);
        User citizen2 = userRepository.findByEmail("priya.sharma@civicresolve.in").orElse(null);
        User officerVerma = userRepository.findByEmail("officer.verma@civicresolve.gov").orElse(null);
        User officerSanitation = userRepository.findByEmail("officer.sanitation@civicresolve.gov").orElse(null);
        User officerWater = userRepository.findByEmail("officer.water@civicresolve.gov").orElse(null);

        if (citizen1 != null && officerVerma != null && !complaintRepository.existsByComplaintId("CR-2026-0001")) {
            Complaint c1 = Complaint.builder()
                    .complaintId("CR-2026-0001")
                    .title("Severe Pothole on MG Road near Metro Station")
                    .description("Deep crater-like pothole creating road safety hazard and causing severe vehicle congestion.")
                    .sector("Roads & Transport")
                    .department("Roads & Transport")
                    .location("Ward 14, MG Road near Metro Station [26.8467, 80.9462]")
                    .status(ComplaintStatus.IN_PROGRESS)
                    .urgency("HIGH")
                    .priority("HIGH")
                    .citizen(citizen1)
                    .assignedOfficer(officerVerma)
                    .officerNotes("Field team dispatched for asphalt patch work. Scheduled completion within SLA.")
                    .latitude(26.8467)
                    .longitude(80.9462)
                    .build();
            complaintRepository.save(c1);
        }

        if (citizen1 != null && officerVerma != null && !complaintRepository.existsByComplaintId("CR-2026-0002")) {
            Complaint c2 = Complaint.builder()
                    .complaintId("CR-2026-0002")
                    .title("Street light outage along Cubbon Park residential stretch")
                    .description("Three consecutive streetlight fixtures malfunctioning for over 5 days.")
                    .sector("Street Lighting")
                    .department("Municipal Electrical & Lighting Cell")
                    .location("North Zone Electrical Ward 4 [26.8512, 80.9410]")
                    .status(ComplaintStatus.RESOLVED)
                    .urgency("MEDIUM")
                    .priority("MEDIUM")
                    .citizen(citizen1)
                    .assignedOfficer(officerVerma)
                    .officerNotes("Replaced blown 90W LED drivers and tested junction circuit. Issue fully resolved.")
                    .proofUrl("https://civicresolve.s3.amazonaws.com/proofs/CR-2026-0002-resolved.jpg")
                    .latitude(26.8512)
                    .longitude(80.9410)
                    .resolvedAt(LocalDateTime.now().minusHours(4))
                    .build();
            complaintRepository.save(c2);
        }

        if (citizen2 != null && !complaintRepository.existsByComplaintId("CR-2026-0003")) {
            Complaint c3 = Complaint.builder()
                    .complaintId("CR-2026-0003")
                    .title("Uncollected municipal waste near commercial market")
                    .description("Garbage overflow at secondary collection point causing sanitary concerns.")
                    .sector("Sanitation & Waste")
                    .department("Municipal Solid Waste Management")
                    .location("Central Market Ward 8 [26.8420, 80.9501]")
                    .status(ComplaintStatus.SUBMITTED)
                    .urgency("MEDIUM")
                    .priority("MEDIUM")
                    .citizen(citizen2)
                    .latitude(26.8420)
                    .longitude(80.9501)
                    .build();
            complaintRepository.save(c3);
        }

        if (citizen1 != null && officerWater != null && !complaintRepository.existsByComplaintId("CR-2026-0004")) {
            Complaint c4 = Complaint.builder()
                    .complaintId("CR-2026-0004")
                    .title("Main water distribution pipeline rupture flooding street")
                    .description("High-pressure water leakage flooding main road and contaminating local distribution line.")
                    .sector("Water Supply")
                    .department("Water Supply & Drainage")
                    .location("Sub-division 3, Water Works Junction [26.8390, 80.9320]")
                    .status(ComplaintStatus.ESCALATED)
                    .urgency("CRITICAL")
                    .priority("CRITICAL")
                    .citizen(citizen1)
                    .assignedOfficer(officerWater)
                    .officerNotes("48-Hour SLA window breached. Escalated to Executive Engineer for heavy machinery repair.")
                    .latitude(26.8390)
                    .longitude(80.9320)
                    .build();
            complaintRepository.save(c4);
        }

        if (citizen2 != null && officerSanitation != null && !complaintRepository.existsByComplaintId("CR-2026-0005")) {
            Complaint c5 = Complaint.builder()
                    .complaintId("CR-2026-0005")
                    .title("Routine drainage clearing completed and inspected")
                    .description("Seasonal clearing of storm water drains on 5th cross road completed.")
                    .sector("Sanitation & Waste")
                    .department("Municipal Solid Waste Management")
                    .location("5th Cross, Sector 2 [26.8435, 80.9480]")
                    .status(ComplaintStatus.RESOLVED)
                    .urgency("LOW")
                    .priority("LOW")
                    .citizen(citizen2)
                    .assignedOfficer(officerSanitation)
                    .officerNotes("Inspected by ward supervisor, verified clean.")
                    .latitude(26.8435)
                    .longitude(80.9480)
                    .resolvedAt(LocalDateTime.now().minusDays(2))
                    .build();
            complaintRepository.save(c5);
        }
    }
}
