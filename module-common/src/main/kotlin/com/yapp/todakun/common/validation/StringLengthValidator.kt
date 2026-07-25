package com.yapp.todakun.common.validation

/** [value]의 길이가 [maxLength]를 초과하면 [onExceeded]를 호출한다. 도메인마다 다른 예외를 던지도록 호출부에 위임한다. */
fun validateMaxLength(
    value: String,
    maxLength: Int,
    onExceeded: () -> Nothing,
) {
    if (value.length > maxLength) {
        onExceeded()
    }
}
