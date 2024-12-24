import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer

val shadowCommon: Configuration by configurations.creating

architectury {
    platformSetupLoomIde()
    fabric()
}

configurations {
    compileClasspath.get().extendsFrom(configurations["shadowCommon"])
    runtimeClasspath.get().extendsFrom(configurations["shadowCommon"])
    getByName("developmentFabric").extendsFrom(configurations["shadowCommon"])
}
loom {
    enableTransitiveAccessWideners.set(true)
    silentMojangMappingsLicense()
}

dependencies {
    modImplementation("com.cobblemon:fabric:${property("cobblemon_version")}")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    shadowCommon(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionFabric")) { isTransitive = false }


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

tasks {
    base.archivesName.set(
        "${project.property("mod_version")}/${project.property("archives_base_name")}-fabric" +
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
        exclude("generations/gg/generations/core/generationscore/fabric/datagen/**")
        exclude("data/forge/**")
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

        transformers.add(ServiceFileTransformer())

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


