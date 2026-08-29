package com.yapp.todakun.auth.adapter.redis.blacklist

import org.springframework.data.repository.CrudRepository

interface BlacklistTokenRepository : CrudRepository<BlacklistToken, String>
