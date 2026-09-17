package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;

import java.util.List;

public interface ComplaintService {

    Complaint createComplaint(Complaint complaint, Long citizenId);

    List<Complaint> getAllComplaints();

    Complaint getComplaintById(Long id);

    Complaint getComplaintByComplaintId(String complaintId);

    List<Complaint> getComplaintsByStatus(ComplaintStatus status);

    List<Complaint> getComplaintsBySector(String sector);

    void deleteComplaint(Long id);
}