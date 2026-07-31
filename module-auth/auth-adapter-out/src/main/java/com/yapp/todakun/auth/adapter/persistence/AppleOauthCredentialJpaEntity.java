package com.yapp.todakun.auth.adapter.persistence;

import com.yapp.todakun.auth.AppleOauthCredential;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "apple_oauth_credential",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_apple_oauth_credential_provider_id",
                columnNames = "provider_id"
        )
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AppleOauthCredentialJpaEntity extends BaseEntity {

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String refreshToken;

    public static AppleOauthCredentialJpaEntity fromDomain(AppleOauthCredential credential) {
        return AppleOauthCredentialJpaEntity.builder()
                .id(credential.getId())
                .providerId(credential.getProviderId())
                .clientId(credential.getClientId())
                .refreshToken(credential.getRefreshToken())
                .build();
    }

    public AppleOauthCredential toDomain() {
        return AppleOauthCredential.reconstitute(getId(), providerId, clientId, refreshToken);
    }
}
