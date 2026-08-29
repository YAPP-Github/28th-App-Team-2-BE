package com.yapp.todakun.chat.adapter.ai

import com.yapp.todakun.chat.ChatActionType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 8, 19)

private fun rawChatAction(
    hasAction: Boolean = true,
    type: ChatActionType? = ChatActionType.CALENDAR_ADD,
    label: String? = "내 캘린더에 추가하기",
    category: String? = "계약・이사",
    date: String? = "2026-08-25",
): RawChatAction =
    RawChatAction(
        hasAction = hasAction,
        type = type,
        label = label,
        category = category,
        date = date,
    )

class RawChatActionTest : DescribeSpec({
    describe("toDomainOrNull") {
        context("모든 필드가 유효하고 날짜가 오늘 이후면") {
            it("액션 카드로 변환한다") {
                val action = rawChatAction().toDomainOrNull(TODAY)

                action?.type shouldBe ChatActionType.CALENDAR_ADD
                action?.date shouldBe LocalDate.of(2026, 8, 25)
            }
        }

        context("날짜가 오늘이면") {
            it("당일 일정도 캘린더에 담을 수 있으므로 액션 카드로 변환한다") {
                val action = rawChatAction(date = TODAY.toString()).toDomainOrNull(TODAY)

                action?.date shouldBe TODAY
            }
        }

        context("모델이 과거 날짜를 반환하면") {
            it("캘린더에 담을 수 없으므로 액션 카드 없음으로 처리한다") {
                // 모델이 학습 데이터에 이끌려 지난 연도를 "올해"로 착각하고 날짜를 만들어내는 경우를 방어한다.
                rawChatAction(date = "2024-08-25").toDomainOrNull(TODAY).shouldBeNull()
                rawChatAction(date = TODAY.minusDays(1).toString()).toDomainOrNull(TODAY).shouldBeNull()
            }
        }

        context("hasAction이 false면") {
            it("null을 반환한다") {
                rawChatAction(hasAction = false).toDomainOrNull(TODAY).shouldBeNull()
            }
        }

        context("날짜 형식이 yyyy-MM-dd를 벗어나면") {
            it("null을 반환한다") {
                rawChatAction(date = "2026년 8월 25일").toDomainOrNull(TODAY).shouldBeNull()
            }
        }

        context("필수 필드가 비어 있으면") {
            it("null을 반환한다") {
                rawChatAction(type = null).toDomainOrNull(TODAY).shouldBeNull()
                rawChatAction(label = " ").toDomainOrNull(TODAY).shouldBeNull()
                rawChatAction(category = null).toDomainOrNull(TODAY).shouldBeNull()
                rawChatAction(date = null).toDomainOrNull(TODAY).shouldBeNull()
            }
        }
    }
})
