package com.yapp.todakun.terms.repository

import com.yapp.todakun.terms.Terms

interface TermsRepository {
    fun findAll(): List<Terms>
}
