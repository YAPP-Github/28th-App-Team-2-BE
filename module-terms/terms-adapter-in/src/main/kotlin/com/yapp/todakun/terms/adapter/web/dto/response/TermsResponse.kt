package com.yapp.todakun.terms.adapter.web.dto.response

import com.yapp.todakun.terms.Terms
import java.util.UUID

data class TermsResponse(
    val id: UUID,
    val type: String,
    val title: String,
    val required: Boolean,
) {
    companion object {
        fun from(terms: Terms) =
            TermsResponse(
                id = terms.id,
                type = terms.type.name,
                title = terms.title,
                required = terms.required,
            )
    }
}
