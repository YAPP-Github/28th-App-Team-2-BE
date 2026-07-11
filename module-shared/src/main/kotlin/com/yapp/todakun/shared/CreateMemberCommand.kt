package com.yapp.todakun.shared

import java.time.LocalDate

data class CreateMemberCommand(
    val provider: OauthProvider,
    val providerId: String,
    val name: String,
    val birthDate: LocalDate,
    val birthTime: String,
    val calendarType: String,
    val gender: String,
    val job: String,
    val relationshipStatus: String,
)
