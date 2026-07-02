package com.yapp.todakun.auth.adapter.web

import com.yapp.todakun.common.code.ResponseCode
import com.yapp.todakun.web.response.CommonResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper

/**
 * [CustomAuthenticationEntryPoint], [CustomAccessDeniedHandler]가 공통으로 사용하는 에러 응답 작성기.
 * 서블릿 필터 체인 단계라 `GlobalExceptionHandler`가 잡지 못하므로 여기서 직접 [CommonResponse] 포맷으로 쓴다.
 */
internal fun HttpServletResponse.writeSecurityError(
    objectMapper: ObjectMapper,
    errorCode: ResponseCode,
) {
    status = errorCode.status
    contentType = MediaType.APPLICATION_JSON_VALUE
    characterEncoding = Charsets.UTF_8.name()
    writer.write(objectMapper.writeValueAsString(CommonResponse.error(errorCode).body))
}
