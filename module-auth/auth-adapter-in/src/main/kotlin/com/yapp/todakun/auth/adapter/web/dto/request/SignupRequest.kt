package com.yapp.todakun.auth.adapter.web.dto.request

import com.yapp.todakun.auth.port.inbound.SignupCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class SignupRequest(
    @field:NotBlank
    val onboardingToken: String,
    @field:NotBlank
    val name: String,
    @field:NotNull
    val birthDate: LocalDate,
    @field:NotBlank
    val birthTime: String,
    @field:NotBlank
    val calendarType: String,
    @field:NotBlank
    val gender: String,
    @field:NotBlank
    val job: String,
    @field:NotBlank
    val relationshipStatus: String,
) {
    fun toCommand() =
        SignupCommand(
            onboardingToken = onboardingToken,
            name = name,
            birthDate = birthDate,
            birthTime = birthTime,
            calendarType = calendarType,
            gender = gender,
            job = job,
            relationshipStatus = relationshipStatus,
        )
}
