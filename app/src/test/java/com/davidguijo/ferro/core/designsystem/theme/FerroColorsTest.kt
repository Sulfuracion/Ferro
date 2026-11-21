package com.davidguijo.ferro.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FerroColorsTest {
    @Test
    fun `dark and light palettes contain all 21 tokens from tokens json`() {
        val expectedTokens =
            setOf(
                "bg",
                "surface",
                "surface-raised",
                "line",
                "line-strong",
                "ink",
                "ink-muted",
                "ink-subtle",
                "brasa",
                "brasa-pressed",
                "on-brasa",
                "brasa-text",
                "brasa-soft",
                "iron",
                "success",
                "warning",
                "danger",
                "set-warmup",
                "set-failure",
                "set-drop",
                "focus"
            )

        val darkMap = DarkFerroColors.asTokenMap()
        val lightMap = LightFerroColors.asTokenMap()

        assertEquals(expectedTokens, darkMap.keys)
        assertEquals(expectedTokens, lightMap.keys)
        assertTrue(darkMap.values.none { it == Color.Unspecified })
        assertTrue(lightMap.values.none { it == Color.Unspecified })
    }

    @Test
    fun `dark and light palettes match tokens json values and resolved aliases`() {
        val tokensFile =
            listOf(
                File("docs/design/tokens.json"),
                File("../docs/design/tokens.json")
            ).firstOrNull { it.exists() }
                ?: error("No se encontró docs/design/tokens.json")

        val root = Json.parseToJsonElement(tokensFile.readText()).jsonObject
        val tokenArray = root.getValue("color").jsonObject.getValue("tokens").jsonArray

        val rawDark = mutableMapOf<String, String>()
        val rawLight = mutableMapOf<String, String>()
        for (element in tokenArray) {
            val obj = element.jsonObject
            val name = obj.getValue("name").jsonPrimitive.content
            val valueObj = obj.getValue("value").jsonObject
            rawDark[name] = valueObj.getValue("dark").jsonPrimitive.content
            rawLight[name] = valueObj.getValue("light").jsonPrimitive.content
        }

        val darkMap = DarkFerroColors.asTokenMap()
        val lightMap = LightFerroColors.asTokenMap()

        assertEquals(rawDark.keys, darkMap.keys)
        assertEquals(rawLight.keys, lightMap.keys)

        for (name in rawDark.keys) {
            val expectedDark = resolveHex(rawDark.getValue(name), rawDark)
            val expectedLight = resolveHex(rawLight.getValue(name), rawLight)
            assertEquals("Dark token '$name'", expectedDark, darkMap.getValue(name).toHexString())
            assertEquals("Light token '$name'", expectedLight, lightMap.getValue(name).toHexString())
        }
    }

    private fun resolveHex(raw: String, table: Map<String, String>): String {
        if (!raw.startsWith("{")) return raw.uppercase()
        val alias = raw.removePrefix("{").removeSuffix("}")
        return resolveHex(table.getValue(alias), table)
    }

    private fun Color.toHexString(): String {
        val rgb = toArgb() and 0x00FFFFFF
        return "#%06X".format(rgb)
    }
}
