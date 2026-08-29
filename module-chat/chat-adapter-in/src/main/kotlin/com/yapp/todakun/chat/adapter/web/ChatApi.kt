package com.yapp.todakun.chat.adapter.web

import com.yapp.todakun.chat.adapter.web.dto.response.ChatEntryResponse
import com.yapp.todakun.chat.adapter.web.dto.response.ConversationDetailResponse
import com.yapp.todakun.chat.adapter.web.dto.response.ConversationListResponse
import com.yapp.todakun.web.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Chat", description = "토닥이 채팅 API(진입 화면·대화 히스토리)")
interface ChatApi {
    @Operation(
        summary = "채팅 진입 화면 조회",
        description = "추천 질문 칩 목록과 오늘 남은 무료 채팅 횟수를 조회한다.",
    )
    @GetMapping("api/v1/chat/entry")
    fun getEntry(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<ChatEntryResponse>>

    @Operation(
        summary = "대화 히스토리 목록 조회",
        description = "회원의 전체 대화를 최근 활동 순으로 조회한다.",
    )
    @GetMapping("api/v1/chat/conversations")
    fun getConversations(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
    ): ResponseEntity<CommonResponse<ConversationListResponse>>

    @Operation(
        summary = "대화 상세 조회",
        description = "대화의 전체 메시지를 조회한다. 조회와 동시에 해당 대화를 읽음 처리한다.",
    )
    @GetMapping("api/v1/chat/conversations/{conversationId}")
    fun getConversation(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "대화 ID")
        @PathVariable conversationId: UUID,
    ): ResponseEntity<CommonResponse<ConversationDetailResponse>>

    @Operation(summary = "대화 삭제", description = "본인 소유의 대화만 삭제할 수 있다.")
    @DeleteMapping("api/v1/chat/conversations/{conversationId}")
    fun deleteConversation(
        @Parameter(hidden = true)
        @AuthenticationPrincipal memberId: UUID,
        @Parameter(description = "대화 ID")
        @PathVariable conversationId: UUID,
    ): ResponseEntity<CommonResponse<Unit>>
}
