package com.civicresolve.repository;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select c from Complaint c left join fetch c.citizen left join fetch c.assignedOfficer where c.id = :id")
    Optional<Complaint> findWithDetailsById(@Param("id") Long id);

    List<Complaint> findByCitizen(User citizen);

    @EntityGraph(attributePaths = {"citizen", "assignedOfficer"})
    List<Complaint> findByCitizenOrderByCreatedAtDesc(User citizen);

    List<Complaint> findByCitizen_IdOrderByCreatedAtDesc(Long citizenId);

    List<Complaint> findByAssignedOfficer(User assignedOfficer);

    @EntityGraph(attributePaths = {"citizen"})
    List<Complaint> findByAssignedOfficerOrderByCreatedAtDesc(User assignedOfficer);

    List<Complaint> findByDepartment(String department);

    @EntityGraph(attributePaths = {"citizen", "assignedOfficer"})
    List<Complaint> findByDepartmentOrderByCreatedAtDesc(String department);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findBySector(String sector);

    @EntityGraph(attributePaths = {"citizen", "assignedOfficer"})
    List<Complaint> findAllByOrderByCreatedAtDesc();

    boolean existsByComplaintId(String complaintId);

    long countByStatus(ComplaintStatus status);

    long countByCitizen(User citizen);

    long countByCitizenAndStatus(User citizen, ComplaintStatus status);

    long countByAssignedOfficer(User assignedOfficer);

    long countByAssignedOfficerAndStatus(User assignedOfficer, ComplaintStatus status);
}
