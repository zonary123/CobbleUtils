import org.gradle.kotlin.dsl.support.kotlinCompilerOptions

plugins {
    id("java")
    id("java-library")
    kotlin("jvm")

    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

group = property("maven_group") as String
val targetJavaVersion = 21

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    kotlinCompilerOptions("1.8")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
    options.compilerArgs.add("-Xlint:-processing,-classfile,-serial")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = targetJavaVersion.toString()
}

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