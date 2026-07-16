package com.yapp.todakun.auth.adapter.web

import com.yapp.todakun.auth.adapter.web.dto.request.LoginRequest
import com.yapp.todakun.auth.adapter.web.dto.request.SignupRequest
import com.yapp.todakun.auth.adapter.web.dto.response.LoginResponse
import com.yapp.todakun.auth.adapter.web.dto.response.SignupResponse
import com.yapp.todakun.web.openapi.annotation.DisableSwaggerSecurity
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Auth", description = "인증 API")
interface AuthApi {
    @Operation(
        summary = "소셜 로그인",
        description = "OAuth 액세스 토큰으로 로그인을 시도한다. 기존 회원이면 access/refresh 토큰을, 신규 회원이면 온보딩 토큰을 발급한다.",
    )
    @DisableSwaggerSecurity
    @PostMapping("api/v1/auth/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
    ): ResponseEntity<CommonResponse<LoginResponse>>

    @Operation(
        summary = "회원가입",
        description = "온보딩 토큰과 프로필 정보로 회원가입을 완료하고 access/refresh 토큰을 발급한다.",
    )
    @DisableSwaggerSecurity
    @PostMapping("api/v1/auth/signup")
    fun signup(
        @RequestBody @Valid request: SignupRequest,
    ): ResponseEntity<CommonResponse<SignupResponse>>
}
