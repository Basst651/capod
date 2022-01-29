package eu.darken.capod.pods.core.apple.airpods

import eu.darken.capod.common.bluetooth.BleScanResult
import eu.darken.capod.common.debug.logging.logTag
import eu.darken.capod.pods.core.PodDevice
import eu.darken.capod.pods.core.apple.ApplePods
import eu.darken.capod.pods.core.apple.SingleApplePods
import eu.darken.capod.pods.core.apple.SingleApplePodsFactory
import eu.darken.capod.pods.core.apple.protocol.ProximityPairing
import java.time.Instant
import javax.inject.Inject

data class AirPodsMax(
    override val identifier: PodDevice.Id = PodDevice.Id(),
    override val seenLastAt: Instant = Instant.now(),
    override val seenFirstAt: Instant = Instant.now(),
    override val seenCounter: Int = 1,
    override val scanResult: BleScanResult,
    override val proximityMessage: ProximityPairing.Message,
    override val confidence: Float = PodDevice.BASE_CONFIDENCE,
    private val rssiAverage: Int? = null,
) : SingleApplePods {

    override val model: PodDevice.Model = PodDevice.Model.AIRPODS_MAX

    override val rssi: Int
        get() = rssiAverage ?: super.rssi

    class Factory @Inject constructor() : SingleApplePodsFactory(TAG) {

        override fun isResponsible(proximityMessage: ProximityPairing.Message): Boolean =
            proximityMessage.getModelInfo().dirty == DEVICE_CODE_DIRTY

        override fun create(scanResult: BleScanResult, proximityMessage: ProximityPairing.Message): ApplePods {
            var basic = AirPodsMax(scanResult = scanResult, proximityMessage = proximityMessage)
            val result = searchHistory(basic)

            if (result != null) basic = basic.copy(identifier = result.id)
            updateHistory(basic)

            if (result == null) return basic

            return basic.copy(
                identifier = result.id,
                seenFirstAt = result.seenFirstAt,
                seenCounter = result.seenCounter,
                confidence = result.confidence,
                rssiAverage = result.averageRssi(basic.rssi),
            )
        }

    }

    companion object {
        private val DEVICE_CODE_DIRTY = 10.toUByte()
        private val TAG = logTag("PodDevice", "Apple", "AirPods", "Max")
    }
}