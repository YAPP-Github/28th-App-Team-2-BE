package com.yapp.todakun.auth.application.service

import com.yapp.todakun.auth.exception.ReSignupRestrictedException
import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.auth.port.inbound.LoginResult
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.port.outbound.OauthPort
import com.yapp.todakun.auth.port.outbound.OnboardingTokenPort
import com.yapp.todakun.auth.port.outbound.RefreshTokenPort
import com.yapp.todakun.auth.port.outbound.WithdrawnAccountPort
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.GetMemberIdPort

@CommandService
class LoginService(
    private val oauthPort: OauthPort,
    private val getMemberIdPort: GetMemberIdPort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val onboardingTokenPort: OnboardingTokenPort,
    private val withdrawnAccountPort: WithdrawnAccountPort,
) : LoginUseCase {
    override fun login(command: LoginCommand): LoginResult {
        val profile = oauthPort.fetchProfile(command.provider, command.oauthAccessToken, command.authorizationCode)
        val memberId = getMemberIdPort.findIdByOauth(profile.provider, profile.providerId)

        return if (memberId == null) {
            // 신규 회원 분기: 탈퇴 후 재가입 제한(90일) 대상이면 온보딩 발급 없이 즉시 차단한다.
            if (withdrawnAccountPort.isRestricted(profile.provider, profile.providerId)) {
                throw ReSignupRestrictedException()
            }
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
