package com.yapp.todakun.auth.adapter.jwt

import com.yapp.todakun.auth.AccessTokenClaims
import com.yapp.todakun.auth.IssuedAccessToken
import com.yapp.todakun.auth.adapter.jwt.config.AccessTokenProperties
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.port.AccessTokenPort
import com.yapp.todakun.common.exception.UnauthorizedException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@Component
class AccessTokenAdapter(
    private val accessTokenProperties: AccessTokenProperties,
) : AccessTokenPort {
    @OptIn(ExperimentalUuidApi::class)
    override fun generate(memberId: UUID): IssuedAccessToken {
        val jti = Uuid.generateV7().toJavaUuid().toString()
        val value = buildToken(subject = memberId.toString(), jti = jti, expiresInSeconds = accessTokenProperties.expirySeconds)
        return IssuedAccessToken(value = value, jti = jti, expiresInSeconds = accessTokenProperties.expirySeconds)
    }

    override fun parse(token: String): AccessTokenClaims {
        val claims = parseClaims(token)
        return AccessTokenClaims(
            memberId = UUID.fromString(claims.subject),
            jti = requireNotNull(claims.id),
            remainingSeconds = ((claims.expiration.time - System.currentTimeMillis()) / 1000).coerceAtLeast(0),
        )
    }

    private val signingKey: SecretKey by lazy { Keys.hmacShaKeyFor(accessTokenProperties.secret.toByteArray(Charsets.UTF_8)) }

    private fun buildToken(
        subject: String,
        jti: String,
        expiresInSeconds: Long,
    ): String {
        val now = Date()

        return Jwts.builder()
            .subject(subject)
            .id(jti)
            .issuedAt(now)
            .expiration(Date(now.time + expiresInSeconds * 1000))
            .signWith(signingKey)
            .compact()
    }

    private fun parseClaims(token: String): Claims =
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_EXPIRED)
        } catch (e: JwtException) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_INVALID)
        }
}
