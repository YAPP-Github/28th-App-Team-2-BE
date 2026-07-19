package com.yapp.todakun.saju.adapter.manseryeok

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.exception.SajuInputInvalidException
import com.yapp.todakun.saju.exception.SajuYearOutOfRangeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ManseryeokAdapterTest : DescribeSpec({
    val adapter = ManseryeokAdapter()

    // 양력 2001-05-30 (미시) — manseryeok-js 검증값: 년 辛巳 / 월 癸巳(입하) / 일 癸巳 / 시 己未
    // (지침 문서의 己巳/辛未는 placeholder 오류였고, 라이브러리 3개 API가 모두 위 값으로 일치)
    describe("calculate - 양력 4주(라이브러리 검증값)") {
        val result =
            adapter.calculate(LocalDate.of(2001, 5, 30), BirthTime.MISI, CalendarType.SOLAR, false)

        it("년주는 辛巳(신사년)") {
            result.year.stem shouldBe HeavenlyStem.SIN
            result.year.branch shouldBe EarthlyBranch.SA
        }
        it("월주는 癸巳, 절기는 입하") {
            result.month.stem shouldBe HeavenlyStem.GYE
            result.month.branch shouldBe EarthlyBranch.SA
            result.solarTermName shouldBe "입하"
        }
        it("일주는 癸巳(일간 癸)") {
            result.day.stem shouldBe HeavenlyStem.GYE
            result.day.branch shouldBe EarthlyBranch.SA
        }
        it("시주는 己未(癸일간 미시, 오서둔)") {
            result.hour?.stem shouldBe HeavenlyStem.GI
            result.hour?.branch shouldBe EarthlyBranch.MI
        }
    }

    describe("calculate - 시간 모름") {
        it("시주를 계산하지 않는다") {
            val result =
                adapter.calculate(LocalDate.of(2001, 5, 30), BirthTime.UNKNOWN, CalendarType.SOLAR, false)
            result.hour.shouldBeNull()
        }
    }

    describe("calculate - 음력 입력") {
        it("음력을 양력으로 변환해 4주를 계산한다 (음력 2001 윤4월 8일 = 양력 2001-05-30)") {
            // 양력 2001-05-30 = 음력 2001년 윤4월(閏4月) 8일 → 일주 癸巳, 시주 己未
            val result =
                adapter.calculate(LocalDate.of(2001, 4, 8), BirthTime.MISI, CalendarType.LUNAR, true)
            result.day.stem shouldBe HeavenlyStem.GYE
            result.day.branch shouldBe EarthlyBranch.SA
            result.hour?.stem shouldBe HeavenlyStem.GI
        }

        it("평달과 윤달은 다른 양력으로 변환된다 (음력 2001 평4월 8일 = 양력 2001-05-01)") {
            val result =
                adapter.calculate(LocalDate.of(2001, 4, 8), BirthTime.MISI, CalendarType.LUNAR, false)
            (result.day.stem == HeavenlyStem.GYE && result.day.branch == EarthlyBranch.SA) shouldBe false
        }

        it("유효하지 않은 음력 날짜(존재하지 않는 윤달)는 입력 예외") {
            shouldThrow<SajuInputInvalidException> {
                // 2001년 윤달은 4월뿐 → 윤5월은 존재하지 않음
                adapter.calculate(LocalDate.of(2001, 5, 8), BirthTime.MISI, CalendarType.LUNAR, true)
            }
        }
    }

    describe("calculate - 입춘 경계(절기 기준)") {
        it("입춘 이전(2001-01-20) 출생은 전년도(2000 경진년) 간지로 계산한다") {
            val result = adapter.calculate(LocalDate.of(2001, 1, 20), BirthTime.OSI, CalendarType.SOLAR, false)
            result.year.stem shouldBe HeavenlyStem.GYEONG
            result.year.branch shouldBe EarthlyBranch.JIN
        }
    }

    describe("calculate - 예외 처리") {
        it("지원 범위 밖 연도는 범위 초과 예외") {
            shouldThrow<SajuYearOutOfRangeException> {
                adapter.calculate(LocalDate.of(1899, 5, 30), BirthTime.MISI, CalendarType.SOLAR, false)
            }
        }
    }
})
