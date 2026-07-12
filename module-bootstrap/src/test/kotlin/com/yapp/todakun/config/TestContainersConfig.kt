package com.yapp.todakun.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfig {
    companion object {
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"))
                .withReuse(true)
                .apply { start() }

        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7.2"))
                .withExposedPorts(6379)
                .withReuse(true)
                .apply { start() }
    }

    @Bean
    @ServiceConnection
    fun postgresContainer() = postgres

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer() = redis
}
