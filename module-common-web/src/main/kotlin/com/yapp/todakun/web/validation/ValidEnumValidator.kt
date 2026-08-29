package com.yapp.todakun.web.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidEnumValidator : ConstraintValidator<ValidEnum, String> {
    private lateinit var acceptedValues: Set<String>

    override fun initialize(constraintAnnotation: ValidEnum) {
        acceptedValues = constraintAnnotation.enumClass.java.enumConstants.map { it.name }.toSet()
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || value in acceptedValues
}
