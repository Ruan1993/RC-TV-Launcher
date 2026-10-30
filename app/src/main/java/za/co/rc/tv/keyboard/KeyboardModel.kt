package za.co.rc.tv.keyboard

enum class KeyboardMode { LETTERS, SYMBOLS, NUMBERS }
enum class KeyAction { TEXT, SHIFT, MODE, DELETE, ENTER, SWITCH, HIDE }
data class TvKey(val label: String, val text: String = label, val action: KeyAction = KeyAction.TEXT, val weight: Float = 1f)
data class KeyPosition(val row: Int, val column: Int)

object KeyboardModel {
    fun rows(mode: KeyboardMode, shift: Boolean, enter: String = "Enter"): List<List<TvKey>> {
        fun letters(text: String) = text.map { TvKey(if (shift) it.uppercaseChar().toString() else it.toString()) }
        val delete = TvKey("⌫", action = KeyAction.DELETE, weight = 1.5f)
        val content = when (mode) {
            KeyboardMode.LETTERS -> listOf(letters("1234567890"), letters("qwertyuiop"), letters("asdfghjkl"),
                listOf(TvKey(if (shift) "SHIFT" else "Shift", action = KeyAction.SHIFT, weight = 1.5f)) + letters("zxcvbnm") + delete)
            KeyboardMode.SYMBOLS -> listOf("!@#$%^&*()", "-_=+[]{}\\|", ";:'\",.<>/?", "`~€£¥•…").map { letters(it) }.let { it.dropLast(1) + listOf(it.last() + delete) }
            KeyboardMode.NUMBERS -> listOf("123", "456", "789", "-0.").map { letters(it) }.let { it.dropLast(1) + listOf(it.last() + delete) }
        }
        val modeLabel = when (mode) { KeyboardMode.LETTERS -> "?123"; KeyboardMode.SYMBOLS -> "123"; KeyboardMode.NUMBERS -> "ABC" }
        return content + listOf(listOf(TvKey(modeLabel, action = KeyAction.MODE, weight = 1.5f), TvKey("Space", " ", weight = 4f),
            TvKey("Switch", action = KeyAction.SWITCH, weight = 1.5f), TvKey(enter, action = KeyAction.ENTER, weight = 2f), TvKey("Hide", action = KeyAction.HIDE, weight = 1.5f)))
    }
    fun move(rows: List<List<TvKey>>, from: KeyPosition, horizontal: Int = 0, vertical: Int = 0): KeyPosition {
        if (horizontal != 0) return from.copy(column = (from.column + horizontal).mod(rows[from.row].size))
        val row = (from.row + vertical).coerceIn(0, rows.lastIndex)
        fun centres(keys: List<TvKey>): List<Float> {
            val total = keys.sumOf { it.weight.toDouble() }.toFloat(); var x = 0f
            return keys.map { val centre = (x + it.weight / 2) / total; x += it.weight; centre }
        }
        val x = centres(rows[from.row])[from.column]
        val next = centres(rows[row])
        return KeyPosition(row, next.indices.minBy { kotlin.math.abs(next[it] - x) })
    }
}
