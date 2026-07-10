package com.yapp.todakun.auth.config

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.kotest.extensions.spring.SpringExtension

class KotestProjectConfig : AbstractProjectConfig() {
    override val isolationMode = IsolationMode.SingleInstance

    override fun extensions() = listOf(SpringExtension)
}
