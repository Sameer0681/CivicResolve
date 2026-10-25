package com.civicresolve.controller;

import com.civicresolve.dto.ComplaintCreateRequest;
import com.civicresolve.dto.ComplaintStatusUpdateRequest;
import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.ComplaintStatus;
import com.civicresolve.entity.Role;
import com.civicresolve.entity.User;
import com.civicresolve.service.ComplaintService;
import com.civicresolve.service.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import com.civicresolve.exception.BadRequestException;
import com.civicresolve.exception.GlobalExceptionHandler;
import com.civicresolve.exception.OtpRequiredException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComplaintControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintController complaintController;

    private ObjectMapper objectMapper;
    private Complaint complaint;
    private User citizen;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        citizen = User.builder()
                .id(1L)
                .firstName("Aarav")
                .lastName("Sharma")
                .email("citizen@civicresolve.in")
                .role(Role.ROLE_CITIZEN)
                .build();

        userDetails = new CustomUserDetails(citizen);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(complaintController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        complaint = Complaint.builder()
                .id(10L)
                .complaintId("CR-2025-001")
                .title("Severe Pothole on MG Road")
                .description("Dangerous crater on road")
                .sector("Roads & Transport")
                .department("Public Works Department")
                .status(ComplaintStatus.IN_PROGRESS)
                .citizen(citizen)
                .build();
    }

    @Test
    void testPublicTrackingEndpoint() throws Exception {
        when(complaintService.getComplaintByComplaintId("CR-2025-001")).thenReturn(complaint);

        mockMvc.perform(get("/api/complaints/track/CR-2025-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value("CR-2025-001"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sector").value("Roads & Transport"));
    }

    @Test
    void testFilterByStatus() throws Exception {
        when(complaintService.getComplaintsByStatus(ComplaintStatus.IN_PROGRESS)).thenReturn(List.of(complaint));

        mockMvc.perform(get("/api/complaints/status/IN_PROGRESS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value("CR-2025-001"));
    }

    @Test
    void testFilterBySector() throws Exception {
        when(complaintService.getComplaintsBySector("Roads & Transport")).thenReturn(List.of(complaint));

        mockMvc.perform(get("/api/complaints/sector/Roads & Transport")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sector").value("Roads & Transport"));
    }

    @Test
    void testCreateComplaintEndpoint() throws Exception {
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), any())).thenReturn(complaint);

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Severe Pothole on MG Road")
                .description("Dangerous crater on road")
                .sector("Roads & Transport")
                .build();

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.complaintId").value("CR-2025-001"));
    }

    @Test
    void testGetComplaintsByCitizenId() throws Exception {
        when(complaintService.getComplaintsByCitizen(1L)).thenReturn(List.of(complaint));

        mockMvc.perform(get("/api/complaints/citizen/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].complaintId").value("CR-2025-001"));
    }

    @Test
    void testUpdateComplaintStatusEndpoint() throws Exception {
        when(complaintService.updateComplaintStatus(eq("CR-2025-001"), eq(ComplaintStatus.RESOLVED), any(), any(), eq(1L), anyBoolean()))
                .thenReturn(complaint);

        ComplaintStatusUpdateRequest request = new ComplaintStatusUpdateRequest();
        request.setStatus(ComplaintStatus.RESOLVED);
        request.setRemarks("Repaired asphalt");

        mockMvc.perform(put("/api/complaints/CR-2025-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCitizenSendingDifferentCitizenIdIsIgnoredAndUsesLoggedInUser() throws Exception {
        // Point 2: Citizen A (id=1L) sending citizenId=99 in query param (e.g. frontend hardcoding ?citizenId=3)
        // must strictly be ignored, and complaint created for logged-in citizen A (id=1L).
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), any())).thenReturn(complaint);

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Street light not working")
                .description("Dark road at night")
                .sector("Street Lighting")
                .build();

        mockMvc.perform(post("/api/complaints?citizenId=99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(complaintService).createComplaint(any(Complaint.class), eq(1L), any());
    }

    @Test
    void testCreateComplaintWithoutTokenTriggersOtpAndReturns428() throws Exception {
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), isNull()))
                .thenThrow(new OtpRequiredException(
                        "OTP verification is required to submit a complaint. A verification code has been sent to your registered phone.",
                        "+91XXXXXX3210",
                        300L,
                        60L
                ));

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Drainage blocked")
                .description("Water overflowing onto pavement")
                .sector("Sanitation")
                .build();

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.status").value(428))
                .andExpect(jsonPath("$.error").value("OTP_REQUIRED"))
                .andExpect(jsonPath("$.message").value("OTP verification is required to submit a complaint. A verification code has been sent to your registered phone."))
                .andExpect(jsonPath("$.maskedPhone").value("+91XXXXXX3210"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60))
                .andExpect(jsonPath("$.otp").doesNotExist());
    }

    @Test
    void testCreateComplaintMissingPhoneReturns400() throws Exception {
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), isNull()))
                .thenThrow(new BadRequestException(
                        "No mobile phone number registered on your account. Please add a valid phone number via PUT /api/auth/profile/phone before submitting complaints."
                ));

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Street lamp broken")
                .description("Lamp post #12 dark")
                .sector("Lighting")
                .build();

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("No mobile phone number registered on your account. Please add a valid phone number via PUT /api/auth/profile/phone before submitting complaints."));
    }

    @Test
    void testCreateComplaint_smsFails_callOk_returnsOtpRequired_428() throws Exception {
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), isNull()))
                .thenThrow(new OtpRequiredException(
                        "OTP verification is required to submit a complaint. A verification code has been dispatched to your registered phone via voice call.",
                        "+91XXXXXX3210",
                        300L,
                        60L,
                        "CALL"
                ));

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Pothole")
                .description("Pothole description")
                .sector("Roads")
                .build();

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.status").value(428))
                .andExpect(jsonPath("$.error").value("OTP_REQUIRED"))
                .andExpect(jsonPath("$.maskedPhone").value("+91XXXXXX3210"))
                .andExpect(jsonPath("$.channel").value("CALL"))
                .andExpect(jsonPath("$.message").value("OTP verification is required to submit a complaint. A verification code has been dispatched to your registered phone via voice call."));
    }

    @Test
    void testCreateComplaint_bothChannelsFail_returns502Json() throws Exception {
        when(complaintService.createComplaint(any(Complaint.class), eq(1L), isNull()))
                .thenThrow(new com.civicresolve.exception.OtpException(
                        "OTP_DELIVERY_FAILED",
                        "Failed to deliver verification code. Upstream provider error: (SMS: 572006 rejection; Call: Network timeout)",
                        org.springframework.http.HttpStatus.BAD_GATEWAY
                ));

        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .title("Pothole")
                .description("Pothole description")
                .sector("Roads")
                .build();

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("OTP_DELIVERY_FAILED"))
                .andExpect(jsonPath("$.message").value("Failed to deliver verification code. Upstream provider error: (SMS: 572006 rejection; Call: Network timeout)"));
    }
}
