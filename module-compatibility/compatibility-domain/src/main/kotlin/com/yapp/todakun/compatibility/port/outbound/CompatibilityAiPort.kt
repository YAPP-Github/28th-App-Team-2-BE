package com.yapp.todakun.compatibility.port.outbound

/**
 * 두 명식을 조합한 궁합 총운을 AI로 생성하는 아웃바운드 포트.
 * 프롬프트 구성·모델 호출·구조화 매핑은 어댑터가 담당한다. 오행 비율은 결정적 계산이라 이 포트로 생성하지 않는다.
 */
interface CompatibilityAiPort {
    fun generate(input: CompatibilityAiInput): GeneratedCompatibility
}
