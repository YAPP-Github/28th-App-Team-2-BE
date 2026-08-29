package com.yapp.todakun.saju.adapter.persistence

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.Gender
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.config.TestContainersConfig
import com.yapp.todakun.saju.port.outbound.FourPillars
import com.yapp.todakun.saju.port.outbound.GanjiPillar
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class SajuChartRepositoryAdapterTest(
    private val chartJpaRepository: SajuChartJpaRepository,
    private val pillarJpaRepository: SajuPillarJpaRepository,
    private val ohaengJpaRepository: SajuOhaengJpaRepository,
    private val sipseongJpaRepository: SajuSipseongJpaRepository,
) : DescribeSpec(
        {
            val adapter =
                SajuChartRepositoryAdapter(chartJpaRepository, pillarJpaRepository, ohaengJpaRepository, sipseongJpaRepository)

            fun newChart() =
                SajuChart.create(
                    name = "토닥이",
                    gender = Gender.FEMALE,
                    calendarType = CalendarType.SOLAR,
                    birthDate = LocalDate.of(2001, 5, 30),
                    birthTime = BirthTime.MISI,
                    isLeapMonth = false,
                    fourPillars =
                        FourPillars(
                            year = GanjiPillar(HeavenlyStem.SIN, EarthlyBranch.SA),
                            month = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                            day = GanjiPillar(HeavenlyStem.GYE, EarthlyBranch.SA),
                            hour = GanjiPillar(HeavenlyStem.GI, EarthlyBranch.MI),
                            solarTermName = "입하",
                        ),
                )

            describe("save & findById") {
                context("명식을 저장하면") {
                    it("헤더·4주·오행·십성을 복원해 조회한다") {
                        val chart = newChart()

                        adapter.save(chart)
                        val found = adapter.findById(chart.id)

                        found.shouldNotBeNull()
                        found.id shouldBe chart.id
                        found.name shouldBe "토닥이"
                        found.dayMaster shouldBe HeavenlyStem.GYE
                        found.pillars.size shouldBe 4
                        found.ohaeng.size shouldBe 5
                        found.sipseong.size shouldBe 10
                    }
                }
            }

            describe("findSummariesByIds") {
                context("여러 명식을 저장한 뒤 id 목록으로 조회하면") {
                    it("자식(4주·오행·십성) 조회 없이 헤더만 담긴 요약을 반환한다") {
                        val chart1 = newChart()
                        val chart2 = newChart()
                        adapter.save(chart1)
                        adapter.save(chart2)

                        val summaries = adapter.findSummariesByIds(listOf(chart1.id, chart2.id))

                        summaries.map { it.id } shouldContainExactlyInAnyOrder listOf(chart1.id, chart2.id)
                        summaries.first { it.id == chart1.id }.name shouldBe chart1.name
                    }
                }

                context("존재하지 않는 id가 섞여 있으면") {
                    it("존재하는 명식의 요약만 반환한다") {
                        val chart = newChart()
                        adapter.save(chart)

                        val summaries = adapter.findSummariesByIds(listOf(chart.id, Uuid.generateV7().toJavaUuid()))

                        summaries.map { it.id } shouldBe listOf(chart.id)
                    }
                }

                context("빈 id 목록으로 조회하면") {
                    it("빈 리스트를 반환한다") {
                        adapter.findSummariesByIds(emptyList()) shouldBe emptyList()
                    }
                }
            }

            describe("deleteById") {
                context("명식을 삭제하면") {
                    it("헤더와 자식(4주·오행·십성)이 모두 사라진다") {
                        val chart = newChart()
                        adapter.save(chart)

                        adapter.deleteById(chart.id)

                        adapter.findById(chart.id).shouldBeNull()
                        pillarJpaRepository.findByChartId(chart.id).size shouldBe 0
                        ohaengJpaRepository.findByChartId(chart.id).size shouldBe 0
                        sipseongJpaRepository.findByChartId(chart.id).size shouldBe 0
                    }
                }
            }
        },
    )
