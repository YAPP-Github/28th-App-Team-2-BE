package com.yapp.todakun.saju.adapter.web.dto.response

import com.yapp.todakun.saju.EarthlyBranch
import com.yapp.todakun.saju.Element
import com.yapp.todakun.saju.HeavenlyStem
import com.yapp.todakun.saju.OhaengCount
import com.yapp.todakun.saju.PillarDetail
import com.yapp.todakun.saju.RelationshipType
import com.yapp.todakun.saju.Sibiunseong
import com.yapp.todakun.saju.Sinsal
import com.yapp.todakun.saju.Sipseong
import com.yapp.todakun.saju.SipseongCount
import com.yapp.todakun.saju.port.inbound.SajuChartDetail
import java.time.LocalDate
import java.util.UUID

/** 만세력 상세 응답. 사주원국(4주) + 오행/십성 분포 + 파생(지장간·십이신살)을 담는다. */
data class SajuChartDetailResponse(
    val linkId: UUID,
    val role: String,
    val relationshipType: RelationshipTypeResponse?,
    val name: String?,
    val gender: String,
    val birthDate: LocalDate,
    val calendarType: String,
    val birthTime: String,
    val isTimeUnknown: Boolean,
    val dayMaster: StemResponse,
    val pillars: List<PillarResponse>,
    val ohaeng: List<OhaengResponse>,
    val sipseong: List<SipseongCountResponse>,
) {
    companion object {
        fun from(detail: SajuChartDetail): SajuChartDetailResponse =
            SajuChartDetailResponse(
                linkId = detail.linkId,
                role = detail.role.name,
                relationshipType = detail.relationshipType?.let { RelationshipTypeResponse.from(it) },
                name = detail.name,
                gender = detail.gender.name,
                birthDate = detail.birthDate,
                calendarType = detail.calendarType.name,
                birthTime = detail.birthTime.name,
                isTimeUnknown = detail.isTimeUnknown,
                dayMaster = StemResponse.from(detail.dayMaster),
                pillars = detail.pillars.map { PillarResponse.from(it) },
                ohaeng = detail.ohaeng.map { OhaengResponse.from(it) },
                sipseong = detail.sipseong.map { SipseongCountResponse.from(it) },
            )
    }
}

/** 한 기둥 상세. */
data class PillarResponse(
    val pillarType: String,
    val heavenlyStem: StemResponse,
    val earthlyBranch: BranchResponse,
    val stemSipseong: SipseongResponse?,
    val branchSipseong: SipseongResponse,
    val jijanggan: List<StemResponse>,
    val sibiunseong: SibiunseongResponse,
    val sinsal: SinsalResponse,
) {
    companion object {
        fun from(detail: PillarDetail): PillarResponse =
            PillarResponse(
                pillarType = detail.pillar.pillarType.name,
                heavenlyStem = StemResponse.from(detail.pillar.stem),
                earthlyBranch = BranchResponse.from(detail.pillar.branch),
                stemSipseong = detail.pillar.stemSipseong?.let { SipseongResponse.from(it) },
                branchSipseong = SipseongResponse.from(detail.pillar.branchSipseong),
                jijanggan = detail.jijanggan.map { StemResponse.from(it) },
                sibiunseong = SibiunseongResponse.from(detail.pillar.sibiunseong),
                sinsal = SinsalResponse.from(detail.sinsal),
            )
    }
}

/** 천간 표시 정보(코드·한자·독음·오행). */
data class StemResponse(
    val code: String,
    val hanja: String,
    val reading: String,
    val element: ElementResponse,
) {
    companion object {
        fun from(stem: HeavenlyStem): StemResponse = StemResponse(stem.name, stem.hanja, stem.reading, ElementResponse.from(stem.element))
    }
}

/** 지지 표시 정보(코드·한자·독음·오행). */
data class BranchResponse(
    val code: String,
    val hanja: String,
    val reading: String,
    val element: ElementResponse,
) {
    companion object {
        fun from(branch: EarthlyBranch): BranchResponse =
            BranchResponse(branch.name, branch.hanja, branch.reading, ElementResponse.from(branch.element))
    }
}

/** 오행 표시 정보(코드·독음·한자). */
data class ElementResponse(
    val code: String,
    val label: String,
    val hanja: String,
) {
    companion object {
        fun from(element: Element): ElementResponse = ElementResponse(element.name, element.label, element.hanja)
    }
}

/** 십성 표시 정보(코드·한글). */
data class SipseongResponse(
    val code: String,
    val label: String,
) {
    companion object {
        fun from(sipseong: Sipseong): SipseongResponse = SipseongResponse(sipseong.name, sipseong.label)
    }
}

/** 십이운성 표시 정보(코드·한글). */
data class SibiunseongResponse(
    val code: String,
    val label: String,
) {
    companion object {
        fun from(sibiunseong: Sibiunseong): SibiunseongResponse = SibiunseongResponse(sibiunseong.name, sibiunseong.label)
    }
}

/** 십이신살 표시 정보(코드·한글). */
data class SinsalResponse(
    val code: String,
    val label: String,
) {
    companion object {
        fun from(sinsal: Sinsal): SinsalResponse = SinsalResponse(sinsal.name, sinsal.label)
    }
}

/** 관계 라벨 표시 정보(코드·한글). */
data class RelationshipTypeResponse(
    val code: String,
    val label: String,
) {
    companion object {
        fun from(type: RelationshipType): RelationshipTypeResponse = RelationshipTypeResponse(type.name, type.label)
    }
}

/** 오행 분포 한 행. */
data class OhaengResponse(
    val element: ElementResponse,
    val count: Int,
    val percentage: Double,
) {
    companion object {
        fun from(ohaeng: OhaengCount): OhaengResponse =
            OhaengResponse(ElementResponse.from(ohaeng.element), ohaeng.count, ohaeng.percentage)
    }
}

/** 십성 분포 한 행. */
data class SipseongCountResponse(
    val sipseong: SipseongResponse,
    val count: Int,
    val percentage: Double,
) {
    companion object {
        fun from(sipseong: SipseongCount): SipseongCountResponse =
            SipseongCountResponse(SipseongResponse.from(sipseong.sipseong), sipseong.count, sipseong.percentage)
    }
}
