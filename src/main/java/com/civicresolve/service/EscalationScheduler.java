package com.civicresolve.service;

import com.civicresolve.entity.Escalation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "civicresolve.escalation.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalationEngine escalationEngine;

    public EscalationScheduler(EscalationEngine escalationEngine) {
        this.escalationEngine = escalationEngine;
    }

    @Scheduled(fixedDelayString = "${civicresolve.escalation.scheduler.fixed-delay-ms:300000}")
    public void runScheduledEscalation() {
        log.info("Escalation scheduler started SLA monitoring cycle.");

        try {
            List<Escalation> createdEscalations = escalationEngine.processEscalations(LocalDateTime.now());
            log.info("Escalation scheduler completed SLA monitoring cycle. Processed and created {} new escalation records.",
                    createdEscalations.size());
        } catch (Exception e) {
            log.error("Error occurred during scheduled escalation execution: {}. Will retry on next scheduled execution cycle.",
                    e.getMessage(), e);
        }
    }
}
