package com.civicresolve.repository;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.Escalation;
import com.civicresolve.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    List<Escalation> findByComplaint(Complaint complaint);

    List<Escalation> findByStatus(String status);

    List<Escalation> findByEscalationLevel(Integer escalationLevel);

    List<Escalation> findByAssignedOfficer(User assignedOfficer);

    long countByStatus(String status);
}
