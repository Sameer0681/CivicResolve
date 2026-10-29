package com.civicresolve.repository;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.Escalation;
import com.civicresolve.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

    @EntityGraph(attributePaths = {"complaint", "complaint.citizen", "assignedOfficer", "escalatedBy"})
    List<Escalation> findByComplaint(Complaint complaint);

    @EntityGraph(attributePaths = {"complaint", "complaint.citizen", "assignedOfficer", "escalatedBy"})
    List<Escalation> findByComplaint_Id(Long complaintId);

    List<Escalation> findByStatus(String status);

    List<Escalation> findByEscalationLevel(Integer escalationLevel);

    @EntityGraph(attributePaths = {"complaint", "complaint.citizen", "assignedOfficer", "escalatedBy"})
    List<Escalation> findByAssignedOfficer(User assignedOfficer);

    @EntityGraph(attributePaths = {"complaint", "complaint.citizen", "assignedOfficer", "escalatedBy"})
    List<Escalation> findByAssignedOfficer_IdOrderByCreatedAtDesc(Long officerId);

    @EntityGraph(attributePaths = {"complaint", "complaint.citizen", "assignedOfficer", "escalatedBy"})
    List<Escalation> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    void deleteByComplaint_Id(Long complaintId);
}
