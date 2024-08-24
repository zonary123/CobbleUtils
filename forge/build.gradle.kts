import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
//val developmentForge: Configuration by configurations.getting

architectury {
    platformSetupLoomIde()
    forge()
}

configurations {
    //create("common")
    //create("shadowCommon")
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    getByName("developmentForge").extendsFrom(configurations["common"])
}

loom {
    enableTransitiveAccessWideners.set(true)
    silentMojangMappingsLicense()
}

dependencies {
    //minecraft("net.minecraft:minecraft:${property("minecraft_version")}")
    //mappings(loom.officialMojangMappings())
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    // Forge
    forge("net.minecraftforge:forge:${property("forge_version")}")

    "common"(project(":common", "namedElements")) { isTransitive = false }
    "shadowCommon"(project(":common", "transformProductionForge")) { isTransitive = false }

    modImplementation("com.cobblemon:forge:${property("cobblemon_version")}")

    implementation("thedarkcolour:kotlinforforge:4.4.0")

    /*
            implementation("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
            implementation("net.kyori:adventure-api:${property("kyori_version")}")
            //implementation("net.kyori:adventure-key:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-serializer-plain:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-serializer-legacy:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-minimessage:${property("kyori_version")}")
            implementation("net.kyori:examination-api:1.3.0")
            implementation("net.kyori:examination-string:1.3.0")
            implementation("net.kyori:adventure-nbt:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-serializer-json:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-logger-slf4j:${property("kyori_version")}")
            implementation("net.kyori:adventure-platform-api:4.3.0")
            implementation("net.kyori:adventure-text-serializer-ansi:${property("kyori_version")}")
            implementation("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
            // implementation("org.mongodb:mongodb-driver-reactivestreams:5.1.2")*/

    api("net.kyori:adventure-api:${property("kyori_version")}")
    api("net.kyori:adventure-nbt:${property("kyori_version")}")
    api("net.kyori:adventure-text-serializer-plain:${property("kyori_version")}")
    api("net.kyori:adventure-text-serializer-legacy:${property("kyori_version")}")
    api("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
    api("net.kyori:adventure-text-minimessage:${property("kyori_version")}")
    api("net.kyori:adventure-text-logger-slf4j:${property("kyori_version")}")
    api("net.kyori:adventure-platform-api:4.3.0")
    api("net.kyori:event-api:5.0.0-SNAPSHOT")
    /*
        shadowCommon("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-api:${property("kyori_version")}")
        //shadowCommon("net.kyori:adventure-key:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-serializer-plain:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-serializer-legacy:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-minimessage:${property("kyori_version")}")
        shadowCommon("net.kyori:examination-api:1.3.0")
        shadowCommon("net.kyori:examination-string:1.3.0")
        shadowCommon("net.kyori:adventure-nbt:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-serializer-json:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-logger-slf4j:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-platform-api:4.3.0")
        shadowCommon("net.kyori:adventure-text-serializer-ansi:${property("kyori_version")}")
        shadowCommon("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
        //shadowCommon("org.mongodb:mongodb-driver-reactivestreams:5.1.2")
        */
    shadowCommon("net.kyori:adventure-api:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-nbt:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-serializer-plain:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-serializer-legacy:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-minimessage:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-logger-slf4j:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-platform-api:4.3.0")
    shadowCommon("net.kyori:event-api:5.0.0-SNAPSHOT")
}

tasks.processResources {
    filesMatching("META-INF/mods.toml") {
        expand(
            mapOf(
                "author" to project.property("author"),
                "mod_name" to project.property("mod_name"),
                "mod_id" to project.property("mod_id"),
                "version" to project.property("mod_version"),
                "mod_description" to project.property("mod_description"),
                "repository" to project.property("repository"),
                "license" to project.property("license"),
                "mod_icon" to project.property("mod_icon"),
                "environment" to project.property("environment"),
                "supported_minecraft_versions" to project.property("supported_minecraft_versions")
            )
        )
    }
}

tasks {
    base.archivesName.set("${project.property("mod_version")}/${project.property("archives_base_name")}-forge")
    processResources {
        inputs.property("version", project.version)

        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to project.version))
        }
    }
    shadowJar {
        exclude("fabric.mod.json")
        exclude("architectury.common.json")
        exclude("com/google/gson/**/*")
        exclude("org/intellij/**/*")
        exclude("org/jetbrains/**/*")
        exclude("generations/gg/generations/core/generationscore/forge/datagen/**")

        relocate("org.bson", "com.kingpixel.cobbleutils.bson")
        relocate("net.kyori", "com.kingpixel.cobbleutils.kyori") {
            exclude("net/kyori/adventure/key/**") // Excluir la relocalización de "net.kyori.adventure.key"
        }
        relocate("org.slf4j", "com.kingpixel.cobbleutils.slf4j")

        transformers.add(ServiceFileTransformer())

        configurations = listOf(project.configurations.getByName("shadowCommon"))
        archiveClassifier.set("dev-shadow")
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        dependsOn(shadowJar)

    }

    jar {
        archiveClassifier.set("dev")
    }
}
