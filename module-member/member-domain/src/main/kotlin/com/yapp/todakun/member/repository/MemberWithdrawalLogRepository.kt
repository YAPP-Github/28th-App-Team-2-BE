package com.yapp.todakun.member.repository

import com.yapp.todakun.member.MemberWithdrawalLog

/** 회원 탈퇴 사유 로그 영속화 아웃바운드 포트. */
interface MemberWithdrawalLogRepository {
    fun save(log: MemberWithdrawalLog): MemberWithdrawalLog
}
