package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.repository.TermsRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

@Repository
class TermsRepositoryAdapter(
    private val termsJpaRepository: TermsJpaRepository,
) : TermsRepository {
    // 필수 약관 먼저, 그다음 유형순 — 클라이언트 렌더링 순서를 안정화한다.
    override fun findAll(): List<Terms> =
        termsJpaRepository
            .findAll(Sort.by(Sort.Order.desc("required"), Sort.Order.asc("type")))
            .map { it.toDomain() }

    override fun findAllByType(type: TermsType): List<Terms> = termsJpaRepository.findAllByType(type).map { it.toDomain() }
}
