package com.yapp.todakun.terms.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.repository.TermsRepository

@QueryService
class GetTermsService(
    private val termsRepository: TermsRepository,
) : GetTermsUseCase {
    override fun getAllTerms(): List<Terms> = termsRepository.findAll()
}
