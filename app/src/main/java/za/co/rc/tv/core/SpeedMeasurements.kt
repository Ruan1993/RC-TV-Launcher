package za.co.rc.tv.core

data class TransferSample(val bytes: Long, val nanos: Long)
object SpeedMeasurements {
    const val MIB = 1024L * 1024
    fun mbps(samples: List<TransferSample>): Double? {
        val useful = samples.filter { it.bytes >= MIB && it.nanos >= 1_200_000_000L }
        if (useful.size < 2 || useful.sumOf { it.bytes } < 8 * MIB) return null
        return useful.sumOf { it.bytes }.toDouble() * 8_000.0 / useful.sumOf { it.nanos }
    }
    fun medianMillis(nanos: List<Long>): Double? = nanos.filter { it > 0 }.sorted().let {
        if (it.isEmpty()) null else if (it.size % 2 == 1) it[it.size / 2] / 1e6 else (it[it.size / 2 - 1] / 2.0 + it[it.size / 2] / 2.0) / 1e6
    }
}
