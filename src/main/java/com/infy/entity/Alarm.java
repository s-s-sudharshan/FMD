package com.infy.entity;

import java.time.LocalDateTime;

import com.infy.enums.AlarmStatus;
import com.infy.enums.DeviceType;
import com.infy.enums.Severity;
import com.infy.enums.TrapType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fault/alarm raised by a monitored device. No @Data on entities -- see plan.md Lombok convention.
 * deviceIp/serialNumber/deviceType are deliberately denormalized copies so
 * alarm history stays readable even if the device is later edited/deactivated.
 * The six acknowledged/cleared/terminated By/At fields are the audit trail
 * (Extra 3): usernames are stored as plain strings, not foreign keys, so the
 * history stays readable after a user is changed. All are nullable so rows
 * created before the audit feature need no backfill.
 */
@Entity
@Table(name = "alarms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false)
    private String deviceIp;

    @Column(nullable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrapType trap;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private Integer occurrence = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlarmStatus status = AlarmStatus.UNACKNOWLEDGED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String acknowledgedBy;

    @Column
    private LocalDateTime acknowledgedAt;

    @Column
    private String clearedBy;

    @Column
    private LocalDateTime clearedAt;

    @Column
    private String terminatedBy;

    @Column
    private LocalDateTime terminatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
