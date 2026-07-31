package com.yapp.todakun.web.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/** [ValidEnum]을 `List<String>` 필드에 직접 붙였을 때(원소별 검증) 쓰는 validator. null은 통과시킨다(@Size와 병행 사용). */
class ValidEnumListValidator : ConstraintValidator<ValidEnum, List<String>?> {
    private lateinit var acceptedValues: Set<String>

    override fun initialize(constraintAnnotation: ValidEnum) {
        acceptedValues = constraintAnnotation.enumClass.java.enumConstants.map { it.name }.toSet()
    }

    override fun isValid(
        value: List<String>?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || value.all { it in acceptedValues }
}
