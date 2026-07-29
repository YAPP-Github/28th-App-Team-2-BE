package com.yapp.todakun.shared

import java.util.UUID

interface GetMemberIdsPort {
    fun getMemberIds(): List<UUID>
}
