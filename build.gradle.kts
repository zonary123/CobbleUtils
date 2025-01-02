plugins {
    id("java")
    id("java-library")

    id("dev.architectury.loom") version "1.9-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

group = property("maven_group") as String


allprojects {


    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "com.github.johnrengelman.shadow")

    dependencies {
        "minecraft"("com.mojang:minecraft:${property("minecraft_version")}")
        "mappings"("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
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

    repositories {
        mavenCentral()
        maven("https://cursemaven.com")
        maven("https://thedarkcolour.github.io/KotlinForForge/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://jitpack.io")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://maven.impactdev.net/repository/development")
        maven("https://repo.essentialsx.net/releases/")
        maven("https://maven.impactdev.net/repository/development/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenLocal()

        maven("https://maven.nucleoid.xyz/") {
            name = "Nucleoid"
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            name = "Sonatype Snapshots"
        }
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") {
            name = "Sonatype 01 Snapshots"
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
    }
}