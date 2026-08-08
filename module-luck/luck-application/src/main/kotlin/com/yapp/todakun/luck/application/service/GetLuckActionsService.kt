package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.port.inbound.GetLuckActionsUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import org.springframework.cache.annotation.Cacheable
import java.time.LocalDate
import java.util.UUID

@QueryService
class GetLuckActionsService(
    private val luckActionRepository: LuckActionRepository,
) : GetLuckActionsUseCase {
    // 목록성 데이터, 갱신 빈도 낮음 — evict 없이 TTL만으로 무효화한다(이슈 #56).
    @Cacheable(cacheNames = [CacheNames.LUCK_ACTIONS], key = "#memberId + ':' + #fortuneDate")
    override fun getTodayLuckActions(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): List<LuckAction> = luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, fortuneDate)
}
