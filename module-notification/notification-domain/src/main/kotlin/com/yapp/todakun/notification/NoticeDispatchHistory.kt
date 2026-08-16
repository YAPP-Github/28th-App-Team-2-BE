package com.yapp.todakun.notification

import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

// 명시적 키와 내용 파생 키가 같은 해시 네임스페이스를 쓰면서도 서로 충돌하지 않도록 붙이는 접두사.
private const val EXPLICIT_KEY_PREFIX = "explicit"
private const val CONTENT_KEY_PREFIX = "content"

// 필드 길이와 값을 잇는 구분자. 각 필드를 "길이:값"으로 인코딩해 이어 붙인다 — 구분자 문자 하나로만 나누면
// 그 문자가 값 안에 들어올 때 경계가 밀려(("제목A", "B본문")과 ("제목A:B", "본문")) 서로 다른 공지가
// 같은 해시를 갖게 되고, 두 번째 공지가 이미 발송된 것으로 오인돼 조용히 스킵된다.
// 길이 접두사는 값에 어떤 문자가 오든 경계가 흔들리지 않아 그 모호성을 원천적으로 없앤다.
private const val LENGTH_DELIMITER = ":"

/**
 * 공지(NOTICE) 발송 처리 이력. [idempotencyKey]가 같은 공지를 다시 발송하려는 시도는
 * 유니크 제약(`NoticeDispatchHistoryRepository.saveIfAbsent`)이 걸러내, 서로 다른 시점의 순차 재기동에도
 * at-most-once 발송을 보장한다 — advisory lock(NOTICE_LOCK_KEY)은 동시 실행만 직렬화할 뿐 이 문제는 풀지 못한다.
 */
data class NoticeDispatchHistory(
    val id: UUID,
    val idempotencyKey: String,
    val title: String,
    val content: String,
    val type: NoticeType,
    val deepLink: String?,
    val dispatchedAt: Instant,
) {
    companion object {
        @ExperimentalUuidApi
        fun create(
            idempotencyKey: String,
            title: String,
            content: String,
            type: NoticeType,
            deepLink: String?,
            dispatchedAt: Instant,
        ): NoticeDispatchHistory =
            NoticeDispatchHistory(
                id = Uuid.generateV7().toJavaUuid(),
                idempotencyKey = idempotencyKey,
                title = title,
                content = content,
                type = type,
                deepLink = deepLink,
                dispatchedAt = dispatchedAt,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            idempotencyKey: String,
            title: String,
            content: String,
            type: NoticeType,
            deepLink: String?,
            dispatchedAt: Instant,
        ): NoticeDispatchHistory = NoticeDispatchHistory(id, idempotencyKey, title, content, type, deepLink, dispatchedAt)

        /**
         * 공지 처리 이력의 멱등키를 파생한다. 같은 인자로 재기동해도 같은 키가 나와야 하므로
         * 시각·랜덤 값을 섞지 않는다(순수 함수) — 그래야 저장소의 유니크 제약이 재기동 중복을 걸러낼 수 있다.
         * [explicitKey]가 공백이 아니면 그 값을 우선 사용하고, 없으면 제목·본문·딥링크·타입에서 파생한다.
         * 두 경로 모두 SHA-256으로 해싱해 64자 고정 길이 컬럼을 유지하면서, 서로 다른 접두사로 원본 문자열을
         * 구분해 명시적 키와 내용 파생 키가 같은 네임스페이스에 있으면서도 절대 충돌하지 않게 한다.
         * 필드는 [encodeUnambiguously]로 경계를 못 박아 이어 붙인다 — 그래야 필드 경계만 다른 서로 다른 공지가
         * 같은 키로 뭉개져 두 번째 공지가 조용히 스킵되는 일이 없다.
         */
        fun deriveIdempotencyKey(
            title: String,
            content: String,
            deepLink: String?,
            type: NoticeType,
            explicitKey: String?,
        ): String {
            val keyMaterial =
                if (!explicitKey.isNullOrBlank()) {
                    listOf(EXPLICIT_KEY_PREFIX, explicitKey.trim())
                } else {
                    listOf(CONTENT_KEY_PREFIX, title, content, deepLink.orEmpty(), type.name)
                }
            return sha256Hex(encodeUnambiguously(keyMaterial))
        }

        /**
         * 각 필드를 `길이:값`으로 인코딩해 이어 붙인다. 길이를 먼저 읽고 그만큼을 값으로 떼어내는 방식이라
         * 값에 어떤 문자(구분자 포함)가 들어와도 필드 경계가 한 가지로만 해석된다 — 서로 다른 입력이 같은
         * 재료 문자열로 뭉개질 수 없다.
         */
        private fun encodeUnambiguously(fields: List<String>): String = fields.joinToString("") { "${it.length}$LENGTH_DELIMITER$it" }

        private fun sha256Hex(input: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
