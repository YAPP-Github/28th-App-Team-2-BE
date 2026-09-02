package com.yapp.todakun.auth.adapter.web.controller

import com.yapp.todakun.auth.adapter.web.AuthApi
import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.request.RefreshRequest
import com.yapp.todakun.auth.adapter.web.dto.request.SignupRequest
import com.yapp.todakun.auth.adapter.web.dto.response.LoginResponse
import com.yapp.todakun.auth.adapter.web.dto.response.RefreshResponse
import com.yapp.todakun.auth.adapter.web.dto.response.SignupResponse
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.auth.port.inbound.LogoutCommand
import com.yapp.todakun.auth.port.inbound.LogoutUseCase
import com.yapp.todakun.auth.port.inbound.RefreshUseCase
import com.yapp.todakun.auth.port.inbound.SignupUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshUseCase: RefreshUseCase,
) : AuthApi {
    override fun login(request: LoginRequest): ResponseEntity<CommonResponse<LoginResponse>> {
        val result = loginUseCase.login(request.toCommand())

        return CommonResponse.success(LoginResponse.from(result))
    }

    override fun signup(request: SignupRequest): ResponseEntity<CommonResponse<SignupResponse>> {
        val result = signupUseCase.signup(request.toCommand())

        return CommonResponse.created(SignupResponse.from(result))
    }

    override fun logout(
        memberId: UUID,
        jti: String,
        remainingSeconds: Long,
    ): ResponseEntity<CommonResponse<Unit>> {
        logoutUseCase.logout(LogoutCommand(memberId = memberId, jti = jti, remainingSeconds = remainingSeconds))

        return CommonResponse.deleted()
    }

    override fun refresh(request: RefreshRequest): ResponseEntity<CommonResponse<RefreshResponse>> {
        val result = refreshUseCase.refresh(request.toCommand())

        return CommonResponse.success(RefreshResponse.from(result))
    }
}
