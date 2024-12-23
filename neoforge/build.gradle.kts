plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
val shadowCommon: Configuration by configurations.creating

architectury {
    platformSetupLoomIde()
    neoForge()
}

configurations {

    compileClasspath.get().extendsFrom(configurations["shadowCommon"])
    runtimeClasspath.get().extendsFrom(configurations["shadowCommon"])
    getByName("developmentNeoForge").extendsFrom(configurations["shadowCommon"])
    shadowCommon.isCanBeResolved = true
    shadowCommon.isCanBeConsumed = false
}

loom {
    enableTransitiveAccessWideners.set(true)
    silentMojangMappingsLicense()
}

dependencies {
    //minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    //mappings(loom.officialMojangMappings())

    neoForge("net.neoforged:neoforge:${property("neoforge_version")}")
    modImplementation("dev.architectury:architectury-neoforge:${property("architectury_version")}")
    modImplementation("com.cobblemon:neoforge:${property("cobblemon_version")}")
    "shadowCommon"(project(":common", "namedElements"))
    "shadowCommon"(project(":common", "transformProductionNeoForge"))

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
    expand(
        mapOf(
            "mod_name" to project.property("mod_name"),
            "mod_id" to project.property("mod_id"),
            "mod_version" to project.property("mod_version"),
            "mod_description" to project.property("mod_description"),
            "author" to project.property("author"),
            "repository" to project.property("repository"),
            "license" to project.property("license"),
            "mod_icon" to project.property("mod_icon"),
            "environment" to project.property("environment"),
            "supported_minecraft_versions" to project.property("supported_minecraft_versions")
        )
    )
}

tasks {
    base.archivesName.set(
        "${project.property("mod_version")}/${project.property("archives_base_name")}-neoforge" +
                "-${
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
        exclude("architectury.common.json")
        exclude("com/google/gson/**/*")
        exclude("org/intellij/**/*")
        exclude("org/jetbrains/**/*")
        // Vault
        exclude("org/bukkit/**/*")
        exclude("org/apache/**/*")
        exclude("org/yaml/**/*")
        exclude("org/junit/**/*")
        exclude("org/java_websocket/**/*")
        exclude("org/hamcrest/**/*")
        exclude("com/google/**/*")
        exclude("org/slf4j/**")

        relocate("com.mongodb", "com.kingpixel.cobbleutils.mongodb")
        relocate("org.bson", "com.kingpixel.cobbleutils.bson")
        relocate("net.kyori", "com.kingpixel.cobbleutils.kyori") {
            exclude("net/kyori/adventure/key/**/*")
        }

        //transformers.add(ServiceFileTransformer())

        configurations = listOf(project.configurations.getByName("shadowCommon"))
        archiveClassifier.set("dev-shadow")
    }



    remapJar {
        injectAccessWidener.set(true)
        inputFile.set(shadowJar.get().archiveFile)
        dependsOn(shadowJar)
    }

    jar.get().archiveClassifier.set("dev")
}
