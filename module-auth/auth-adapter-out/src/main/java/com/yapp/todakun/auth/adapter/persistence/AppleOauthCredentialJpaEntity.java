package com.yapp.todakun.auth.adapter.persistence;

import com.yapp.todakun.auth.AppleOauthCredential;
import com.yapp.todakun.auth.adapter.persistence.crypto.AesGcmStringConverter;
import com.yapp.todakun.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    // Apple 계정 연결 해제(revoke) 권한을 갖는 민감 자격증명 → DB 유출 대비 AES-256-GCM으로 at-rest 암호화한다.
    // 암호화 오버헤드(IV+태그+Base64)로 원문보다 길어지므로 기본 255자보다 넉넉하게 잡는다.
    @Convert(converter = AesGcmStringConverter.class)
    @Column(nullable = false, length = 1000)
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
