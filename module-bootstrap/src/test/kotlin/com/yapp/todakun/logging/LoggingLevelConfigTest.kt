package com.yapp.todakun.logging

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.ClassPathResource

private const val BEAN_OUTPUT_CONVERTER_LOGGER = "logging.level.org.springframework.ai.converter.BeanOutputConverter"

/**
 * dev/prod `application-{profile}.yaml`에서 `BeanOutputConverter` 로거가 OFF로 억제돼 있는지 검증하는 회귀 테스트다.
 *
 * ## 왜 필요한가
 * `BeanOutputConverter`는 AI 응답 파싱 실패마다(재시도로 자연 복구되는 경우까지) ERROR를 찍는다.
 * dev/prod는 ERROR를 Discord 웹훅으로 직발송하므로([LogbackSpringConfigTest] 참고), 이 로거를 그대로 두면
 * daily-fortune 배치가 회원당 3회 재시도 중인 정상 케이스까지 오탐 알림으로 샌다. 실제 재시도 소진(skip)은
 * `DailyFortuneSkipListener`가 별도로 ERROR 로깅해 알림을 담당하므로, 이 프레임워크 로거는 꺼둬야 한다.
 * yaml 값이 실수로 지워지거나 오타가 나도 로컬/CI에서 드러나지 않으므로 값을 직접 읽어 검증한다.
 */
class LoggingLevelConfigTest : DescribeSpec({

    fun loadProperties(resourceName: String): Map<String, Any> {
        val factory =
            YamlPropertiesFactoryBean().apply {
                setResources(ClassPathResource(resourceName))
            }
        val properties = requireNotNull(factory.getObject()) { "$resourceName 을(를) 로드하지 못했습니다" }
        return properties.entries.associate { (key, value) -> key.toString() to value }
    }

    describe("dev 프로필") {
        it("BeanOutputConverter 로거가 OFF다") {
            loadProperties("application-dev.yaml")[BEAN_OUTPUT_CONVERTER_LOGGER] shouldBe "OFF"
        }
    }

    describe("prod 프로필") {
        it("BeanOutputConverter 로거가 OFF다") {
            loadProperties("application-prod.yaml")[BEAN_OUTPUT_CONVERTER_LOGGER] shouldBe "OFF"
        }
    }
})
