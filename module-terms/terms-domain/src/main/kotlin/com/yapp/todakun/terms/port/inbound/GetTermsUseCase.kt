package com.yapp.todakun.terms.port.inbound

import com.yapp.todakun.terms.Terms

interface GetTermsUseCase {
    fun getAllTerms(): List<Terms>
}
