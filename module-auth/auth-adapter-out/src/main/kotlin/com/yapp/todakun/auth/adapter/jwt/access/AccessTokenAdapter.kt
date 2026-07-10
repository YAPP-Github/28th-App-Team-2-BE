package com.yapp.todakun.auth.adapter.jwt.access

import com.yapp.todakun.auth.claims.AccessTokenClaims
import com.yapp.todakun.auth.code.AuthErrorCode
import com.yapp.todakun.auth.port.outbound.AccessTokenPort
import com.yapp.todakun.auth.token.IssuedAccessToken
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
    @ExperimentalUuidApi
    override fun generate(memberId: UUID): IssuedAccessToken {
        val jti = Uuid.generateV7().toJavaUuid().toString()
        val now = Date()

        val value =
            Jwts.builder()
                .subject(memberId.toString())
                .id(jti)
                .issuedAt(now)
                .expiration(Date(now.time + accessTokenProperties.expirySeconds * 1000))
                .signWith(signingKey)
                .compact()

        return IssuedAccessToken(
            value = value,
            jti = jti,
            expiresInSeconds = accessTokenProperties.expirySeconds,
        )
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

    private fun parseClaims(token: String): Claims =
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: ExpiredJwtException) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_EXPIRED)
        } catch (_: JwtException) {
            throw UnauthorizedException(AuthErrorCode.TOKEN_INVALID)
        }
}
