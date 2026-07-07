package com.yapp.todakun.member.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberJpaRepository : JpaRepository<MemberJpaEntity, UUID>
