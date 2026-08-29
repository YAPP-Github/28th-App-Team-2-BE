package com.yapp.todakun.saju

/**
 * 상대방 사주와 본인의 관계 라벨. `role=PARTNER`에서만 사용하며 언제든 수정 가능하다(SELF는 null).
 * "궁합 상대 선택" 목록의 관계 태그로 표시된다. [label]은 화면 표시용 한글.
 */
enum class RelationshipType(
    val label: String,
) {
    LOVER("연인"),
    FRIEND("친구"),
    FAMILY("가족"),
    COLLEAGUE("동료"),
    ETC("기타"),
}
