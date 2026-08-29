package com.yapp.todakun.terms.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.terms.MemberTermsAgreement
import com.yapp.todakun.terms.TermsAgreementPolicy
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementCommand
import com.yapp.todakun.terms.port.inbound.SaveTermsAgreementUseCase
import com.yapp.todakun.terms.repository.MemberTermsAgreementRepository
import com.yapp.todakun.terms.repository.TermsRepository
import kotlin.uuid.ExperimentalUuidApi

@CommandService
class SaveTermsAgreementService(
    private val termsRepository: TermsRepository,
    private val memberTermsAgreementRepository: MemberTermsAgreementRepository,
) : SaveTermsAgreementUseCase {
    @ExperimentalUuidApi
    override fun save(command: SaveTermsAgreementCommand): List<MemberTermsAgreement> {
        val catalog = termsRepository.findAll()
        val submittedTermsIds = command.items.map { it.termsId }
        val agreedTermsIds = command.items.filter { it.agreed }.map { it.termsId }.toSet()

        TermsAgreementPolicy.validate(catalog, submittedTermsIds, agreedTermsIds)

        // 재제출 시 기존 결정을 갱신하고, 처음 제출된 약관은 새로 생성한다((member, terms) upsert).
        val existingByTermsId =
            memberTermsAgreementRepository
                .findAllByMemberId(command.memberId)
                .associateBy { it.termsId }

        val toSave =
            command.items.map { item ->
                existingByTermsId[item.termsId]?.updateDecision(item.agreed)
                    ?: MemberTermsAgreement.create(command.memberId, item.termsId, item.agreed)
            }

        return memberTermsAgreementRepository.saveAll(toSave)
    }
}
