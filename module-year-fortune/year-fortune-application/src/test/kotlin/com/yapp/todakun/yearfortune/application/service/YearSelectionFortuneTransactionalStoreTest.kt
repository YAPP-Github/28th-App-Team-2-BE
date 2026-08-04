package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class YearSelectionFortuneTransactionalStoreTest :
    DescribeSpec({
        val yearSelectionFortuneRepository = mockk<YearSelectionFortuneRepository>()
        val store = YearSelectionFortuneTransactionalStore(yearSelectionFortuneRepository)

        val memberId = Uuid.generateV7().toJavaUuid()
        val year = 2026

        afterTest { clearMocks(yearSelectionFortuneRepository) }

        beforeTest { every { yearSelectionFortuneRepository.lock(memberId, year) } just Runs }

        describe("findExistingWithLock") {
            it("(memberId, year) 락을 선점한 뒤 조회한다") {
                val existing = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns existing

                val result = store.findExistingWithLock(memberId, year)

                result shouldBe existing
                verifyOrder {
                    yearSelectionFortuneRepository.lock(memberId, year)
                    yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                }
            }
        }

        describe("saveIfAbsent") {
            context("락 재획득 후 재조회에서 이미 생성된 결과가 있으면") {
                it("저장하지 않고 기존 결과를 반환한다(멱등)") {
                    val existing = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns existing

                    val result = store.saveIfAbsent(existing)

                    result shouldBe existing
                    verify(exactly = 0) { yearSelectionFortuneRepository.save(any()) }
                    verifyOrder {
                        yearSelectionFortuneRepository.lock(memberId, year)
                        yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                    }
                }
            }

            context("재조회에서 결과가 없으면") {
                it("락을 잡고 저장한다") {
                    val toSave = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns null
                    every { yearSelectionFortuneRepository.save(toSave) } returns toSave

                    val result = store.saveIfAbsent(toSave)

                    result shouldBe toSave
                    verify(exactly = 1) { yearSelectionFortuneRepository.save(toSave) }
                }
            }
        }
    })
