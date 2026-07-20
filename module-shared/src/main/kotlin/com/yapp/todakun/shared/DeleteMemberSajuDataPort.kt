package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원이 소유한 사주 데이터(본인·상대 명식과 소유권 링크)를 일괄 삭제하는 크로스 도메인 포트.
 * 탈퇴 시 member가 saju 내부 구조를 알지 않고 이 포트로 위임한다(saju-application 구현).
 */
interface DeleteMemberSajuDataPort {
    fun deleteByMemberId(memberId: UUID)
}
