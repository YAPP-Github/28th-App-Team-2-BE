package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val LUCK_ACTION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000005")

// 재조회 결과와 저장 후보를 구분해, 구현이 재조회 결과 대신 입력값을 반환해도 테스트가 통과하지 않도록 한다.
private val EXISTING_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000d1")
private val CANDIDATE_ID = UUID.fromString("018f0000-0000-7000-8000-0000000000d2")

@ExperimentalUuidApi
class DailyFortuneTransactionalStoreTest :
    DescribeSpec({
        val dailyFortuneRepository = mockk<DailyFortuneRepository>()
        val createLuckActionPort = mockk<CreateLuckActionPort>()
        val store = DailyFortuneTransactionalStore(dailyFortuneRepository, createLuckActionPort)

        val fortuneDate = LocalDate.of(2026, 6, 24)

        afterTest { clearMocks(dailyFortuneRepository, createLuckActionPort) }

        beforeTest { every { dailyFortuneRepository.lock(MEMBER_ID, fortuneDate) } just Runs }

        describe("findExistingWithLock") {
            it("(memberId, fortuneDate) 락을 선점한 뒤 조회한다") {
                val existing = DailyFortuneFixture.create(memberId = MEMBER_ID, fortuneDate = fortuneDate)
                every { dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate) } returns existing

                val result = store.findExistingWithLock(MEMBER_ID, fortuneDate)

                result shouldBe existing
                verifyOrder {
                    dailyFortuneRepository.lock(MEMBER_ID, fortuneDate)
                    dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate)
                }
            }
        }

        describe("saveIfAbsent") {
            context("락 재획득 후 재조회에서 이미 생성된 결과가 있으면") {
                it("저장·LuckAction 생성 없이 재조회된 기존 ID를 반환한다(멱등)") {
                    val candidate = DailyFortuneFixture.create(id = CANDIDATE_ID, memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    val existing = DailyFortuneFixture.create(id = EXISTING_ID, memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    every { dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate) } returns existing

                    val result = store.saveIfAbsent(candidate, categoryFortunes())

                    result shouldBe EXISTING_ID
                    verify(exactly = 0) { dailyFortuneRepository.save(any()) }
                    verify(exactly = 0) { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) }
                    verifyOrder {
                        dailyFortuneRepository.lock(MEMBER_ID, fortuneDate)
                        dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate)
                    }
                }
            }

            context("재조회에서 결과가 없으면") {
                it("락을 잡고 DailyFortune과 카테고리별 LuckAction을 함께 저장한다") {
                    val toSave = DailyFortuneFixture.create(id = CANDIDATE_ID, memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    every { dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate) } returns null
                    every { dailyFortuneRepository.save(toSave) } returns toSave
                    every { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) } returns LUCK_ACTION_ID

                    val result = store.saveIfAbsent(toSave, categoryFortunes())

                    result shouldBe toSave.id
                    verify(exactly = 1) { dailyFortuneRepository.save(toSave) }
                    verify(
                        exactly = FortuneCategory.entries.size,
                    ) { createLuckActionPort.create(MEMBER_ID, any(), fortuneDate, any(), any(), any()) }
                    verifyOrder {
                        dailyFortuneRepository.lock(MEMBER_ID, fortuneDate)
                        dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, fortuneDate)
                        dailyFortuneRepository.save(toSave)
                    }
                }
            }
        }
    })

private fun categoryFortunes(): List<GeneratedCategoryFortune> =
    FortuneCategory.entries.map {
        GeneratedCategoryFortune(
            fortuneCategory = it,
            score = 70,
            title = "오늘의 액션",
            content = "상세 해석",
        )
    }
