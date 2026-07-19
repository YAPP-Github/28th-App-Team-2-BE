package com.yapp.todakun.terms.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TermsJpaRepository : JpaRepository<TermsJpaEntity, UUID>
