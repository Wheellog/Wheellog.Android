package com.cooper.wheellog

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.Collator
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class InfoBlocksTest {
    private lateinit var context: Context
    private fun localized(language: String): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppConfig(context) // Seed the settings version before installing legacy preferences.
    }

    @Test fun `Russian legacy titles migrate even when first reopened in English`() {
        val ru = localized("ru")
        val en = localized("en")
        val ids = listOf("temperature2", "voltage", "top_speed", "distance", "average_riding_speed")
        val titles = InfoBlocks(ru).titles
        AppConfig(ru).setValue(R.string.view_blocks_string, ids.joinToString(";") { ru.getString(titles.getValue(it)) })
        val english = AppConfig(en)
        assertEquals(ids, english.viewBlockIds)
        assertEquals(ids.joinToString(";"), english.getValue(R.string.view_blocks_string, ""))
        assertEquals(ids.map { en.getString(titles.getValue(it)) }, english.viewBlocks.toList())
        assertEquals(ids, AppConfig(ru).viewBlockIds)
    }

    @Test fun `all English and Russian block selections survive language round trips`() {
        for (language in listOf("ru", "en")) {
            val source = localized(language)
            val blocks = InfoBlocks(source)
            val ids = blocks.titles.keys.reversed()
            AppConfig(source).setValue(R.string.view_blocks_string, ids.joinToString(";") { source.getString(blocks.titles.getValue(it)) })
            assertEquals(ids, AppConfig(localized(if (language == "en") "ru" else "en")).viewBlockIds)
            assertEquals(ids, AppConfig(source).viewBlockIds)
        }
    }

    @Test fun `empty selection remains empty and defaults match in either language`() {
        assertEquals(InfoBlocks(context).defaults, AppConfig(localized("ru")).viewBlockIds)
        assertEquals(AppConfig(localized("ru")).viewBlockIds, AppConfig(localized("en")).viewBlockIds)
        AppConfig(context).viewBlockIds = emptyList()
        assertTrue(AppConfig(localized("en")).viewBlocks.isEmpty())
        assertTrue(AppConfig(localized("ru")).viewBlockIds.isEmpty())
    }

    @Test fun `unknown legacy entries are retained without hiding recognised blocks`() {
        val config = AppConfig(localized("en"))
        config.setValue(R.string.view_blocks_string, "unrecognised-legacy-block;Voltage;Voltage")
        assertEquals(listOf("voltage"), config.viewBlockIds)
        assertEquals("unrecognised-legacy-block;voltage", config.getValue(R.string.view_blocks_string, ""))
    }

    @Test fun `reordered blocks and disabled choices survive locale and process recreation`() {
        val config = AppConfig(localized("ru"))
        config.viewBlockIds = listOf("distance", "voltage", "pwm")
        config.viewBlockOrder = listOf("temperature", "pwm", "distance", "voltage")
        val restored = AppConfig(localized("en"))
        assertEquals(listOf("pwm", "distance", "voltage"), restored.viewBlockIds)
        assertEquals(listOf("temperature", "pwm", "distance", "voltage"), restored.viewBlockOrder.take(4))
        assertFalse("temperature" in restored.viewBlockIds)
        // Replacing a tile by long press must persist a stable key as well.
        restored.viewBlocks = arrayOf("Voltage", "Battery")
        assertEquals(listOf("voltage", "battery"), AppConfig(localized("ru")).viewBlockIds)
        assertEquals(listOf("voltage", "battery"), AppConfig(context).viewBlockOrder.take(2))
    }

    @Test fun `presets preserve exact blocks and order across languages and support edits and deletion`() {
        val config = AppConfig(localized("ru"))
        val order = listOf("pwm", "battery", "voltage")
        val saved = config.infoBlockPresets.save("Город", order, setOf("pwm", "voltage"))
        val restored = AppConfig(localized("en"))
        assertEquals(saved, restored.infoBlockPresets.read().single())
        val edited = restored.infoBlockPresets.save("City", listOf("voltage", "battery", "pwm"), setOf("battery", "pwm"), saved.id)
        assertEquals(saved.id, edited.id)
        val preset = AppConfig(localized("ru")).infoBlockPresets.read().single()
        assertEquals(edited, preset)
        restored.viewBlockIds = preset.order.filter { it in preset.enabled }
        restored.viewBlockOrder = preset.order
        assertEquals(listOf("battery", "pwm"), restored.viewBlockIds)
        restored.infoBlockPresets.delete(saved.id)
        assertTrue(AppConfig(context).infoBlockPresets.read().isEmpty())
        assertEquals(listOf("battery", "pwm"), restored.viewBlockIds)
    }

    @Test fun `ten preset limit rejects an eleventh while allowing edits and new saves after deletion`() {
        val store = AppConfig(context).infoBlockPresets
        val presets = (1..10).map { store.save("Preset $it", listOf("pwm"), setOf("pwm")) }
        assertThrows(IllegalArgumentException::class.java) { store.save("Overflow", emptyList(), emptySet()) }
        assertEquals(10, store.read().size)
        store.save("Renamed", listOf("battery"), setOf("battery"), presets.first().id)
        assertEquals(10, store.read().size)
        assertThrows(IllegalArgumentException::class.java) { store.save("renamed", emptyList(), emptySet(), presets.last().id) }
        store.delete(presets.first().id)
        store.save("New", emptyList(), emptySet())
        assertEquals(10, store.read().size)
    }

    @Test fun `deleting or cancelling a draft does not change current blocks`() {
        val config = AppConfig(context)
        config.viewBlockIds = listOf("distance", "voltage")
        val preset = config.infoBlockPresets.save("Empty", emptyList(), emptySet())
        assertTrue(preset.enabled.isEmpty())
        config.infoBlockPresets.delete(preset.id)
        assertEquals(listOf("distance", "voltage"), config.viewBlockIds)
        assertThrows(IllegalArgumentException::class.java) { config.infoBlockPresets.save(" ", emptyList(), emptySet()) }
    }

    @Test fun `speed correction defaults clamps and per wheel settings survive language changes`() {
        val config = AppConfig(localized("ru"))
        config.lastMac = "AA:BB:01"
        assertFalse(config.useSpeedCorrection)
        assertEquals(0.0, config.speedCorrectionPercent, 0.0)
        config.speedCorrectionPercent = -99.0
        config.useSpeedCorrection = true
        assertEquals(-10.0, config.appliedSpeedCorrectionPercent, 0.0)
        config.speedCorrectionPercent = -10.0
        val en = AppConfig(localized("en"))
        assertEquals(-10.0, en.appliedSpeedCorrectionPercent, 0.0)
        en.lastMac = "AA:BB:02"
        assertEquals(0.0, en.speedCorrectionPercent, 0.0)
        assertFalse(en.useSpeedCorrection)
        en.speedCorrectionPercent = 100.0
        en.useSpeedCorrection = true
        assertEquals(10.0, en.appliedSpeedCorrectionPercent, 0.0)
        en.lastMac = "AA:BB:01"
        assertEquals(-10.0, en.appliedSpeedCorrectionPercent, 0.0)
        en.useSpeedCorrection = false
        assertEquals(0.0, en.appliedSpeedCorrectionPercent, 0.0)
        assertEquals(-10.0, en.speedCorrectionPercent, 0.0)
    }

    private fun assertEditorGroups(blocks: InfoBlocks, ctx: Context, result: List<String>, active: List<String>) {
        assertEquals(active, result.take(active.size))
        assertEquals(blocks.titles.keys, result.toSet())
        assertEquals(result.size, result.distinct().size)
        val collator = Collator.getInstance(LocaleManager.getLocale(ctx.resources))
        result.drop(active.size).zipWithNext().forEach { (left, right) ->
            assertTrue("inactive choices are out of alphabetical order", collator.compare(
                ctx.getString(blocks.titles.getValue(left)), ctx.getString(blocks.titles.getValue(right))) <= 0)
        }
    }

    @Test fun `editor groups selected blocks first and sorts other translated names in both languages`() {
        val active = listOf("voltage", "distance", "pwm")
        for (language in listOf("ru", "en")) {
            val ctx = localized(language)
            val blocks = InfoBlocks(ctx)
            val interleaved = listOf("battery", "voltage", "temperature", "distance", "pwm")
            assertEditorGroups(blocks, ctx, blocks.editorOrder(interleaved, active.toSet()), active)
        }
    }

    @Test fun `newly enabled blocks append and disabled blocks return to alphabetical choices`() {
        val ctx = localized("ru")
        val blocks = InfoBlocks(ctx)
        val active = listOf("distance", "voltage")
        val initial = blocks.editorOrder(active + blocks.titles.keys, active.toSet())
        val enabled = blocks.toggleInEditor(initial, active.toSet(), "battery", true)
        assertEditorGroups(blocks, ctx, enabled, active + "battery")
        val disabled = blocks.toggleInEditor(enabled, (active + "battery").toSet(), "distance", false)
        assertEditorGroups(blocks, ctx, disabled, listOf("voltage", "battery"))
        val reenabled = blocks.toggleInEditor(disabled, setOf("voltage", "battery"), "distance", true)
        assertEditorGroups(blocks, ctx, reenabled, listOf("voltage", "battery", "distance"))
    }

    @Test fun `empty and full selections remain grouped without losing any block`() {
        val ctx = localized("en")
        val blocks = InfoBlocks(ctx)
        val all = blocks.titles.keys.reversed()
        assertEditorGroups(blocks, ctx, blocks.editorOrder(all, emptySet()), emptyList())
        assertEquals(all, blocks.editorOrder(all, all.toSet()))
        val single = blocks.toggleInEditor(all, emptySet(), "pwm", true)
        assertEditorGroups(blocks, ctx, single, listOf("pwm"))
        assertEditorGroups(blocks, ctx, blocks.toggleInEditor(single, setOf("pwm"), "pwm", false), emptyList())
    }

    @Test fun `saved editor and preset order survive reopening in another language`() {
        val ru = localized("ru")
        val config = AppConfig(ru)
        val blocks = InfoBlocks(ru)
        val active = listOf("pwm", "voltage", "distance")
        val order = blocks.editorOrder(listOf("temperature") + active, active.toSet())
        config.viewBlockIds = active
        config.viewBlockOrder = order
        val preset = config.infoBlockPresets.save("City", order, active.toSet())
        val en = localized("en")
        val restored = AppConfig(en)
        val english = InfoBlocks(en)
        assertEquals(active, restored.viewBlockIds)
        assertEditorGroups(english, en, english.editorOrder(restored.viewBlockOrder, restored.viewBlockIds.toSet()), active)
        val loaded = restored.infoBlockPresets.read().single()
        assertEquals(preset, loaded)
        assertEditorGroups(english, en, english.editorOrder(loaded.order, loaded.enabled), active)
    }
}
