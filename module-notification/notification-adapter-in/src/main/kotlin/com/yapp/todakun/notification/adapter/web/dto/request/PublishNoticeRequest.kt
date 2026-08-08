package com.yapp.todakun.notification.adapter.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class PublishNoticeRequest(
    @field:NotBlank
    @field:Schema(description = "공지 제목", example = "서비스 점검 안내")
    val title: String,
    @field:NotBlank
    @field:Schema(description = "공지 본문", example = "8/9(일) 02:00~04:00 서비스 점검이 진행됩니다.")
    val content: String,
    @field:Schema(description = "딥링크(선택)", example = "todakun://notice/1")
    val deepLink: String? = null,
)
