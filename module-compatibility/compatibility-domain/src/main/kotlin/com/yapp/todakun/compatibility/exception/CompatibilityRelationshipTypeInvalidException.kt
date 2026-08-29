package com.yapp.todakun.compatibility.exception

import com.yapp.todakun.common.exception.BusinessException
import com.yapp.todakun.compatibility.code.CompatibilityErrorCode

/** 상대 명식 관계 라벨을 궁합 관계 유형으로 변환하지 못한 경우(400). */
class CompatibilityRelationshipTypeInvalidException :
    BusinessException(CompatibilityErrorCode.COMPATIBILITY_RELATIONSHIP_TYPE_INVALID)
