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

    List<Complaint> findByCitizen(User citizen);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findBySector(String sector);

    boolean existsByComplaintId(String complaintId);
}
