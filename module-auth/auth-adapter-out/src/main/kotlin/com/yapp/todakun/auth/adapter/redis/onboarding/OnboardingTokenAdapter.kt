package com.yapp.todakun.auth.adapter.redis.onboarding

import com.yapp.todakun.auth.OAuthMemberProfile
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.shared.OAuthProvider
import org.springframework.stereotype.Component
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@Component
class OnboardingTokenAdapter(
    private val onboardingTokenRepository: OnboardingTokenRepository,
    private val onboardingTokenProperties: OnboardingTokenProperties,
) : OnboardingTokenPort {
    @ExperimentalUuidApi
    override fun issue(profile: OAuthMemberProfile): IssuedOnboardingToken {
        val token = Uuid.generateV7().toJavaUuid().toString()
        onboardingTokenRepository.save(
            OnboardingToken(
                value = token,
                provider = profile.provider.name,
                providerId = profile.providerId,
                email = profile.email,
                ttl = onboardingTokenProperties.expirySeconds,
            ),
        )

        return IssuedOnboardingToken(value = token, expiresInSeconds = onboardingTokenProperties.expirySeconds)
    }

    override fun findProfile(token: String): OAuthMemberProfile? =
        onboardingTokenRepository.findById(token)
            .map {
                OAuthMemberProfile(
                    provider = OAuthProvider.valueOf(it.provider),
                    providerId = it.providerId,
                    email = it.email,
                )
            }
            .orElse(null)

    override fun revoke(token: String) {
        onboardingTokenRepository.deleteById(token)
    }
}
