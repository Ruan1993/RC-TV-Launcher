package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test
import za.co.rc.tv.keyboard.*

class KeyboardModelTest {
    @Test fun layoutsContainRequiredTextAndActions() {
        val letters = KeyboardModel.rows(KeyboardMode.LETTERS, false).flatten()
        assertEquals(('a'..'z').toSet(), letters.filter { it.action == KeyAction.TEXT && it.text.length == 1 && it.text[0].isLetter() }.map { it.text[0] }.toSet())
        assertTrue(letters.any { it.text == " " })
        for (action in listOf(KeyAction.SHIFT, KeyAction.DELETE, KeyAction.ENTER, KeyAction.SWITCH, KeyAction.HIDE)) assertTrue(letters.any { it.action == action })
        assertTrue(KeyboardModel.rows(KeyboardMode.LETTERS, true).flatten().any { it.text == "Q" })
        assertTrue(KeyboardModel.rows(KeyboardMode.SYMBOLS, false).flatten().any { it.text == "@" })
    }
    @Test fun remoteNavigationWrapsHorizontallyAndStaysInsideRows() {
        val rows = KeyboardModel.rows(KeyboardMode.LETTERS, false)
        assertEquals(KeyPosition(0, 9), KeyboardModel.move(rows, KeyPosition(0, 0), horizontal = -1))
        assertEquals(0, KeyboardModel.move(rows, KeyPosition(0, 5), vertical = -1).row)
        val down = KeyboardModel.move(rows, KeyPosition(3, 4), vertical = 1)
        assertEquals(4, down.row)
        assertTrue(down.column in rows[4].indices)
        for (mode in KeyboardMode.entries) for ((r, row) in KeyboardModel.rows(mode, false).withIndex()) for (c in row.indices) {
            val layout = KeyboardModel.rows(mode, false)
            val next = KeyboardModel.move(layout, KeyPosition(r, c), vertical = 1)
            assertTrue(next.column in layout[next.row].indices)
        }
    }
}
