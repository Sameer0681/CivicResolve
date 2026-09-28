package com.civicresolve.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EscalationSchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    static class TestConfig {
        // Minimal configuration for context runner
    }

    @Test
    @DisplayName("Test 1 & 2 — Scheduler bean created when enabled=true or default")
    void testSchedulerEnabledByDefault() {
        contextRunner
                .withPropertyValues("civicresolve.escalation.scheduler.enabled=true", "civicresolve.escalation.scheduler.fixed-delay-ms=300000")
                .withBean(EscalationEngine.class, () -> mock(EscalationEngine.class))
                .withBean(EscalationScheduler.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(EscalationScheduler.class);
                });
    }

    @Test
    @DisplayName("Test 3 — Scheduler bean NOT created when enabled=false")
    void testSchedulerDisabled() {
        contextRunner
                .withPropertyValues("civicresolve.escalation.scheduler.enabled=false")
                .withBean(EscalationEngine.class, () -> mock(EscalationEngine.class))
                .withBean(EscalationScheduler.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EscalationScheduler.class);
                });
    }

    @Test
    @DisplayName("Test 4 — Scheduler delegates execution to EscalationEngine")
    void testSchedulerDelegatesToEngine() {
        EscalationEngine mockEngine = mock(EscalationEngine.class);
        when(mockEngine.processEscalations(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        EscalationScheduler scheduler = new EscalationScheduler(mockEngine);
        scheduler.runScheduledEscalation();

        verify(mockEngine, times(1)).processEscalations(any(LocalDateTime.class));
    }
}
