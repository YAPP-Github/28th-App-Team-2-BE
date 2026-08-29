package com.yapp.todakun.terms.repository

import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType

interface TermsRepository {
    fun findAll(): List<Terms>

    fun findAllByType(type: TermsType): List<Terms>
}
