package com.yapp.todakun.compatibility

import com.yapp.todakun.compatibility.exception.CompatibilityRelationshipTypeInvalidException

/**
 * 궁합을 비교한 시점의 관계 유형 스냅샷. 상대 명식의 관계 라벨을 복사해온다(이후 관계가 바뀌어도 이 값은 유지된다).
 * 화면 상단 배지에 [label](한글)로 표시된다.
 */
enum class CompatibilityRelationshipType(
    val label: String,
) {
    LOVER("연인"),
    FRIEND("친구"),
    FAMILY("가족"),
    COLLEAGUE("동료"),
    ETC("기타"),
    ;

    companion object {
        /** 상대 명식 관계 라벨 코드(LOVER/FRIEND/...)를 궁합 관계 유형으로 변환한다. */
        fun from(name: String): CompatibilityRelationshipType =
            entries.firstOrNull { it.name == name } ?: throw CompatibilityRelationshipTypeInvalidException()
    }
}
