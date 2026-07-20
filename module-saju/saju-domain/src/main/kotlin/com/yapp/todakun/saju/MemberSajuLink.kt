package com.yapp.todakun.saju

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원([memberId])과 사주 명식([chartId])의 소유권 연결. 명식(계산 결과)과 소유권을 분리해
 * saju 도메인이 차트 소유를 관리한다. [role]로 본인/상대를 구분하고, PARTNER만 [relationshipType]을 가진다.
 * [memberId]는 단순 UUID 참조라 크로스 도메인 엔티티 참조가 아니다.
 */
data class MemberSajuLink(
    val id: UUID,
    val memberId: UUID,
    val chartId: UUID,
    val role: SajuRole,
    val relationshipType: RelationshipType?,
) {
    /** PARTNER 링크의 관계 라벨·연결 명식을 교체한 새 링크를 반환한다(불변). */
    fun updated(
        chartId: UUID,
        relationshipType: RelationshipType?,
    ): MemberSajuLink = copy(chartId = chartId, relationshipType = relationshipType)

    companion object {
        /** 본인 명식 연결(role=SELF, relationshipType=null)을 생성한다. */
        @ExperimentalUuidApi
        fun self(
            memberId: UUID,
            chartId: UUID,
        ): MemberSajuLink =
            MemberSajuLink(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                chartId = chartId,
                role = SajuRole.SELF,
                relationshipType = null,
            )

        /** 상대 명식 연결(role=PARTNER)을 생성한다. */
        @ExperimentalUuidApi
        fun partner(
            memberId: UUID,
            chartId: UUID,
            relationshipType: RelationshipType,
        ): MemberSajuLink =
            MemberSajuLink(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                chartId = chartId,
                role = SajuRole.PARTNER,
                relationshipType = relationshipType,
            )

        /** 영속 계층에서 저장된 값으로 링크를 복원한다. */
        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            chartId: UUID,
            role: SajuRole,
            relationshipType: RelationshipType?,
        ): MemberSajuLink =
            MemberSajuLink(
                id = id,
                memberId = memberId,
                chartId = chartId,
                role = role,
                relationshipType = relationshipType,
            )
    }
}
