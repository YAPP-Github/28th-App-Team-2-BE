package com.yapp.todakun.auth.port.inbound

import java.time.LocalDate

data class SignupCommand(
    val onboardingToken: String,
    val name: String,
    val birthDate: LocalDate,
    val birthTime: String,
    val calendarType: String,
    val gender: String,
    val job: String,
    val relationshipStatus: String,
    val favoriteFortuneCategories: List<String>,
)
