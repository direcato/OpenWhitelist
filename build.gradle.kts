import java.io.ByteArrayOutputStream

plugins {
    java
}

group = "io.github.openwhitelist"

version = try {
    ByteArrayOutputStream().use { baos ->
        exec {
            commandLine("git", "describe", "--tags", "--dirty", "--always")
            standardOutput = baos
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        val raw = baos.toString("UTF-8").trim()
        if (raw.isEmpty()) "0.0.0-SNAPSHOT" else raw.removePrefix("v")
    }
} catch (e: Exception) {
    "0.0.0-SNAPSHOT"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT") {
        exclude("org.geysermc.cumulus", "cumulus")
        exclude("org.geysermc.geyser", "common")
    }
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    jar {
        archiveFileName.set("OpenWhitelist.jar")
    }
}
