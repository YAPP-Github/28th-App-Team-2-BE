package com.yapp.todakun.auth.adapter.redis.onboarding

import org.springframework.data.repository.CrudRepository

interface OnboardingTokenRepository : CrudRepository<OnboardingToken, String>
