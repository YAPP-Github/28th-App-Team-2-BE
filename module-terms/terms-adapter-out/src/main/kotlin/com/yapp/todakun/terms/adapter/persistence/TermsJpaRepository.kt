package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.TermsType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TermsJpaRepository : JpaRepository<TermsJpaEntity, UUID> {
    fun findAllByType(type: TermsType): List<TermsJpaEntity>
}
