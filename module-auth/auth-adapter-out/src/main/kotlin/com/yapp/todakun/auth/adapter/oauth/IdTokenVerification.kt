package com.yapp.todakun.auth.adapter.oauth

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.yapp.todakun.auth.exception.OauthTokenInvalidException
import java.text.ParseException

/** Google/Apple이 동일하게 쓰는 RS256 id_token 검증 - 서명/형식 검증 실패는 모두 토큰 무효로 취급한다. */
internal fun parseIdTokenClaims(
    processor: ConfigurableJWTProcessor<SecurityContext>,
    idToken: String,
): JWTClaimsSet =
    try {
        processor.process(idToken, null)
    } catch (_: ParseException) {
        throw OauthTokenInvalidException()
    } catch (_: BadJOSEException) {
        throw OauthTokenInvalidException()
    } catch (_: JOSEException) {
        throw OauthTokenInvalidException()
    }
