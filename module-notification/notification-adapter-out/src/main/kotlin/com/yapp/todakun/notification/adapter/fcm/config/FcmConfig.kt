package com.yapp.todakun.notification.adapter.fcm.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.yapp.todakun.notification.adapter.fcm.properties.FcmProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * FCM 초기화. ADC(Application Default Credentials)로 인증하며(키 파일 불필요),
 * fcm.enabled=true일 때만 Firebase 빈을 생성한다(false/미설정이면 no-op).
 */
@Configuration
@EnableConfigurationProperties(FcmProperties::class)
@ConditionalOnProperty(prefix = "fcm", name = ["enabled"], havingValue = "true")
class FcmConfig(
    private val fcmProperties: FcmProperties,
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        // 컨텍스트 리프레시 시 중복 초기화를 방지(멱등).
        FirebaseApp.getApps().firstOrNull()?.let { return it }
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(fcmProperties.projectId)
                .build()
        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)
}
