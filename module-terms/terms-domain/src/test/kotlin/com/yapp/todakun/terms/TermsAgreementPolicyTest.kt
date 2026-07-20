package com.yapp.todakun.terms

import com.yapp.todakun.terms.exception.DuplicateTermsAgreementException
import com.yapp.todakun.terms.exception.RequiredTermsNotAgreedException
import com.yapp.todakun.terms.exception.TermsNotFoundException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import java.util.UUID

class TermsAgreementPolicyTest :
    DescribeSpec(
        {
            val serviceId = UUID.fromString("018f0000-0000-7000-8000-0000000000a1")
            val privacyId = UUID.fromString("018f0000-0000-7000-8000-0000000000a2")
            val marketingId = UUID.fromString("018f0000-0000-7000-8000-0000000000a3")
            val unknownId = UUID.fromString("018f0000-0000-7000-8000-0000000000ff")

            val catalog =
                listOf(
                    Terms.reconstitute(serviceId, TermsType.SERVICE, "서비스 이용약관", required = true),
                    Terms.reconstitute(privacyId, TermsType.PRIVACY, "개인정보 수집·이용 동의", required = true),
                    Terms.reconstitute(marketingId, TermsType.MARKETING, "마케팅 정보 수신 동의", required = false),
                )

            describe("validate") {
                context("모든 필수 약관에 동의하고 제출된 약관이 카탈로그에 존재하면") {
                    it("예외를 던지지 않는다") {
                        shouldNotThrowAny {
                            TermsAgreementPolicy.validate(
                                catalog = catalog,
                                submittedTermsIds = listOf(serviceId, privacyId, marketingId),
                                agreedTermsIds = setOf(serviceId, privacyId),
                            )
                        }
                    }
                }

                context("같은 약관 ID가 중복되면") {
                    it("DuplicateTermsAgreementException을 던진다") {
                        shouldThrow<DuplicateTermsAgreementException> {
                            TermsAgreementPolicy.validate(
                                catalog = catalog,
                                submittedTermsIds = listOf(serviceId, privacyId, serviceId),
                                agreedTermsIds = setOf(serviceId, privacyId),
                            )
                        }
                    }
                }

                context("존재하지 않는 약관 ID가 포함되면") {
                    it("TermsNotFoundException을 던진다") {
                        shouldThrow<TermsNotFoundException> {
                            TermsAgreementPolicy.validate(
                                catalog = catalog,
                                submittedTermsIds = listOf(serviceId, unknownId),
                                agreedTermsIds = setOf(serviceId, unknownId),
                            )
                        }
                    }
                }

                context("필수 약관에 동의하지 않으면") {
                    it("RequiredTermsNotAgreedException을 던진다") {
                        shouldThrow<RequiredTermsNotAgreedException> {
                            TermsAgreementPolicy.validate(
                                catalog = catalog,
                                submittedTermsIds = listOf(serviceId, privacyId),
                                agreedTermsIds = setOf(serviceId),
                            )
                        }
                    }
                }
            }
        },
    )
