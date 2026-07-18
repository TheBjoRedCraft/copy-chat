package dev.thebjoredcraft.copyChat.client.selection

import dev.thebjoredcraft.copyChat.mixin.client.ChatComponentAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.util.FormattedCharSequence
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Holds the current chat text selection (shift + mouse drag) and implements
 * hit-testing, highlight rendering and clipboard copy on top of the vanilla
 * chat geometry from [ChatComponent.extractRenderState].
 */
object ChatSelection {
    private const val HIGHLIGHT_COLOR = 0x8033A0FF.toInt()
    private const val LEFT_BUTTON = 0

    /** A caret between two characters of one visible chat line. */
    data class Caret(val line: GuiMessage.Line, val charIndex: Int)

    private var anchor: Caret? = null
    private var focus: Caret? = null
    private var dragging = false

    fun clear() {
        anchor = null
        focus = null
        dragging = false
    }

    // ---------------------------------------------------------------- input

    fun handleMouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != LEFT_BUTTON) return false
        if (!event.hasShiftDown()) {
            clear()
            return false
        }

        val caret = caretAt(event.x(), event.y(), clampToChat = false)
        if (caret == null) {
            clear()
            return false
        }

        anchor = caret
        focus = caret
        dragging = true
        return true
    }

    fun handleMouseDragged(event: MouseButtonEvent): Boolean {
        if (!dragging || event.button() != LEFT_BUTTON) return false
        caretAt(event.x(), event.y(), clampToChat = true)?.let { focus = it }
        return true
    }

    fun handleMouseReleased(event: MouseButtonEvent): Boolean {
        if (!dragging || event.button() != LEFT_BUTTON) return false
        dragging = false
        return true
    }

    fun handleKeyPressed(event: KeyEvent): Boolean {
        if (!event.isCopy) return false
        val text = selectedText() ?: return false
        Minecraft.getInstance().keyboardHandler.setClipboard(text)
        return true
    }

    // ------------------------------------------------------------ rendering

    /** Draws the selection highlight; called after the chat itself was extracted. */
    fun render(chat: ChatComponent, graphics: GuiGraphicsExtractor, font: Font) {
        val range = resolve(chat) ?: return
        val geometry = ChatGeometry(Minecraft.getInstance(), chat)
        val accessor = chat as ChatComponentAccessor
        val lines = accessor.copychatTrimmedMessages()
        val scroll = accessor.copychatChatScrollbarPos()

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(geometry.scale, geometry.scale)
        pose.translate(4.0f, 0.0f)

        for (slot in 0 until geometry.visibleCount) {
            val index = slot + scroll
            if (index < range.bottomIndex || index > range.topIndex || index !in lines.indices) continue

            val content = lines[index].content()
            val text = lineText(content)
            val fromChar = if (index == range.topIndex) range.topChar.coerceIn(0, text.length) else 0
            val toChar = if (index == range.bottomIndex) range.bottomChar.coerceIn(0, text.length) else text.length
            if (fromChar >= toChar) continue

            val x0 = prefixWidth(font, content, fromChar)
            val x1 = prefixWidth(font, content, toChar)
            if (x1 <= x0) continue

            val entryBottom = geometry.chatBottom - slot * geometry.entryHeight
            graphics.fill(x0, entryBottom - geometry.entryHeight, x1, entryBottom, HIGHLIGHT_COLOR)
        }

        pose.popMatrix()
    }

    // ---------------------------------------------------------- hit testing

    private fun caretAt(guiX: Double, guiY: Double, clampToChat: Boolean): Caret? {
        val minecraft = Minecraft.getInstance()
        val chat = minecraft.gui.hud.chat
        if (!chat.isChatFocused) return null

        val geometry = ChatGeometry(minecraft, chat)
        if (geometry.visibleCount <= 0) return null

        val localX = guiX / geometry.scale - 4.0
        val localY = guiY / geometry.scale

        var slot = floor((geometry.chatBottom - localY) / geometry.entryHeight).toInt()
        if (clampToChat) {
            slot = slot.coerceIn(0, geometry.visibleCount - 1)
        } else if (slot < 0 || slot >= geometry.visibleCount) {
            return null
        }

        val accessor = chat as ChatComponentAccessor
        val lines = accessor.copychatTrimmedMessages()
        val index = slot + accessor.copychatChatScrollbarPos()
        val line = lines.getOrNull(index) ?: return null

        return Caret(line, charIndexAtX(minecraft.font, line.content(), localX.toFloat()))
    }

    // ------------------------------------------------------------ selection

    private class Range(val topIndex: Int, val topChar: Int, val bottomIndex: Int, val bottomChar: Int)

    /**
     * Resolves the anchor/focus lines to their current positions in the trimmed
     * message list. Uses identity so the selection survives new incoming lines
     * (which are prepended) and silently disappears when the chat is rebuilt.
     */
    private fun resolve(chat: ChatComponent): Range? {
        val a = anchor ?: return null
        val f = focus ?: return null
        val lines = (chat as ChatComponentAccessor).copychatTrimmedMessages()

        var anchorIndex = -1
        var focusIndex = -1
        for (i in lines.indices) {
            if (lines[i] === a.line) anchorIndex = i
            if (lines[i] === f.line) focusIndex = i
        }
        if (anchorIndex < 0 || focusIndex < 0) return null

        // Larger trimmed index = higher up on screen = earlier in reading order.
        return when {
            anchorIndex == focusIndex ->
                Range(anchorIndex, minOf(a.charIndex, f.charIndex), focusIndex, maxOf(a.charIndex, f.charIndex))
            anchorIndex > focusIndex -> Range(anchorIndex, a.charIndex, focusIndex, f.charIndex)
            else -> Range(focusIndex, f.charIndex, anchorIndex, a.charIndex)
        }
    }

    private fun selectedText(): String? {
        val chat = Minecraft.getInstance().gui.hud.chat
        val range = resolve(chat) ?: return null
        val lines = (chat as ChatComponentAccessor).copychatTrimmedMessages()

        val builder = StringBuilder()
        for (index in range.topIndex downTo range.bottomIndex) {
            val text = lineText(lines[index].content())
            val from = if (index == range.topIndex) range.topChar.coerceIn(0, text.length) else 0
            val to = if (index == range.bottomIndex) range.bottomChar.coerceIn(0, text.length) else text.length
            if (index != range.topIndex) builder.append('\n')
            if (from < to) builder.append(text, from, to)
        }
        return builder.toString().takeIf { it.isNotEmpty() }
    }

    // ------------------------------------------------------------ text math

    private fun lineText(sequence: FormattedCharSequence): String {
        val builder = StringBuilder()
        sequence.accept { _, _, codePoint ->
            builder.appendCodePoint(codePoint)
            true
        }
        return builder.toString()
    }

    /** A view of [sequence] restricted to the char (not codepoint) range [from, to). */
    private fun subSequence(sequence: FormattedCharSequence, from: Int, to: Int): FormattedCharSequence {
        if (from >= to) return FormattedCharSequence.EMPTY
        return FormattedCharSequence { sink ->
            var position = 0
            sequence.accept { _, style, codePoint ->
                val charCount = Character.charCount(codePoint)
                val keepGoing = if (position >= from && position + charCount <= to) {
                    sink.accept(position - from, style, codePoint)
                } else {
                    true
                }
                position += charCount
                keepGoing && position < to
            }
            true
        }
    }

    private fun prefixWidth(font: Font, sequence: FormattedCharSequence, chars: Int): Int =
        if (chars <= 0) 0 else font.splitter.stringWidth(subSequence(sequence, 0, chars)).roundToInt()

    private fun charIndexAtX(font: Font, sequence: FormattedCharSequence, x: Float): Int {
        if (x <= 0.0f) return 0
        var accumulated = 0.0f
        var index = 0
        var hit = -1
        sequence.accept { _, style, codePoint ->
            val width = font.splitter.stringWidth(FormattedCharSequence.codepoint(codePoint, style))
            if (x < accumulated + width / 2.0f) {
                hit = index
                false
            } else {
                accumulated += width
                index += Character.charCount(codePoint)
                true
            }
        }
        return if (hit >= 0) hit else index
    }

    /** Mirrors the layout constants of [ChatComponent.extractRenderState]. */
    private class ChatGeometry(minecraft: Minecraft, chat: ChatComponent) {
        val scale: Float = minecraft.options.chatScale().get().toFloat()
        val entryHeight: Int
        val chatBottom: Int
        val visibleCount: Int

        init {
            val lineSpacing = minecraft.options.chatLineSpacing().get()
            entryHeight = (9.0 * (lineSpacing + 1.0)).toInt()
            chatBottom = floor((minecraft.window.guiScaledHeight - 40) / scale.toDouble()).toInt()

            val accessor = chat as ChatComponentAccessor
            val lineCount = accessor.copychatTrimmedMessages().size - accessor.copychatChatScrollbarPos()
            visibleCount = minOf(lineCount, chat.linesPerPage)
        }
    }
}
