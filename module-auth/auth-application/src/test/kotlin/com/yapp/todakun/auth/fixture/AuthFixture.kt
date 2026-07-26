package com.yapp.todakun.auth.fixture

import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.port.inbound.LoginCommand
import com.yapp.todakun.auth.port.inbound.LogoutCommand
import com.yapp.todakun.auth.port.inbound.RefreshCommand
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.shared.OauthProvider
import java.time.LocalDate
import java.util.UUID

private const val OAUTH_ACCESS_TOKEN = "test-oauth-access-token"
private const val ACCESS_TOKEN = "test-access-token"
private const val REFRESH_TOKEN = "test-refresh-token"
private const val PROVIDER_ID = "1234567890"
private const val EMAIL = "test@todakun.com"
private const val ONBOARDING_TOKEN = "test-onboarding-token"
private const val NAME = "홍길동"
private const val BIRTH_TIME = "0600"
private const val CALENDAR_TYPE = "SOLAR"
private const val GENDER = "MALE"
private const val JOB = "STUDENT"
private const val RELATIONSHIP_STATUS = "SINGLE"
private val FAVORITE_FORTUNE_CATEGORIES = listOf("RELATIONSHIP", "MONEY", "HEALTH")

object AuthFixture {
    val MEMBER_ID: UUID = UUID.fromString("018f0000-0000-7000-8000-000000000001")

    fun loginCommand(
        provider: OauthProvider = OauthProvider.KAKAO,
        oauthAccessToken: String = OAUTH_ACCESS_TOKEN,
    ): LoginCommand =
        LoginCommand(
            provider = provider,
            oauthAccessToken = oauthAccessToken,
        )

    fun signupCommand(
        onboardingToken: String = ONBOARDING_TOKEN,
        name: String = NAME,
        birthDate: LocalDate = LocalDate.of(2000, 1, 1),
        birthTime: String = BIRTH_TIME,
        calendarType: String = CALENDAR_TYPE,
        gender: String = GENDER,
        job: String = JOB,
        relationshipStatus: String = RELATIONSHIP_STATUS,
        favoriteFortuneCategories: List<String> = FAVORITE_FORTUNE_CATEGORIES,
    ): SignupCommand =
        SignupCommand(
            onboardingToken = onboardingToken,
            name = name,
            birthDate = birthDate,
            birthTime = birthTime,
            calendarType = calendarType,
            gender = gender,
            job = job,
            relationshipStatus = relationshipStatus,
            favoriteFortuneCategories = favoriteFortuneCategories,
        )

    fun logoutCommand(accessToken: String = ACCESS_TOKEN): LogoutCommand = LogoutCommand(accessToken = accessToken)

    fun refreshCommand(refreshToken: String = REFRESH_TOKEN): RefreshCommand = RefreshCommand(refreshToken = refreshToken)

    fun oauthMemberProfile(
        provider: OauthProvider = OauthProvider.KAKAO,
        providerId: String = PROVIDER_ID,
        email: String = EMAIL,
    ): OauthMemberProfile =
        OauthMemberProfile(
            provider = provider,
            providerId = providerId,
            email = email,
        )
}
