package com.yapp.todakun.auth.adapter.persistence.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_ALGORITHM = "AES"
private const val IV_LENGTH_BYTES = 12
private const val TAG_LENGTH_BITS = 128
private const val ENCRYPTION_KEY_ENV = "ENCRYPTION_KEY"

/**
 * Apple refresh token처럼 탈퇴 시 계정 연결 해제(revoke) 권한을 갖는 민감 컬럼을 AES-256-GCM으로 at-rest 암호화한다.
 * JPA 프로바이더가 리플렉션으로 직접 생성하는 컨버터라(Spring bean 아님) 키는 [ENCRYPTION_KEY_ENV] 환경변수에서 직접 읽는다.
 * 저장 형식: Base64(IV 12바이트 || 암호문+태그).
 */
@Converter
class AesGcmStringConverter : AttributeConverter<String, String> {
    private val secureRandom = SecureRandom()
    private val secretKey = resolveSecretKey()

    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) return null

        val iv = ByteArray(IV_LENGTH_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))

        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) return null

        val decoded = Base64.getDecoder().decode(dbData)
        val iv = decoded.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = decoded.copyOfRange(IV_LENGTH_BYTES, decoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun resolveSecretKey(): SecretKeySpec {
        val encodedKey =
            System.getenv(ENCRYPTION_KEY_ENV)
                ?: error("$ENCRYPTION_KEY_ENV 환경변수가 설정되어 있지 않습니다.")

        return SecretKeySpec(Base64.getDecoder().decode(encodedKey), KEY_ALGORITHM)
    }
}
