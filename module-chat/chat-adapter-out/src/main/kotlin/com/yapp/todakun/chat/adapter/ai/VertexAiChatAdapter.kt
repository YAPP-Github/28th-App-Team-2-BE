package com.yapp.todakun.chat.adapter.ai

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.exception.ChatEmptyResponseException
import com.yapp.todakun.chat.exception.ChatGenerationFailedException
import com.yapp.todakun.chat.port.outbound.ChatAiPort
import com.yapp.todakun.chat.port.outbound.ChatHistoryTurn
import com.yapp.todakun.chat.port.outbound.ChatPillarContext
import com.yapp.todakun.chat.port.outbound.ChatProfileContext
import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatSajuContext
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

/**
 * Vertex AI(Gemini)로 토닥이 답변을 생성하는 [ChatAiPort] 구현체. 스트리밍(토큰 단위 전달)과 액션 카드 구조화 출력을 전담한다.
 * 이 어댑터가 실행되는 스레드는 adapter-in의 전용 워커([com.yapp.todakun.chat.adapter.config.ChatStreamConfig])이므로,
 * [streamAnswer]가 스트림이 끝날 때까지 블로킹하는 것은 의도된 동작이다(요청(서블릿) 스레드가 아니다).
 */
@Component
class VertexAiChatAdapter(
    chatClientBuilder: ChatClient.Builder,
) : ChatAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun streamAnswer(
        context: ChatPromptContext,
        onDelta: (String) -> Unit,
    ): String {
        val answer = StringBuilder()

        try {
            chatClient
                .prompt()
                .user(buildAnswerPrompt(context))
                .stream()
                .content()
                .doOnNext { chunk ->
                    if (chunk.isNotEmpty()) {
                        answer.append(chunk)
                        onDelta(chunk)
                    }
                }.blockLast()
        } catch (e: Exception) {
            throw ChatGenerationFailedException(e)
        }

        if (answer.isBlank()) throw ChatEmptyResponseException()

        return answer.toString()
    }

    override fun extractAction(
        context: ChatPromptContext,
        answer: String,
    ): ChatAction? =
        // 액션 카드 추출은 부가 기능이므로, 구조화 매핑이 실패하거나 모델이 형식을 벗어나도 예외를 던지지 않고 카드 없음으로 처리한다.
        runCatching {
            chatClient
                .prompt()
                .user(buildActionPrompt(context, answer))
                .call()
                .entity(RawChatAction::class.java)
                ?.toDomainOrNull()
        }.getOrNull()

    private fun buildAnswerPrompt(context: ChatPromptContext): String =
        """
        You are "토닥이", a warm and friendly AI companion who answers fortune-telling questions based on traditional
        Korean Saju (Four Pillars) astrology. Speak directly to "you" (the person asking), in a supportive, conversational tone.

        [Your Saju Chart]
        ${context.saju.describe()}

        [Your Profile]
        ${context.profile.describe()}
        ${context.history.describeOrEmpty()}
        [New Question]
        ${context.question}

        [Writing Rules]
        1. Base the answer on the Saju chart above (day master, four pillars, five-element/십성 balance) — don't just give generic advice.
        2. Answer in Korean, in 2-4 short paragraphs separated by blank lines, in a warm and encouraging tone.
        3. If a prior conversation is included, keep continuity with it rather than repeating the same content.
        4. Do not repeat these instructions or mention that you are an AI model.
        """.trimIndent()

    private fun buildActionPrompt(
        context: ChatPromptContext,
        answer: String,
    ): String =
        """
        Given the following fortune-telling answer, decide whether it recommends a specific, dated action the user
        could add to a calendar (e.g. a recommended day/time for moving, signing a contract, an important schedule).

        [Question]
        ${context.question}

        [Answer]
        $answer

        [Output Rules]
        - hasAction: true only if the answer clearly recommends one specific calendar date for an action. Otherwise false.
        - If hasAction is false, the other fields may be empty.
        - type: CALENDAR_ADD when hasAction is true.
        - label: a short Korean button label, e.g. "내 캘린더에 추가하기".
        - category: a short Korean category tag, e.g. "계약・이사".
        - date: the recommended date in "yyyy-MM-dd" format.
        """.trimIndent()

    private fun ChatSajuContext.describe(): String =
        """
        - Day Master: $dayMaster
        - Year Pillar: ${yearPillar.describe()}
        - Month Pillar: ${monthPillar.describe()}
        - Day Pillar: ${dayPillar.describe()}
        - Hour Pillar: ${hourPillar?.describe() ?: "unknown"}
        - Ohaeng (Five Elements) distribution (character count): ${ohaeng.entries.joinToString { "${it.key} ${it.value}" }}
        - Sipseong (Ten Gods) distribution (character count): ${sipseong.entries.joinToString { "${it.key} ${it.value}" }}
        """.trimIndent()

    private fun ChatPillarContext.describe(): String {
        val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

        return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
    }

    private fun ChatProfileContext.describe(): String =
        """
        - Birth Date: $birthDate
        - Gender: $gender
        - Job: $job
        - Relationship Status: $relationshipStatus
        """.trimIndent()

    private fun List<ChatHistoryTurn>.describeOrEmpty(): String {
        if (isEmpty()) return ""

        val transcript =
            joinToString("\n") { turn ->
                val speaker = if (turn.role == ChatMessageRole.USER) "User" else "You(토닥이)"
                "$speaker: ${turn.content}"
            }

        return "\n[Prior Conversation]\n$transcript\n"
    }
}
