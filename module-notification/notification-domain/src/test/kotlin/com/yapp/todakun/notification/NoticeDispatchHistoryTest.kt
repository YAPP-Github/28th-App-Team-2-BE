package com.yapp.todakun.notification

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch

class NoticeDispatchHistoryTest :
    DescribeSpec({
        describe("deriveIdempotencyKey") {
            context("제목·본문·딥링크가 모두 같으면") {
                it("같은 키를 만든다") {
                    val key1 = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", null)
                    val key2 = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", null)

                    key1 shouldBe key2
                }
            }

            context("본문이 다르면") {
                it("다른 키를 만든다") {
                    val key1 = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문A", "notice/1", null)
                    val key2 = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문B", "notice/1", null)

                    key1 shouldNotBe key2
                }
            }

            context("필드 경계만 다른 제목·본문이면") {
                it("다른 키를 만든다(구분자 없이 이어 붙일 때 생기는 해시 충돌 방지)") {
                    val key1 = NoticeDispatchHistory.deriveIdempotencyKey("제목AB", "본문", "notice/1", null)
                    val key2 = NoticeDispatchHistory.deriveIdempotencyKey("제목", "AB본문", "notice/1", null)

                    key1 shouldNotBe key2
                }
            }

            context("딥링크가 null인 경우와 빈 문자열인 경우") {
                it("orEmpty로 병합되어 같은 키를 만든다(구현상 두 경우를 구분하지 않는다)") {
                    val keyWithNullDeepLink = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", null, null)
                    val keyWithBlankDeepLink = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "", null)

                    keyWithNullDeepLink shouldBe keyWithBlankDeepLink
                }
            }

            context("명시적 키를 주면") {
                it("같은 제목·본문·딥링크의 내용 파생 키와 다른 키를 만든다") {
                    val contentDerivedKey = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", null)
                    val explicitKey = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", "explicit-key")

                    explicitKey shouldNotBe contentDerivedKey
                }

                it("제목·본문·딥링크가 달라도 같은 명시적 키면 같은 결과를 만든다") {
                    val key1 = NoticeDispatchHistory.deriveIdempotencyKey("제목A", "본문A", "notice/1", "same-key")
                    val key2 = NoticeDispatchHistory.deriveIdempotencyKey("제목B", "본문B", "notice/2", "same-key")

                    key1 shouldBe key2
                }
            }

            context("명시적 키가 공백이면") {
                it("내용 파생 키로 폴백한다") {
                    val contentDerivedKey = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", null)
                    val blankExplicitKey = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", "   ")

                    blankExplicitKey shouldBe contentDerivedKey
                }
            }

            context("어떤 입력이 주어지든") {
                it("64자 소문자 hex 문자열을 반환한다") {
                    val key = NoticeDispatchHistory.deriveIdempotencyKey("제목", "본문", "notice/1", null)

                    key shouldHaveLength 64
                    key shouldMatch Regex("^[0-9a-f]{64}$")
                }
            }
        }
    })
