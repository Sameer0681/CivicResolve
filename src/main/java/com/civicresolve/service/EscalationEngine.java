package com.civicresolve.service;

import com.civicresolve.entity.Escalation;

import java.time.LocalDateTime;
import java.util.List;

public interface EscalationEngine {

    List<Escalation> processEscalations(LocalDateTime referenceTime);

    List<Escalation> processEscalations();
}
