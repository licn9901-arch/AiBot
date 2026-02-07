package com.deskpet.core.model;

import com.deskpet.core.persistence.SnowflakeIdentifierGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "thing_model_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThingModelEvent {

    public enum EventType {
        INFO("info"),
        ALERT("alert"),
        ERROR("error");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static EventType fromValue(String value) {
            for (EventType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return INFO;
        }
    }

    @Id
    @GenericGenerator(name = "snowflake", type = SnowflakeIdentifierGenerator.class)
    @GeneratedValue(generator = "snowflake")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String identifier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_params", columnDefinition = "jsonb")
    private List<Map<String, Object>> outputParams;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public EventType getEventTypeEnum() {
        return EventType.fromValue(eventType);
    }

    public void setEventTypeEnum(EventType type) {
        this.eventType = type.getValue();
    }

    public boolean isInfo() {
        return "info".equals(eventType);
    }

    public boolean isAlert() {
        return "alert".equals(eventType);
    }

    public boolean isError() {
        return "error".equals(eventType);
    }
}
