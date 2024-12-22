val shadowCommon: Configuration by configurations.creating

architectury {
    platformSetupLoomIde()
    fabric()
}

configurations {

    getByName("developmentFabric").extendsFrom(configurations["shadowCommon"])
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




