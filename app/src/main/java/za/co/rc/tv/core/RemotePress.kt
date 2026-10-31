package za.co.rc.tv.core

/** One launch OR one context action per physical press, including repeat/cancel events. */
class RemotePress {
    private var active = false
    private var handled = false
    fun down(repeat: Int): Boolean {
        if (repeat != 0 || active) return false
        active = true
        handled = false
        return true
    }
    fun hold(): Boolean {
        if (!active || handled) return false
        handled = true
        return true
    }
    fun up(cancelled: Boolean): Boolean {
        val launch = active && !handled && !cancelled
        cancel()
        return launch
    }
    fun cancel() { active = false; handled = false }
}
