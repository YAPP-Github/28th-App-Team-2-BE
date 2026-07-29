package com.yapp.todakun.compatibility

import com.yapp.todakun.compatibility.exception.CompatibilityOhaengCountNegativeException
import com.yapp.todakun.compatibility.exception.CompatibilityOhaengEmptyException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CompatibilityOhaengCalculatorTest : DescribeSpec({
    describe("combine") {
        context("두 명식의 오행 글자 수가 주어지면") {
            it("5개 오행을 enum 순서로 반환하고 비율 합계가 정확히 100이다") {
                val my = mapOf("WOOD" to 3, "FIRE" to 2, "EARTH" to 2, "METAL" to 1)
                val partner = mapOf("FIRE" to 4, "WATER" to 3)

                val result = CompatibilityOhaengCalculator.combine(my, partner)

                result.map { it.element } shouldBe CompatibilityElement.entries.toList()
                result.sumOf { it.percentage } shouldBe 100
            }
        }

        context("한쪽에만 있는 오행은 0으로 합산한다") {
            it("두 명식 모두 없는 오행 비율은 0이다") {
                val my = mapOf("WOOD" to 2)
                val partner = mapOf("WOOD" to 2)

                val result = CompatibilityOhaengCalculator.combine(my, partner)

                result.first { it.element == CompatibilityElement.WOOD }.percentage shouldBe 100
                result.first { it.element == CompatibilityElement.FIRE }.percentage shouldBe 0
            }
        }

        context("두 명식의 오행 글자 수 합이 0이면") {
            it("CompatibilityOhaengEmptyException을 던진다") {
                shouldThrow<CompatibilityOhaengEmptyException> {
                    CompatibilityOhaengCalculator.combine(emptyMap(), emptyMap())
                }
            }
        }

        context("오행 글자 수에 음수가 포함되면") {
            it("CompatibilityOhaengCountNegativeException을 던진다") {
                shouldThrow<CompatibilityOhaengCountNegativeException> {
                    CompatibilityOhaengCalculator.combine(mapOf("WOOD" to -1), mapOf("FIRE" to 2))
                }
            }
        }
    }
})
