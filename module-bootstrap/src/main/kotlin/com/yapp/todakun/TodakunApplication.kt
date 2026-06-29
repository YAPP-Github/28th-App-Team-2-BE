package com.yapp.todakun

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TodakunApplication

fun main(args: Array<String>) {
    runApplication<TodakunApplication>(*args)
}
