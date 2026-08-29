package com.yapp.todakun.common.logging

/**
 * 클래스에 붙이면 KSP가 컴파일 타임에 같은 패키지·같은 이름으로 `log` 로거 확장 프로퍼티를 생성한다.
 * 클래스 내부 메서드에서는 암시적 리시버로 잡혀 기존 `private val log = LoggerFactory.getLogger(...)`와 동일하게 `log.xxx(...)`로 그대로 쓸 수 있다.
 * 생성 로직은 [com.yapp.todakun.common.logging.processor.LoggableSymbolProcessor] 참고.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Loggable
