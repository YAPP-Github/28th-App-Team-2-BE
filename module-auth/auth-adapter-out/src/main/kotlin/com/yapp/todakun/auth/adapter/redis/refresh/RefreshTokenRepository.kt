package com.yapp.todakun.auth.adapter.redis.refresh

import org.springframework.data.repository.CrudRepository

interface RefreshTokenRepository : CrudRepository<RefreshToken, String> {
    fun deleteAllByMemberId(memberId: String)
}
