package com.yapp.todakun.web.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * 문자열(또는 문자열 리스트의 각 원소)이 [enumClass]의 상수 이름(name) 중 하나인지 검증한다.
 * null은 통과시킨다(@NotBlank와 병행 사용). 리스트에 적용할 때는 제네릭 타입 인자가 아니라 필드에 직접 붙인다
 * (Kotlin이 TYPE_USE 애노테이션을 런타임에 보장하지 않아 컨테이너 원소 검증이 무시될 수 있음).
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidEnumValidator::class, ValidEnumListValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "올바른 값이 아닙니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
