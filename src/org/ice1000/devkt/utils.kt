package org.ice1000.devkt

import org.ice1000.devkt.config.ShortCut
import java.awt.event.KeyEvent
import javax.swing.JMenuItem
import javax.swing.KeyStroke

data class Quad<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D)

val selfLocation: String = Kotlin::class.java.protectionDomain.codeSource.location.file

val paired = mapOf(
		"\"" to "\"",
		"'" to "'",
		"“" to "”",
		"‘" to "’",
		"`" to "`",
		"`" to "`",
		"(" to ")",
		"（" to "）",
		"『" to "』",
		"「" to "」",
		"〖" to "〗",
		"【" to "】",
		"[" to "]",
		"〔" to "〕",
		"［" to "］",
		"{" to "}",
		"｛" to "｝",
		"<" to ">",
		"《" to "》",
		"〈" to "〉",
		"‹" to "›",
		"«" to "»"
)

/**
 * <kbd>Ctrl</kbd> for Windows/Linux, <kbd>Meta</kbd> for MacOS
 * Replacement of [java.awt.Toolkit.getMenuShortcutKeyMask]
 * @param key like [KeyEvent.VK_S]
 */
fun JMenuItem.keyMap(key: Int, modifiers: Int) {
	accelerator = KeyStroke.getKeyStroke(key, modifiers)
}

fun JMenuItem.keyMap(shortcut: ShortCut) = keyMap(shortcut.keyCode, shortcut.modifier)
