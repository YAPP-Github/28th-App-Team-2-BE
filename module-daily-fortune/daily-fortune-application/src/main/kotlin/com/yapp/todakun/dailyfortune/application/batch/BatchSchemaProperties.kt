package com.yapp.todakun.dailyfortune.application.batch

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.sql.init.DatabaseInitializationMode

@ConfigurationProperties(prefix = "batch.schema")
data class BatchSchemaProperties(
    val initializationMode: DatabaseInitializationMode = DatabaseInitializationMode.ALWAYS,
)
