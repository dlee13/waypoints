plugins {
    `java-library`
}

group = "xyz.holocons.mc"
version = "1.0-SNAPSHOT"
description = "Banner waypoints for HoloCons"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

        val pluginProperties = mapOf(
            "main" to "xyz.holocons.mc.waypoints.WaypointsPlugin",
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21.1",
            "authors" to listOf("dlee13"),
            "website" to "holocons.xyz",
            "depend" to listOf("ProtocolLib"),
            "prefix" to "Waypoints",
        )

        filesMatching("plugin.yml") {
            expand(pluginProperties)
        }
    }
}
