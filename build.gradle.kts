plugins {
    java
    `maven-publish`
    kotlin("jvm")
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
    implementation("org.xerial:sqlite-jdbc:_")
    implementation("com.github.kevinsawicki:http-request:_")
    implementation("com.google.apis:google-api-services-customsearch:_")
    implementation("com.google.http-client:google-http-client-jackson2:_")
    implementation("org.apache.commons:commons-lang3:_")
    implementation("org.twitter4j:twitter4j-core:_")
    implementation("com.google.guava:guava:_")
    implementation("org.rometools:rome-fetcher:_")
    implementation("com.google.apis:google-api-services-urlshortener:_")
    implementation("joda-time:joda-time:_")
    implementation("org.jsoup:jsoup:_")
    implementation("ca.rmen:lib-french-revolutionary-calendar:_")
    api("org.json:json:_")
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
