package com.yapp.todakun.auth.adapter.oauth

import com.yapp.todakun.auth.adapter.oauth.apple.AppleOauthTokenClient
import com.yapp.todakun.auth.port.outbound.AppleOauthCredentialPort
import com.yapp.todakun.common.logging.Loggable
import com.yapp.todakun.shared.OauthProvider
import com.yapp.todakun.shared.RevokeOauthTokenPort
import org.springframework.stereotype.Component

/**
 * [RevokeOauthTokenPort] 구현. Apple만 App Store 심사 가이드라인(5.1.1(v))에 따라
 * 계정 삭제 시 Apple 쪽 연결도 해제해야 하므로, Kakao/Google은 no-op으로 둔다.
 */
@Component
@Loggable
class RevokeOauthTokenAdapter(
    private val appleOauthCredentialPort: AppleOauthCredentialPort,
    private val appleOauthTokenClient: AppleOauthTokenClient,
) : RevokeOauthTokenPort {
    override fun revokeIfApplicable(
        provider: OauthProvider,
        providerId: String,
    ) {
        if (provider != OauthProvider.APPLE) return

        val credential = appleOauthCredentialPort.find(providerId) ?: return

        // revoke 실패가 회원 하드 삭제 자체를 막지 않도록 로그만 남긴다(정책 확정).
        try {
            appleOauthTokenClient.revoke(credential.clientId, credential.refreshToken)
        } catch (exception: RuntimeException) {
            log.warn("Apple 계정 연결 해제 실패 - providerId: {}", providerId, exception)
        }

        appleOauthCredentialPort.delete(providerId)
    }
}
