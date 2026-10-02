package com.example

import com.example.data.model.ChangeType
import com.example.data.model.FileCategory
import com.example.data.model.ModFile
import com.example.data.model.ModMetadata
import com.example.domain.ModConverterEngine
import com.example.domain.SampleMods
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ModConverterEngineTest {

    private val engine = ModConverterEngine()

    @Test
    fun testPureResourceModAnalysis() {
        val samples = SampleMods.getSampleModList()
        val (meta, files) = samples[0] // Neko Isaac pure asset mod

        val analysis = engine.analyzeMod(meta, files)

        assertTrue(analysis.isPureResourceMod)
        assertEquals(0, analysis.luaFiles.size)
        assertEquals(100, analysis.initialCompatibilityScore)
    }

    @Test
    fun testLuaModAnalysisDetectsScriptIncompatibility() {
        val samples = SampleMods.getSampleModList()
        val (meta, files) = samples[1] // Brimstone Horns Lua mod

        val analysis = engine.analyzeMod(meta, files)

        assertFalse(analysis.isPureResourceMod)
        assertTrue(analysis.luaFiles.isNotEmpty())
        assertTrue(analysis.issuesDetected.any { it.contains("The port runs no scripts") || it.contains("Lua") })
    }

    @Test
    fun testOfflineConversionStripsLuaAndMapsContentToResources() = runBlocking {
        val samples = SampleMods.getSampleModList()
        val (meta, files) = samples[1]
        val analysis = engine.analyzeMod(meta, files)

        val report = engine.convertMod(
            analysis = analysis,
            geminiService = null,
            apiKey = null
        )

        // Verifies main.lua is in removedScripts
        assertTrue(report.removedScripts.contains("main.lua"))

        // Verifies no .lua file is in output converted files
        assertFalse(report.convertedFiles.any { it.targetPath.endsWith(".lua") })

        // Verifies content/items.xml is restructured to resources/
        assertTrue(report.convertedFiles.any { it.targetPath.startsWith("resources/") })
    }
}
