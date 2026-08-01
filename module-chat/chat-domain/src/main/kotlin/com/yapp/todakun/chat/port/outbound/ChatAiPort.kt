package com.yapp.todakun.chat.port.outbound

import com.yapp.todakun.chat.ChatAction

/**
 * 회원의 사주·대화 맥락에 근거한 토닥이 답변을 생성하는 아웃바운드 포트.
 * 스트리밍/구조화 출력 등 모델 호출 방식은 어댑터가 담당하며, 도메인은 리액티브 타입(Flux 등)에 의존하지 않는다.
 */
interface ChatAiPort {
    /** 답변을 토큰 단위로 [onDelta]에 흘려보내고, 스트림이 끝나면 완성된 전체 텍스트를 반환한다. */
    fun streamAnswer(
        context: ChatPromptContext,
        onDelta: (String) -> Unit,
    ): String

    /** 완성된 답변에서 캘린더 등 액션 카드를 추출한다(해당 사항이 없으면 null). */
    fun extractAction(
        context: ChatPromptContext,
        answer: String,
    ): ChatAction?
}
