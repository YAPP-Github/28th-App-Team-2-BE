package com.yapp.todakun.auth.adapter.redis.onboarding

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.token.IssuedOnboardingToken
import com.yapp.todakun.shared.OauthProvider
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
    override fun issue(profile: OauthMemberProfile): IssuedOnboardingToken {
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

    override fun findProfile(token: String): OauthMemberProfile? =
        onboardingTokenRepository.findById(token)
            .map {
                OauthMemberProfile(
                    provider = OauthProvider.valueOf(it.provider),
                    providerId = it.providerId,
                    email = it.email,
                )
            }
            .orElse(null)

    override fun revoke(token: String) {
        onboardingTokenRepository.deleteById(token)
    }
}
