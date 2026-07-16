plugins {
    id("todakun.kotlin-common")
    id("todakun.lombok")
}

dependencies {
    implementation(libs.spring.boot.starter.data.jpa)
}
