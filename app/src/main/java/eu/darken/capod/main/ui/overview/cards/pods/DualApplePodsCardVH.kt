package eu.darken.capod.main.ui.overview.cards.pods

import android.view.ViewGroup
import eu.darken.capod.R
import eu.darken.capod.common.lists.binding
import eu.darken.capod.databinding.OverviewPodsAppleDualItemBinding
import eu.darken.capod.pods.core.HasDualPods.Pod
import eu.darken.capod.pods.core.apple.DualApplePods
import eu.darken.capod.pods.core.apple.DualApplePods.DeviceColor
import eu.darken.capod.pods.core.apple.DualApplePods.LidState
import eu.darken.capod.pods.core.getBatteryLevelCase
import eu.darken.capod.pods.core.getBatteryLevelLeftPod
import eu.darken.capod.pods.core.getBatteryLevelRightPod
import eu.darken.capod.pods.core.lastSeenFormatted
import java.time.Instant

class DualApplePodsCardVH(parent: ViewGroup) :
    PodDeviceVH<DualApplePodsCardVH.Item, OverviewPodsAppleDualItemBinding>(
        R.layout.overview_pods_apple_dual_item,
        parent
    ) {

    override val viewBinding = lazy { OverviewPodsAppleDualItemBinding.bind(itemView) }

    override val onBindData = binding(payload = true) { item: Item ->
        val device = item.device
        name.apply {
            val sb = StringBuilder(device.getLabel(context))
            if (device.deviceColor != DeviceColor.UNKNOWN) {
                sb.append(" (${device.getDeviceColorLabel(context)})")
            }
            text = sb
        }
        deviceIcon.setImageResource(device.iconRes)

        lastSeen.text = device.lastSeenFormatted(item.now)

        reception.text = device.getSignalQuality(context)

        podLeft.apply {
            val sb = StringBuilder(context.getString(R.string.pods_dual_left_label))
            sb.append("\n").append(device.getBatteryLevelLeftPod(context))
            when {
                device.isLeftPodCharging -> sb.append("\n").append("Charging")
                device.isLeftPodInEar -> sb.append("\n").append("In ear")
                else -> {}
            }
            text = sb
        }

        podRight.apply {
            val sb = StringBuilder("Right pod")
            sb.append("\n").append(device.getBatteryLevelRightPod(context))
            when {
                device.isRightPodCharging -> sb.append("\n").append("Charging")
                device.isRightPodInEar -> sb.append("\n").append("In ear")
                else -> {}
            }
            text = sb
        }

        when (device.microPhonePod) {
            Pod.LEFT -> podLeft.append("\n(Microphone)")
            Pod.RIGHT -> podRight.append("\n(Microphone)")
        }

        podCase.apply {
            val sb = StringBuilder("Case")
            sb.append("\n").append(device.getBatteryLevelCase(context))
            if (device.isCaseCharging) sb.append("\n").append("Charging")
            when (device.caseLidState) {
                LidState.OPEN -> sb.append("\n").append(context.getString(R.string.pods_case_status_open_label))
                LidState.CLOSED -> sb.append("\n").append(context.getString(R.string.pods_case_status_closed_label))
                LidState.NOT_IN_CASE,
                LidState.UNKNOWN -> {
                }
            }
            text = sb
        }

        status.apply {
            val sb = StringBuilder(device.getConnectionStateLabel(context))
            if (item.showDebug) {
                sb.append("\n\n").append("---Debug---")
                sb.append("\n").append(device.rawDataHex)
            }
            text = sb
        }
    }

    data class Item(
        override val now: Instant,
        override val device: DualApplePods,
        override val showDebug: Boolean,
    ) : PodDeviceVH.Item
}