rootProject.name = "CobbleUtils"

pluginManagement {
    repositories {

        mavenCentral()
        maven("https://cursemaven.com")
        maven("https://thedarkcolour.github.io/KotlinForForge/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://maven.impactdev.net/repository/development/")
        gradlePluginPortal()
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
include("common", "fabric")
//include("common", "fabric", "neoforge")