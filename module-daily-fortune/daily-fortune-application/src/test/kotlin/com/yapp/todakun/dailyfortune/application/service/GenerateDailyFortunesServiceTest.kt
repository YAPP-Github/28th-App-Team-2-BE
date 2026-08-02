package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetMemberIdsPort
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID

private val MEMBER_1 = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val MEMBER_2 = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val DAILY_FORTUNE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")

// MEMBER_1/MEMBER_2(...0001/...0002)와 겹치지 않도록 1000부터 채번한다.
private fun generateFullPageMemberIds(): List<UUID> = List(PAGE_SIZE) { UUID.fromString("018f0000-0000-7000-8000-%012d".format(it + 1000)) }

class GenerateDailyFortunesServiceTest :
    DescribeSpec({
        val getMemberIdsPort = mockk<GetMemberIdsPort>()
        val createDailyFortunePort = mockk<CreateDailyFortunePort>()
        val service = GenerateDailyFortunesService(getMemberIdsPort, createDailyFortunePort)

        val fortuneDate = LocalDate.of(2026, 6, 24)

        afterTest { clearMocks(getMemberIdsPort, createDailyFortunePort) }

        describe("generate") {
            context("회원이 여러 명이면") {
                it("회원마다 오늘의 운세 생성을 요청한다") {
                    every { getMemberIdsPort.getMemberIds(null, PAGE_SIZE) } returns listOf(MEMBER_1, MEMBER_2)
                    every { getMemberIdsPort.getMemberIds(MEMBER_2, PAGE_SIZE) } returns emptyList()
                    every { createDailyFortunePort.create(any(), fortuneDate) } returns DAILY_FORTUNE_ID

                    service.generate(fortuneDate)

                    verify(exactly = 1) { createDailyFortunePort.create(MEMBER_1, fortuneDate) }
                    verify(exactly = 1) { createDailyFortunePort.create(MEMBER_2, fortuneDate) }
                }
            }

            context("회원 중 한 명의 오늘의 운세 생성이 실패하더라도") {
                it("나머지 회원의 오늘의 운세 생성을 이어간다") {
                    every { getMemberIdsPort.getMemberIds(null, PAGE_SIZE) } returns listOf(MEMBER_1, MEMBER_2)
                    every { getMemberIdsPort.getMemberIds(MEMBER_2, PAGE_SIZE) } returns emptyList()
                    every { createDailyFortunePort.create(MEMBER_1, fortuneDate) } throws DailyFortuneGenerationFailedException()
                    every { createDailyFortunePort.create(MEMBER_2, fortuneDate) } returns DAILY_FORTUNE_ID

                    service.generate(fortuneDate)

                    verify(exactly = 1) { createDailyFortunePort.create(MEMBER_2, fortuneDate) }
                }
            }

            context("회원 수가 페이지 크기를 넘으면") {
                it("다음 페이지를 이어서 조회한다") {
                    val fullPage = generateFullPageMemberIds()
                    every { getMemberIdsPort.getMemberIds(null, PAGE_SIZE) } returns fullPage
                    every { getMemberIdsPort.getMemberIds(fullPage.last(), PAGE_SIZE) } returns listOf(MEMBER_1)
                    every { getMemberIdsPort.getMemberIds(MEMBER_1, PAGE_SIZE) } returns emptyList()
                    every { createDailyFortunePort.create(any(), fortuneDate) } returns DAILY_FORTUNE_ID

                    service.generate(fortuneDate)

                    verify(exactly = 1) { getMemberIdsPort.getMemberIds(fullPage.last(), PAGE_SIZE) }
                    verify(exactly = 1) { createDailyFortunePort.create(MEMBER_1, fortuneDate) }
                }
            }
        }
    })
