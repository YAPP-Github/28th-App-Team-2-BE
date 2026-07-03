package com.yapp.todakun.auth.adapter.redis

import org.springframework.data.repository.CrudRepository

interface BlacklistTokenRepository : CrudRepository<BlacklistToken, String>
