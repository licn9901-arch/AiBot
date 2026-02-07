package com.deskpet.core.model;

import com.deskpet.core.persistence.SnowflakeIdentifierGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "thing_model_property")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThingModelProperty {

    public enum DataType {
        INT("int"),
        FLOAT("float"),
        BOOL("bool"),
        STRING("string"),
        ENUM("enum"),
        STRUCT("struct");

        private final String value;

        DataType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DataType fromValue(String value) {
            for (DataType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown data type: " + value);
        }
    }

    public enum AccessMode {
        R("r"),
        RW("rw");

        private final String value;

        AccessMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AccessMode fromValue(String value) {
            for (AccessMode mode : values()) {
                if (mode.value.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return R;
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

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> specs;

    @Column(name = "access_mode", nullable = false, length = 10)
    @Builder.Default
    private String accessMode = "r";

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public DataType getDataTypeEnum() {
        return DataType.fromValue(dataType);
    }

    public void setDataTypeEnum(DataType type) {
        this.dataType = type.getValue();
    }

    public AccessMode getAccessModeEnum() {
        return AccessMode.fromValue(accessMode);
    }

    public void setAccessModeEnum(AccessMode mode) {
        this.accessMode = mode.getValue();
    }

    public boolean isReadOnly() {
        return "r".equals(accessMode);
    }

    public boolean isWritable() {
        return "rw".equals(accessMode);
    }
}
