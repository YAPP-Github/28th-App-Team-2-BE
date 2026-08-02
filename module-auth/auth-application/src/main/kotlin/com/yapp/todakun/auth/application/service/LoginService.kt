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
import com.yapp.todakun.shared.GetMemberIdPort
import org.springframework.stereotype.Service

// login()이 호출하는 포트 중 원자적으로 묶어야 할 RDB 쓰기가 없어(GetMemberIdPort는 읽기 전용,
// 나머지는 Redis/무상태 JWT) 의도적으로 @CommandService(트랜잭션)를 걸지 않는다.
// 걸면 Apple 인증 코드 교환(HTTP, 최대 8초) 동안 DB 커넥션을 불필요하게 점유하게 된다.
@Service
class LoginService(
    private val oauthPort: OauthPort,
    private val getMemberIdPort: GetMemberIdPort,
    private val accessTokenPort: AccessTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
    private val onboardingTokenPort: OnboardingTokenPort,
    private val withdrawnAccountPort: WithdrawnAccountPort,
) : LoginUseCase {
    override fun login(command: LoginCommand): LoginResult {
        command.requireAppleAuthorizationCode()

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
