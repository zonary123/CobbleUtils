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
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    // Forge
    forge("net.minecraftforge:forge:${property("forge_version")}")

    "common"(project(":common", "namedElements")) { isTransitive = false }
    "shadowCommon"(project(":common", "transformProductionForge")) { isTransitive = false }

    modImplementation("com.cobblemon:forge:${property("cobblemon_version")}")

    //implementation("thedarkcolour:kotlinforforge:4.4.0")

    // Kyori Adventure
    shadowCommon("net.kyori:adventure-text-serializer-gson:${property("kyori_version")}")
    shadowCommon("net.kyori:adventure-text-minimessage:${property("kyori_version")}")

    // Database
    shadowCommon("org.mongodb:mongodb-driver-sync:${property("mongodb_version")}")

    // Economy Vault
    shadowCommon("com.github.MilkBowl:VaultAPI:1.7")

    // Discord
    shadowCommon("club.minnced:discord-webhooks:${property("discord_webhooks_version")}")
    shadowCommon("org.json:json:20210307")
    shadowCommon("net.objecthunter:exp4j:0.4.8")
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
    base.archivesName.set(
        "${project.property("mod_version")}/${project.property("archives_base_name")}-forge-${
            project.property(
                "mod_version"
            )
        }"
    )
    processResources {
        inputs.property("version", project.version)

        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to project.version))
        }
    }
    shadowJar {
        exclude("fabric.mod.json")
        exclude("architectury.common.json")
        exclude("com/google/gson/**")
        exclude("org/intellij/**")
        exclude("org/jetbrains/**")
        // Vault
        exclude("org/bukkit/**")
        exclude("org/apache/**")
        exclude("org/yaml/**")
        exclude("org/junit/**")
        exclude("org/java_websocket/**")
        exclude("org/hamcrest/**")
        exclude("com/google/**")
        exclude("kotlin/**")
        exclude("org/slf4j/**")

        relocate("org.json", "com.kingpixel.cobbleutils.json")
        relocate("com.mongodb", "com.kingpixel.cobbleutils.mongodb")
        relocate("org.bson", "com.kingpixel.cobbleutils.bson")
        relocate("net.kyori", "com.kingpixel.cobbleutils.kyori") {
            exclude("net/kyori/adventure/key/**/*")
        }

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
