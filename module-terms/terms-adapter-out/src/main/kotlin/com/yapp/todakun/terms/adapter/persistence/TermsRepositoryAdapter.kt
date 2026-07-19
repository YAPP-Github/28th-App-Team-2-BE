package com.yapp.todakun.terms.adapter.persistence

import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.repository.TermsRepository
import org.springframework.stereotype.Repository

@Repository
class TermsRepositoryAdapter(
    private val termsJpaRepository: TermsJpaRepository,
) : TermsRepository {
    override fun findAll(): List<Terms> = termsJpaRepository.findAll().map { it.toDomain() }
}
