package eu.darken.capod.pods.core.airpods

import android.content.Context
import androidx.annotation.StringRes
import eu.darken.capod.R
import eu.darken.capod.common.debug.logging.log
import eu.darken.capod.common.isBitSet
import eu.darken.capod.common.lowerNibble
import eu.darken.capod.common.upperNibble
import eu.darken.capod.pods.core.DualPods
import eu.darken.capod.pods.core.DualPods.Pod

interface DualApplePods : ApplePods, DualPods {

    val tag: String

    // We start counting at the airpods prefix byte
    val rawPrefix: UByte
        get() = proximityMessage.data[0]

    val rawDeviceModel: UShort
        get() = (((proximityMessage.data[1].toInt() and 255) shl 8) or (proximityMessage.data[2].toInt() and 255)).toUShort()

    val rawStatus: UByte
        get() = proximityMessage.data[3]

    val rawPodsBattery: UByte
        get() = proximityMessage.data[4]

    val rawCaseBattery: UByte
        get() = proximityMessage.data[5]

    val rawCaseLidState: UByte
        get() = proximityMessage.data[6]

    val rawDeviceColor: UByte
        get() = proximityMessage.data[7]

    val rawSuffix: UByte
        get() = proximityMessage.data[8]

    override val microPhonePod: Pod
        get() = when (rawStatus.isBitSet(5)) {
            true -> Pod.LEFT
            false -> Pod.RIGHT
        }

    override val batteryLeftPodPercent: Float?
        get() {
            val value = when (microPhonePod) {
                Pod.LEFT -> rawPodsBattery.lowerNibble.toInt()
                Pod.RIGHT -> rawPodsBattery.upperNibble.toInt()
            }
            return when (value) {
                15 -> null
                else -> if (value > 10) {
                    log(tag) { "Left pod: Above 100% battery: $value" }
                    1.0f
                } else {
                    (value / 10f)
                }
            }
        }

    override val batteryRightPodPercent: Float?
        get() {
            val value = when (microPhonePod) {
                Pod.LEFT -> rawPodsBattery.upperNibble.toInt()
                Pod.RIGHT -> rawPodsBattery.lowerNibble.toInt()
            }
            return when (value) {
                15 -> null
                else -> if (value > 10) {
                    log(tag) { "Right pod: Above 100% battery: $value" }
                    1.0f
                } else {
                    value / 10f
                }
            }
        }

    override val batteryCasePercent: Float?
        get() = when (val value = rawCaseBattery.lowerNibble.toInt()) {
            15 -> null
            else -> if (value > 10) {
                log(tag) { "Case: Above 100% battery: $value" }
                1.0f
            } else {
                value / 10f
            }
        }

    override val isLeftPodInEar: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawStatus.isBitSet(1)
            Pod.RIGHT -> rawStatus.isBitSet(3)
        }

    override val isRightPodInEar: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawStatus.isBitSet(3)
            Pod.RIGHT -> rawStatus.isBitSet(1)
        }

    override val isCaseCharging: Boolean
        get() = rawCaseBattery.upperNibble.isBitSet(2)

    override val isLeftPodCharging: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawCaseBattery.upperNibble.isBitSet(0)
            Pod.RIGHT -> rawCaseBattery.upperNibble.isBitSet(1)
        }

    override val isRightPodCharging: Boolean
        get() = when (microPhonePod) {
            Pod.LEFT -> rawCaseBattery.upperNibble.isBitSet(1)
            Pod.RIGHT -> rawCaseBattery.upperNibble.isBitSet(0)
        }

    val caseLidState: LidState
        get() = LidState.values().firstOrNull { it.raw == rawCaseLidState } ?: LidState.UNKNOWN

    enum class LidState(val raw: UByte?) {
        OPEN(0x31),
        CLOSED(0x38),
        NOT_IN_CASE(0x01),
        UNKNOWN(null);

        constructor(raw: Int) : this(raw.toUByte())
    }

    val deviceColor: DeviceColor
        get() = DeviceColor.values().firstOrNull { it.raw == rawDeviceColor } ?: DeviceColor.UNKNOWN


    fun getDeviceColorLabel(context: Context): String = context.getString(deviceColor.labelRes)

    enum class DeviceColor(val raw: UByte?, @StringRes val labelRes: Int) {

        WHITE(0x00, R.string.pods_device_color_white_label),
        BLACK(0x01, R.string.pods_device_color_black_label),
        RED(0x02, R.string.pods_device_color_red_label),
        BLUE(0x03, R.string.pods_device_color_blue_label),
        PINK(0x04, R.string.pods_device_color_pink_label),
        GRAY(0x05, R.string.pods_device_color_gray_label),
        SILVER(0x06, R.string.pods_device_color_silver_label),
        GOLD(0x07, R.string.pods_device_color_gold_label),
        ROSE_GOLD(0x08, R.string.pods_device_color_rose_gold_label),
        SPACE_GRAY(0x09, R.string.pods_device_color_space_gray_label),
        DARK_BLUE(0x0a, R.string.pods_device_color_dark_blue_label),
        LIGHT_BLUE(0x0b, R.string.pods_device_color_light_blue_label),
        YELLOW(0x0c, R.string.pods_device_color_yellow_label),
        UNKNOWN(null, R.string.general_value_unknown_label);

        constructor(raw: Int, @StringRes labelRes: Int) : this(raw.toUByte(), labelRes)
    }

    val connectionState: ConnectionState
        get() = ConnectionState.values().firstOrNull { rawSuffix == it.raw } ?: ConnectionState.UNKNOWN

    fun getConnectionStateLabel(context: Context): String = context.getString(connectionState.labelRes)

    enum class ConnectionState(val raw: UByte?, @StringRes val labelRes: Int) {
        DISCONNECTED(0x00, R.string.pods_connection_state_disconnected_label),
        IDLE(0x04, R.string.pods_connection_state_idle_label),
        MUSIC(0x05, R.string.pods_connection_state_music_label),
        CALL(0x06, R.string.pods_connection_state_call_label),
        RINGING(0x07, R.string.pods_connection_state_ringing_label),
        HANGING_UP(0x09, R.string.pods_connection_state_hanging_up_label),
        UNKNOWN(null, R.string.general_value_unknown_label);

        constructor(raw: Int, @StringRes labelRes: Int) : this(raw.toUByte(), labelRes)
    }
}