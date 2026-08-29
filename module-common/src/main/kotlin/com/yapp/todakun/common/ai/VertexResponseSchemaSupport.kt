package com.yapp.todakun.common.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.model.ModelOptionsUtils

/**
 * [BeanOutputConverter]는 JSON Schema 표준대로 소문자 `type`을 내지만, Vertex의 `Schema` proto는 대문자 enum만 인식하고
 * 소문자는 `ignoringUnknownFields()`가 예외 없이 조용히 버린다(→ `TYPE_UNSPECIFIED`).
 * spring-ai가 같은 목적으로 공개해 둔 [ModelOptionsUtils.toUpperCaseTypeValues]로 올려서 `responseSchema`에 타입 정보가 실제로 전달되게 한다.
 */
fun vertexResponseSchema(type: Class<*>): String =
    (ObjectMapper().readTree(BeanOutputConverter(type).jsonSchema) as ObjectNode)
        .also { ModelOptionsUtils.toUpperCaseTypeValues(it) }
        .toString()
