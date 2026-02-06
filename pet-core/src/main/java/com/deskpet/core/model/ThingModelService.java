package com.deskpet.core.model;

import com.deskpet.core.persistence.JsonListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "thing_model_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThingModelService {

    public enum CallType {
        ASYNC("async"),
        SYNC("sync");

        private final String value;

        CallType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static CallType fromValue(String value) {
            for (CallType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return ASYNC;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String identifier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "call_type", nullable = false, length = 20)
    @Builder.Default
    private String callType = "async";

    @Convert(converter = JsonListConverter.class)
    @Column(name = "input_params", columnDefinition = "jsonb")
    private List<Map<String, Object>> inputParams;

    @Convert(converter = JsonListConverter.class)
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

    public CallType getCallTypeEnum() {
        return CallType.fromValue(callType);
    }

    public void setCallTypeEnum(CallType type) {
        this.callType = type.getValue();
    }

    public boolean isAsync() {
        return "async".equals(callType);
    }

    public boolean isSync() {
        return "sync".equals(callType);
    }
}
