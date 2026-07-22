package com.yapp.todakun.notification.adapter.persistence;

import com.yapp.todakun.notification.DeviceToken;
import com.yapp.todakun.notification.Platform;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "device_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_device_token_token",
                columnNames = "token"
        ),
        indexes = @Index(name = "ix_device_token_member", columnList = "member_id")
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceTokenJpaEntity extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    public static DeviceTokenJpaEntity fromDomain(DeviceToken deviceToken) {
        return DeviceTokenJpaEntity.builder()
                .id(deviceToken.getId())
                .memberId(deviceToken.getMemberId())
                .token(deviceToken.getToken())
                .platform(deviceToken.getPlatform())
                .build();
    }

    public DeviceToken toDomain() {
        return DeviceToken.reconstitute(getId(), memberId, token, platform);
    }
}
