package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

interface CreateMemberPort {
    fun create(
        provider: OauthProvider,
        providerId: String,
        name: String,
        birthDate: LocalDate,
        birthTime: String,
        calendarType: String,
        gender: String,
        job: String,
        relationshipStatus: String,
        favoriteFortuneCategories: List<String>,
    ): UUID
}
