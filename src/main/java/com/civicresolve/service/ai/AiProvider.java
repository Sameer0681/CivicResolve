package com.civicresolve.service.ai;

import com.civicresolve.dto.AiClassificationResult;

public interface AiProvider {

    AiClassificationResult classify(String title, String description);
}
