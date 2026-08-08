package com.yapp.todakun.terms.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.repository.TermsRepository
import org.springframework.cache.annotation.Cacheable

@QueryService
class GetTermsService(
    private val termsRepository: TermsRepository,
) : GetTermsUseCase {
    // 회원 무관 전역 데이터라 무효화 지점이 없다(이슈 #56) — TERMS 캐시는 TTL 만료로만 갱신된다.
    @Cacheable(cacheNames = [CacheNames.TERMS])
    override fun getAllTerms(): List<Terms> = termsRepository.findAll()
}
