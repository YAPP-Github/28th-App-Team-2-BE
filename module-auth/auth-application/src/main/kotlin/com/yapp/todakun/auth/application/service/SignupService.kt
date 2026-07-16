package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.OnboardingTokenInvalidException
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.auth.port.inbound.SignupResult
import com.yapp.todakun.auth.port.inbound.SignupUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.CreateMemberPort

@CommandService
class SignupService(
    private val onboardingTokenPort: OnboardingTokenPort,
    private val createMemberPort: CreateMemberPort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) : SignupUseCase {
    override fun signup(command: SignupCommand): SignupResult {
        val profile =
            onboardingTokenPort.findProfile(command.onboardingToken)
                ?: throw OnboardingTokenInvalidException()

        try {
            val memberId =
                createMemberPort.create(
                    provider = profile.provider,
                    providerId = profile.providerId,
                    name = command.name,
                    birthDate = command.birthDate,
                    birthTime = command.birthTime,
                    calendarType = command.calendarType,
                    gender = command.gender,
                    job = command.job,
                    relationshipStatus = command.relationshipStatus,
                )

            return SignupResult(
                accessToken = accessTokenPort.generate(memberId),
                refreshToken = refreshTokenPort.issue(memberId),
            )
        } finally {
            onboardingTokenPort.revoke(command.onboardingToken)
        }
    }
}
