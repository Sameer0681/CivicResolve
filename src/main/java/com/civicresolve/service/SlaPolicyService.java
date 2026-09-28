package com.civicresolve.service;

import com.civicresolve.entity.ComplaintPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SlaPolicyService {

    @Value("${civicresolve.sla.low-days:7}")
    private long lowDays = 7;

    @Value("${civicresolve.sla.medium-days:5}")
    private long mediumDays = 5;

    @Value("${civicresolve.sla.high-days:2}")
    private long highDays = 2;

    @Value("${civicresolve.sla.critical-hours:24}")
    private long criticalHours = 24;

    public SlaPolicyService() {
    }

    public SlaPolicyService(long lowDays, long mediumDays, long highDays, long criticalHours) {
        this.lowDays = lowDays;
        this.mediumDays = mediumDays;
        this.highDays = highDays;
        this.criticalHours = criticalHours;
    }

    public Duration getSlaDuration(ComplaintPriority priority) {
        if (priority == null) {
            return Duration.ofDays(mediumDays);
        }
        return switch (priority) {
            case LOW -> Duration.ofDays(lowDays);
            case MEDIUM -> Duration.ofDays(mediumDays);
            case HIGH -> Duration.ofDays(highDays);
            case CRITICAL -> Duration.ofHours(criticalHours);
        };
    }

    public LocalDateTime calculateSlaDeadline(ComplaintPriority priority, LocalDateTime creationTime) {
        LocalDateTime baseTime = creationTime != null ? creationTime : LocalDateTime.now();
        return baseTime.plus(getSlaDuration(priority));
    }

    public long getLowDays() { return lowDays; }
    public long getMediumDays() { return mediumDays; }
    public long getHighDays() { return highDays; }
    public long getCriticalHours() { return criticalHours; }
}
