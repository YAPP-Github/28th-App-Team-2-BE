package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationInProgressException
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneGenerationLockPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dailyfortune.port.outbound.Pillar
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 크로스 도메인 포트 [CreateDailyFortunePort] 구현체(오케스트레이터). 트랜잭션을 걸지 않는다 — 외부 AI 호출을 DB 트랜잭션 밖에서 수행하기 위함이다.
 * (선조회 → 생성 잠금 → AI 생성 → 저장) 각 DB 단계는 [DailyFortuneTransactionalStore]의 독립 트랜잭션으로 위임한다.
 * 1) [DailyFortuneTransactionalStore.findExistingWithLock]로 락+선조회(멱등: 이미 생성된 날짜면 AI를 재호출하지 않는다).
 * 2) [DailyFortuneGenerationLockPort]로 DB와 무관한 생성 잠금을 선점한다(가입 직후 백그라운드 리스너와 홈 화면 자가 치유가 같은 조합을 동시에 만드는 걸 막는다.
 * 3) 선점에 실패하면 [DailyFortuneGenerationInProgressException]을 던진다.
 * 4) 트랜잭션 밖에서 AI를 호출하고 카테고리 유효성 검증 후 도메인 엔티티를 조립한다.
 * 5) [DailyFortuneTransactionalStore.saveIfAbsent]로 락+재조회 후 DailyFortune·LuckAction을 멱등 저장한다(동시 요청 시 유니크 제약 충돌 방지).
 * 생성 잠금은 성공/실패와 무관하게 항상 해제한다.
 */
@Service
class CreateDailyFortuneService(
    private val dailyFortuneTransactionalStore: DailyFortuneTransactionalStore,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getDailyPillarPort: GetDailyPillarPort,
    private val dailyFortuneAiPort: DailyFortuneAiPort,
    private val dailyFortuneGenerationLockPort: DailyFortuneGenerationLockPort,
) : CreateDailyFortunePort {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): UUID {
        dailyFortuneTransactionalStore.findExistingWithLock(memberId, fortuneDate)?.let { return it.id }

        // 이미 다른 호출자(배치/자가 치유/가입 직후 리스너 등)가 같은 조합을 생성 중이면 AI를 중복 호출하지 않는다.
        val lockToken =
            dailyFortuneGenerationLockPort.tryAcquire(memberId, fortuneDate)
                ?: throw DailyFortuneGenerationInProgressException()

        try {
            val generated = requestGeneration(memberId, fortuneDate)
            validateCategoryFortunes(generated)

            val dailyFortune =
                DailyFortune.create(
                    memberId = memberId,
                    fortuneDate = fortuneDate,
                    categoryScores = generated.categoryFortunes.map { it.score },
                    title = generated.title,
                    content = generated.content,
                    luckyItems = generated.luckyItems,
                    cautionaryItems = generated.cautionaryItems,
                )

            return dailyFortuneTransactionalStore.saveIfAbsent(dailyFortune, generated.categoryFortunes)
        } finally {
            dailyFortuneGenerationLockPort.release(memberId, fortuneDate, lockToken)
        }
    }

    private fun requestGeneration(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): GeneratedDailyFortune {
        val profile = buildProfile(memberId)
        val todayPillar = getDailyPillarPort.getPillar(fortuneDate).toPillar()

        return dailyFortuneAiPort.generate(profile, fortuneDate, todayPillar)
    }

    private fun buildProfile(memberId: UUID): MemberSajuProfile {
        val memberProfile = getMemberFortuneProfilePort.getProfile(memberId)
        val chart = getSajuChartPort.getChart(memberId)

        return MemberSajuProfile(
            birthDate = memberProfile.birthDate,
            gender = memberProfile.gender,
            job = memberProfile.job,
            relationshipStatus = memberProfile.relationshipStatus,
            fortuneCategories = FortuneCategory.entries.toList(),
            dayMaster = chart.dayMaster,
            yearPillar = chart.yearPillar.toPillar(),
            monthPillar = chart.monthPillar.toPillar(),
            dayPillar = chart.dayPillar.toPillar(),
            hourPillar = chart.hourPillar?.toPillar(),
            ohaeng = chart.ohaeng,
            sipseong = chart.sipseong,
        )
    }

    /** AI가 5개 카테고리를 정확히 1개씩 채웠는지 검증한다(누락·중복 시 그대로 저장하면 LuckAction이 어긋난다). */
    private fun validateCategoryFortunes(generated: GeneratedDailyFortune) {
        val categories = generated.categoryFortunes.map { it.fortuneCategory }

        if (categories.size != FortuneCategory.entries.size || categories.toSet() != FortuneCategory.entries.toSet()) {
            throw DailyFortuneGenerationFailedException()
        }
    }

    private fun PillarSummary.toPillar(): Pillar =
        Pillar(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )
}
