package kr.toxicity.hud.nms.v26_R2

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds vanilla components straight from adventure text components, without the json encode/decode round trip.
 * Hud components only use text, color, shadow color, decorations and fonts; anything else returns null so the
 * caller can fall back to the serializer.
 */
internal object AdventureConverter {
    private val fonts = ConcurrentHashMap<Key, FontDescription>()
    // keep named colors named, as the json path does
    private val namedColors = NamedTextColor.NAMES.values().associateWith { TextColor.parseColor(NamedTextColor.NAMES.keyOrThrow(it)).orThrow }

    fun convert(component: Component): MutableComponent? {
        if (component !is TextComponent) return null
        val style = convert(component.style()) ?: return null
        val result = net.minecraft.network.chat.Component.literal(component.content()).setStyle(style)
        for (child in component.children()) {
            result.append(convert(child) ?: return null)
        }
        return result
    }

    private fun convert(style: Style): net.minecraft.network.chat.Style? {
        if (style.clickEvent() != null || style.hoverEvent() != null || style.insertion() != null) return null
        if (style.isEmpty) return net.minecraft.network.chat.Style.EMPTY
        var result = net.minecraft.network.chat.Style.EMPTY
        style.color()?.let { result = result.withColor(namedColors[it] ?: TextColor.fromRgb(it.value())) }
        style.shadowColor()?.let { result = result.withShadowColor(it.value()) }
        style.font()?.let { key ->
            result = result.withFont(fonts.computeIfAbsent(key) { FontDescription.Resource(Identifier.fromNamespaceAndPath(it.namespace(), it.value())) })
        }
        decoration(style, TextDecoration.BOLD)?.let { result = result.withBold(it) }
        decoration(style, TextDecoration.ITALIC)?.let { result = result.withItalic(it) }
        decoration(style, TextDecoration.UNDERLINED)?.let { result = result.withUnderlined(it) }
        decoration(style, TextDecoration.STRIKETHROUGH)?.let { result = result.withStrikethrough(it) }
        decoration(style, TextDecoration.OBFUSCATED)?.let { result = result.withObfuscated(it) }
        return result
    }

    private fun decoration(style: Style, decoration: TextDecoration) = when (style.decoration(decoration)) {
        TextDecoration.State.TRUE -> true
        TextDecoration.State.FALSE -> false
        else -> null
    }
}
