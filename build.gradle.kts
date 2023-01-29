plugins {
    `java-library`
}

group = "xyz.holocons.mc"
version = "1.0-SNAPSHOT"
description = "Banner waypoints for HoloCons"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
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
            "apiVersion" to "1.19",
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
