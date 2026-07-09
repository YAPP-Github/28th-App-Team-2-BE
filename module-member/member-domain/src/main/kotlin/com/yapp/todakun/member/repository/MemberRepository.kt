package com.yapp.todakun.member.port

import com.yapp.todakun.member.Member
import java.util.UUID

interface MemberRepository {
    fun save(member: Member): Member

    fun findById(id: UUID): Member?
}
