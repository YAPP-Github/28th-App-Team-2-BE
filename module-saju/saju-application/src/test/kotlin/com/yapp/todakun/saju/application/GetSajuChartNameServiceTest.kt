package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class GetSajuChartNameServiceTest : DescribeSpec({
    val sajuChartRepository = mockk<SajuChartRepository>()
    val service = GetSajuChartNameService(sajuChartRepository)

    afterTest { clearMocks(sajuChartRepository) }

    describe("getName") {
        context("명식이 있으면") {
            it("명식의 이름을 반환한다") {
                val chart = SajuFixture.chart(name = "토닥이")
                every { sajuChartRepository.findById(chart.id) } returns chart

                val result = service.getName(chart.id)

                result shouldBe "토닥이"
            }
        }

        context("명식이 없으면") {
            it("null을 반환한다") {
                every { sajuChartRepository.findById(SajuFixture.CHART_ID) } returns null

                val result = service.getName(SajuFixture.CHART_ID)

                result shouldBe null
            }
        }
    }
})
