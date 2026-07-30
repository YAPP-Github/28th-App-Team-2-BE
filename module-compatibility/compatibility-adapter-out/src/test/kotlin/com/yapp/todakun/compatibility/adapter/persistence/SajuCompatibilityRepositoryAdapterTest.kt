package com.yapp.todakun.compatibility.adapter.persistence

import com.yapp.todakun.compatibility.config.TestContainersConfig
import com.yapp.todakun.compatibility.fixture.CompatibilityFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainersConfig::class)
class SajuCompatibilityRepositoryAdapterTest(
    private val sajuCompatibilityJpaRepository: SajuCompatibilityJpaRepository,
) : DescribeSpec(
        {
            val adapter = SajuCompatibilityRepositoryAdapter(sajuCompatibilityJpaRepository)

            fun savedCompatibility() =
                sajuCompatibilityJpaRepository
                    .save(SajuCompatibilityJpaEntity.fromDomain(CompatibilityFixture.create()))
                    .toDomain()

            describe("findByMemberIdAndCharts") {
                context("저장된 (memberId, myChartId, partnerChartId)로 조회하면") {
                    it("해당 궁합을 오행 5개와 함께 반환한다") {
                        val compatibility = savedCompatibility()

                        val found =
                            adapter.findByMemberIdAndCharts(
                                compatibility.memberId,
                                compatibility.myChartId,
                                compatibility.partnerChartId,
                            )

                        found.shouldNotBeNull()
                        found shouldBe compatibility
                        found.ohaengs.size shouldBe 5
                    }
                }

                context("일치하는 조합이 없으면") {
                    it("null을 반환한다") {
                        val nonExistentMemberId = UUID.fromString("018f0000-0000-7000-8000-0000000000ff")

                        val found =
                            adapter.findByMemberIdAndCharts(
                                nonExistentMemberId,
                                CompatibilityFixture.MY_CHART_ID,
                                CompatibilityFixture.PARTNER_CHART_ID,
                            )

                        found.shouldBeNull()
                    }
                }
            }

            describe("lock") {
                context("같은 트랜잭션 내에서 동일한 (myChartId, partnerChartId)로 재호출하면") {
                    it("재진입 락이므로 예외 없이 통과한다") {
                        adapter.lock(CompatibilityFixture.MY_CHART_ID, CompatibilityFixture.PARTNER_CHART_ID)
                        adapter.lock(CompatibilityFixture.MY_CHART_ID, CompatibilityFixture.PARTNER_CHART_ID)
                    }
                }
            }
        },
    )
