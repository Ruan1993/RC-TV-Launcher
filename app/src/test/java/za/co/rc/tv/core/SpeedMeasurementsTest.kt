package za.co.rc.tv.core

import org.junit.Assert.*
import org.junit.Test

class SpeedMeasurementsTest {
    @Test fun tinyShortIncompleteSamplesCannotBecomeBroadbandResults() {
        assertNull(SpeedMeasurements.mbps(listOf(TransferSample(1024, 2_000_000_000))))
        assertNull(SpeedMeasurements.mbps(List(3) { TransferSample(32 * SpeedMeasurements.MIB, 200_000_000) }))
        assertNull(SpeedMeasurements.mbps(listOf(TransferSample(32 * SpeedMeasurements.MIB, 2_000_000_000))))
    }
    @Test fun weightedDecimalMbpsAndMedianAreCorrect() {
        assertEquals(16.0, SpeedMeasurements.mbps(listOf(TransferSample(4_000_000, 2_000_000_000), TransferSample(8_000_000, 4_000_000_000)))!!, .0001)
        assertEquals(2.5, SpeedMeasurements.medianMillis(listOf(4_000_000, 1_000_000, 2_000_000, 3_000_000))!!, .0001)
    }
}
