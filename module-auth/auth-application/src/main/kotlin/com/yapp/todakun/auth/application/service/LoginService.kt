package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.auth.port.inbound.LoginResult
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OAuthPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.MemberAuthPort

@CommandService
class LoginService(
    private val oAuthPort: OAuthPort,
    private val memberAuthPort: MemberAuthPort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val onboardingTokenPort: OnboardingTokenPort,
) : LoginUseCase {
    override fun login(command: LoginCommand): LoginResult {
        val profile = oAuthPort.fetchProfile(command.provider, command.oauthAccessToken)
        val memberId = memberAuthPort.findMemberId(profile.provider, profile.providerId)

        return if (memberId == null) {
            LoginResult(isNewMember = true, onboardingToken = onboardingTokenPort.issue(profile))
        } else {
            LoginResult(
                isNewMember = false,
                accessToken = accessTokenPort.generate(memberId),
                refreshToken = refreshTokenPort.issue(memberId),
            )
        }
    }
}
