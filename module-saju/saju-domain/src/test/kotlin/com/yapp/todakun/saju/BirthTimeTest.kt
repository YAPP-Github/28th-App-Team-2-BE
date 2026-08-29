package com.yapp.todakun.saju

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class BirthTimeTest : DescribeSpec({
    describe("calculationBranch") {
        it("입력된 시진은 그 시진의 지지를 그대로 쓴다") {
            BirthTime.MISI.calculationBranch shouldBe EarthlyBranch.MI
        }

        it("시간 모름은 00:00이 속한 자시(子時)로 계산한다") {
            BirthTime.UNKNOWN.calculationBranch shouldBe EarthlyBranch.JA
        }

        it("시간 모름의 입력값(branch)은 여전히 없다 — 저장·표시에서 '모름'을 구분한다") {
            BirthTime.UNKNOWN.branch.shouldBeNull()
        }
    }
})
