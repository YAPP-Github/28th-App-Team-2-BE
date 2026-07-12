package com.yapp.todakun.auth.adapter.web.controller

import com.yapp.todakun.auth.adapter.web.AuthApi
import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.request.SignupRequest
import com.yapp.todakun.auth.adapter.web.dto.response.LoginResponse
import com.yapp.todakun.auth.adapter.web.dto.response.SignupResponse
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.auth.port.inbound.SignupUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val loginUseCase: LoginUseCase,
    private val signupUseCase: SignupUseCase,
) : AuthApi {
    override fun login(request: LoginRequest): ResponseEntity<CommonResponse<LoginResponse>> {
        val result = loginUseCase.login(request.toCommand())

        return CommonResponse.success(LoginResponse.from(result))
    }

    override fun signup(request: SignupRequest): ResponseEntity<CommonResponse<SignupResponse>> {
        val result = signupUseCase.signup(request.toCommand())

        return CommonResponse.created(SignupResponse.from(result))
    }
}
