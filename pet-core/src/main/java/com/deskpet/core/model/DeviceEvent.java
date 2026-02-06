package com.deskpet.core.model;

import com.deskpet.core.persistence.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "device_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> params;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public ThingModelEvent.EventType getEventTypeEnum() {
        return ThingModelEvent.EventType.fromValue(eventType);
    }

    public void setEventTypeEnum(ThingModelEvent.EventType type) {
        this.eventType = type.getValue();
    }
}
