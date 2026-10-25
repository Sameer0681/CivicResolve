package com.civicresolve.service;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;

import java.util.List;

public interface ComplaintService {

    Complaint createComplaint(Complaint complaint, Long citizenId);

    Complaint createComplaint(Complaint complaint, Long citizenId, String otpToken);

    List<Complaint> getAllComplaints();

    Complaint getComplaintById(Long id);

    Complaint getComplaintByComplaintId(String complaintId);

    List<Complaint> getComplaintsByStatus(ComplaintStatus status);

    List<Complaint> getComplaintsBySector(String sector);

    List<Complaint> getComplaintsByCitizen(Long citizenId);

    List<Complaint> getComplaintsByOfficer(Long officerId);

    List<Complaint> getComplaintsByDepartment(String department);

    Complaint getComplaintByIdForCitizen(Long id, Long citizenId);

    Complaint getComplaintByIdForOfficer(Long id, Long officerId);

    Complaint updateComplaintStatus(Long id, ComplaintStatus status, String officerNotes, Long officerOrAdminId, boolean isAdmin);

    Complaint updateComplaintStatus(String complaintIdOrId, ComplaintStatus status, String remarks, String proofUrl, Long officerOrAdminId, boolean isAdmin);

    Complaint assignOfficer(Long id, Long officerId);

    void deleteComplaint(Long id);
}
