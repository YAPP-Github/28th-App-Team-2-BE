package com.yapp.todakun.shared

import java.util.UUID

interface CreateMemberPort {
    fun createMember(command: CreateMemberCommand): UUID
}
