package com.civicresolve.service;

import com.civicresolve.dto.AiClassificationResult;

public interface AiClassificationService {

    AiClassificationResult classify(String title, String description);
}
