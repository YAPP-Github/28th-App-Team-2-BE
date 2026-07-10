package com.yapp.todakun.auth.adapter.web.controller

import com.yapp.todakun.auth.adapter.web.AuthApi
import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.response.LoginResponse
import com.yapp.todakun.auth.port.inbound.LoginUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val loginUseCase: LoginUseCase,
) : AuthApi {
    override fun login(request: LoginRequest): ResponseEntity<CommonResponse<LoginResponse>> {
        val result = loginUseCase.login(request.toCommand())

        return CommonResponse.success(LoginResponse.from(result))
    }
}
