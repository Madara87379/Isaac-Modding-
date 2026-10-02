package com.example.domain

import com.example.data.model.FileCategory
import com.example.data.model.ModFile
import com.example.data.model.ModMetadata

object SampleMods {

    fun getSampleModList(): List<Pair<ModMetadata, List<ModFile>>> = listOf(
        getNekoIsaacMod(),
        getBrimstoneHornsMod(),
        getTaintedFallenMod()
    )

    private fun getNekoIsaacMod(): Pair<ModMetadata, List<ModFile>> {
        val meta = ModMetadata(
            name = "Neko Isaac & Purr SFX",
            id = "neko_isaac_mod",
            version = "1.2",
            description = "Replaces Isaac's sprite with cat ears and replaces hurt sounds with meow purrs (Pure Asset Mod)."
        )

        val metadataXml = """
            <metadata>
                <name>Neko Isaac &amp; Purr SFX</name>
                <directory>neko_isaac</directory>
                <id>28491823</id>
                <version>1.2</version>
                <description>Cute cat Isaac resprite and audio replacement.</description>
            </metadata>
        """.trimIndent()

        val files = listOf(
            ModFile("metadata.xml", textContent = metadataXml, size = metadataXml.length.toLong(), category = FileCategory.XML),
            ModFile(
                "Resources/Gfx/Characters/Costumes/character_001_isaac.png",
                binaryBytes = ByteArray(128) { 0x89.toByte() },
                size = 128,
                category = FileCategory.GFX
            ),
            ModFile(
                "Resources/Gfx/ui/stage/boss_portrait_isaac.png",
                binaryBytes = ByteArray(256) { 0x89.toByte() },
                size = 256,
                category = FileCategory.GFX
            ),
            ModFile(
                "Resources/Sfx/Characters/isaac_hurt.wav",
                binaryBytes = ByteArray(512),
                size = 512,
                category = FileCategory.AUDIO
            ),
            ModFile(
                "Resources/Sfx/Characters/isaac_meow.ogg",
                binaryBytes = ByteArray(1024),
                size = 1024,
                category = FileCategory.AUDIO
            )
        )
        return meta to files
    }

    private fun getBrimstoneHornsMod(): Pair<ModMetadata, List<ModFile>> {
        val meta = ModMetadata(
            name = "Brimstone Horns Item",
            id = "brimstone_horns_item",
            version = "2.0",
            description = "Custom passive item with +2.5 Damage and homing tears, plus sprite costumes."
        )

        val metadataXml = """
            <metadata>
                <name>Brimstone Horns Item</name>
                <directory>brimstone_horns</directory>
                <id>39482110</id>
                <version>2.0</version>
                <description>Custom PC collectible with Lua cache evaluation.</description>
            </metadata>
        """.trimIndent()

        val itemsXml = """
            <items gfxroot="gfx/items/collectibles/">
                <passive id="920" name="Brimstone Horns" description="Demonic power (+2.5 Damage, Homing)" gfx="collectibles_920_brimhorns.png" cache="damage" />
            </items>
        """.trimIndent()

        val mainLua = """
            local BrimMod = RegisterMod("Brimstone Horns", 1)
            local BRIM_ID = Isaac.GetItemIdByName("Brimstone Horns")

            function BrimMod:EvaluateCache(player, cacheFlag)
                if player:HasCollectible(BRIM_ID) then
                    if cacheFlag == CacheFlag.CACHE_DAMAGE then
                        player.Damage = player.Damage + 2.5
                    end
                    if cacheFlag == CacheFlag.CACHE_TEARFLAG then
                        player.TearFlags = player.TearFlags | TearFlags.TEAR_HOMING
                    end
                end
            end

            BrimMod:AddCallback(ModCallbacks.MC_EVALUATE_CACHE, BrimMod.EvaluateCache)
            print("Brimstone Horns Mod Loaded on PC!")
        """.trimIndent()

        val files = listOf(
            ModFile("metadata.xml", textContent = metadataXml, size = metadataXml.length.toLong(), category = FileCategory.XML),
            ModFile("content/items.xml", textContent = itemsXml, size = itemsXml.length.toLong(), category = FileCategory.XML),
            ModFile("main.lua", textContent = mainLua, size = mainLua.length.toLong(), category = FileCategory.LUA),
            ModFile(
                "resources/gfx/items/collectibles/collectibles_920_brimhorns.png",
                binaryBytes = ByteArray(128) { 0x89.toByte() },
                size = 128,
                category = FileCategory.GFX
            ),
            ModFile(
                "resources/gfx/characters/costumes/costume_920_brimhorns.anm2",
                textContent = "<animated_actor><content file=\"gfx/characters/costumes/costume_920_brimhorns.png\" /></animated_actor>",
                size = 120,
                category = FileCategory.ANM2
            )
        )
        return meta to files
    }

    private fun getTaintedFallenMod(): Pair<ModMetadata, List<ModFile>> {
        val meta = ModMetadata(
            name = "Tainted Fallen Character",
            id = "tainted_fallen_mod",
            version = "1.0",
            description = "Custom character starting with 3 Black Hearts, flight, and darkness tears."
        )

        val metadataXml = """
            <metadata>
                <name>Tainted Fallen Character</name>
                <directory>tainted_fallen</directory>
                <id>48192031</id>
                <version>1.0</version>
                <description>Play as the Fallen Angel.</description>
            </metadata>
        """.trimIndent()

        val playersXml = """
            <players>
                <player id="35" name="Tainted Fallen" skin="character_fallen.png" costume="35" bhearts="6" canfly="true" armor="0" />
            </players>
        """.trimIndent()

        val mainLua = """
            local FallenMod = RegisterMod("Tainted Fallen", 1)
            local FALLEN_PLAYER_TYPE = Isaac.GetPlayerTypeByName("Tainted Fallen", true)

            function FallenMod:OnPlayerInit(player)
                if player:GetPlayerType() == FALLEN_PLAYER_TYPE then
                    player:AddBlackHearts(6)
                    player.CanFly = true
                end
            end

            FallenMod:AddCallback(ModCallbacks.MC_POST_PLAYER_INIT, FallenMod.OnPlayerInit)
        """.trimIndent()

        val files = listOf(
            ModFile("metadata.xml", textContent = metadataXml, size = metadataXml.length.toLong(), category = FileCategory.XML),
            ModFile("content/players.xml", textContent = playersXml, size = playersXml.length.toLong(), category = FileCategory.XML),
            ModFile("main.lua", textContent = mainLua, size = mainLua.length.toLong(), category = FileCategory.LUA),
            ModFile(
                "resources/gfx/characters/character_fallen.png",
                binaryBytes = ByteArray(256) { 0x89.toByte() },
                size = 256,
                category = FileCategory.GFX
            )
        )
        return meta to files
    }
}
