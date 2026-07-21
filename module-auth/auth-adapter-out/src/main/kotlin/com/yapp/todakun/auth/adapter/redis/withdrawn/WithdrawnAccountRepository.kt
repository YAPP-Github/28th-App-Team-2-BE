package com.yapp.todakun.auth.adapter.redis.withdrawn

import org.springframework.data.repository.CrudRepository

interface WithdrawnAccountRepository : CrudRepository<WithdrawnAccount, String>
