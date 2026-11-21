package com.myhourly.jewellery.inventory.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Remembers every Kafka event this module has already handled.
 * eventId is the primary key, so the database itself refuses a duplicate.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    protected ProcessedEvent() { }

    public ProcessedEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
