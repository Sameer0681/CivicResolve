package com.civicresolve.repository;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintId(String complaintId);

    Optional<Complaint> findTopByComplaintIdStartingWithOrderByComplaintIdDesc(String prefix);

    Optional<Complaint> findByIdAndCitizen_Id(Long id, Long citizenId);

    Optional<Complaint> findByIdAndAssignedOfficer_Id(Long id, Long officerId);

    List<Complaint> findByCitizen(User citizen);

    List<Complaint> findByCitizenOrderByCreatedAtDesc(User citizen);

    List<Complaint> findByCitizen_IdOrderByCreatedAtDesc(Long citizenId);

    List<Complaint> findByAssignedOfficer(User assignedOfficer);

    List<Complaint> findByAssignedOfficerOrderByCreatedAtDesc(User assignedOfficer);

    List<Complaint> findByDepartment(String department);

    List<Complaint> findByDepartmentOrderByCreatedAtDesc(String department);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findBySector(String sector);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    boolean existsByComplaintId(String complaintId);

    long countByStatus(ComplaintStatus status);

    long countByCitizen(User citizen);

    long countByCitizenAndStatus(User citizen, ComplaintStatus status);

    long countByAssignedOfficer(User assignedOfficer);

    long countByAssignedOfficerAndStatus(User assignedOfficer, ComplaintStatus status);
}
