plugins {
    java
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://jcenter.bintray.com")
    }
}

group = "org.jraf"
version = "1.11.2"
description = "irondad"

dependencies {
    implementation(libs.sqlite.jdbc)
    implementation(libs.http.request)
    implementation(libs.google.api.services.customsearch)
    implementation(libs.google.http.client.jackson2)
    implementation(libs.commons.lang3)
    implementation(libs.twitter4j.core)
    implementation(libs.guava)
    implementation(libs.rome.fetcher)
    implementation(libs.google.api.services.urlshortener)
    implementation(libs.joda.time)
    implementation(libs.jsoup)
    implementation(libs.lib.french.revolutionary.calendar)
    api(libs.json)
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew publishToMavenLocal` to publish to the local maven repository
