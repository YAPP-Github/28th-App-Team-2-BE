package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.auth.port.inbound.LoginResult
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.GetMemberPort

@CommandService
class LoginService(
    private val oauthPort: OauthPort,
    private val getMemberPort: GetMemberPort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val onboardingTokenPort: OnboardingTokenPort,
) : LoginUseCase {
    override fun login(command: LoginCommand): LoginResult {
        val profile = oauthPort.fetchProfile(command.provider, command.oauthAccessToken)
        val memberId = getMemberPort.findIdByOauth(profile.provider, profile.providerId)

        return if (memberId == null) {
            LoginResult(
                isNewMember = true,
                onboardingToken = onboardingTokenPort.issue(profile),
            )
        } else {
            LoginResult(
                isNewMember = false,
                accessToken = accessTokenPort.generate(memberId),
                refreshToken = refreshTokenPort.issue(memberId),
            )
        }
    }
}
