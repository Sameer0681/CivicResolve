
package com.civicresolve.ai;

import com.civicresolve.entity.Complaint;
import com.civicresolve.entity.User;
import com.civicresolve.repository.ComplaintRepository;
import com.civicresolve.repository.UserRepository;
import com.civicresolve.service.ComplaintServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiAiProviderTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeminiAiProvider geminiAiProvider;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    @Test
    void createComplaintShouldUseAiClassification() {
        User citizen = mock(User.class);
        Complaint complaint = mock(Complaint.class);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(citizen));

        AiClassificationResult aiResult =
                new AiClassificationResult();
        aiResult.setSector("Roads & Transport");
        aiResult.setDepartment("Public Works Department");

        when(complaint.getTitle()).thenReturn("Dangerous potholes");
        when(complaint.getDescription())
                .thenReturn("Deep potholes on the main road.");

        when(geminiAiProvider.classify(any(String.class)))
                .thenReturn(aiResult);

        when(complaintRepository.save(complaint))
                .thenReturn(complaint);

        complaintService.createComplaint(complaint, 3L);

        verify(geminiAiProvider).classify(
                "Dangerous potholes\nDeep potholes on the main road.");

        verify(complaint).setSector("Roads & Transport");
        verify(complaint).setDepartment("Public Works Department");
        verify(complaint).setCitizen(citizen);
        verify(complaintRepository).save(complaint);

        assertEquals("Roads & Transport", aiResult.getSector());
    }
}
