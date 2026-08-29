package com.yapp.todakun.luck.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.luck.exception.LuckActionNotFoundException
import com.yapp.todakun.luck.port.inbound.ToggleLuckActionUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import org.springframework.cache.annotation.CacheEvict
import java.util.UUID

@CommandService
class ToggleLuckActionService(
    private val luckActionRepository: LuckActionRepository,
) : ToggleLuckActionUseCase {
    // achieved는 예외적으로 가변인 필드라 LUCK_ACTIONS 캐시를 TTL 만료 전에 갱신해야 한다(이슈 #56).
    @CacheEvict(cacheNames = [CacheNames.LUCK_ACTIONS], key = "#memberId + ':' + #result.fortuneDate")
    override fun toggle(
        id: UUID,
        memberId: UUID,
    ): LuckAction {
        val luckAction = luckActionRepository.findById(id) ?: throw LuckActionNotFoundException()

        luckAction.validateOwner(memberId)

        return luckActionRepository.save(luckAction.toggle())
    }
}
