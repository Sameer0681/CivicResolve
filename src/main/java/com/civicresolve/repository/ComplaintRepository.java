package com.civicresolve.repository;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;


@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintId(String complaintId);

    Optional<Complaint> findByIdAndCitizen_Id(Long id, Long citizenId);

    List<Complaint> findByCitizen(User citizen);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findBySector(String sector);

    boolean existsByComplaintId(String complaintId);
}
