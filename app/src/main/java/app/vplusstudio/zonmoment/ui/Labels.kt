package app.vplusstudio.zonmoment.ui

import androidx.annotation.StringRes
import app.vplusstudio.zonmoment.R
import app.vplusstudio.zonmoment.domain.Appliance
import app.vplusstudio.zonmoment.domain.RoofDirection
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@get:StringRes
val Appliance.label: Int
    get() = when (this) {
        Appliance.WASHING_MACHINE -> R.string.appliance_washing_machine
        Appliance.DISHWASHER -> R.string.appliance_dishwasher
        Appliance.DRYER -> R.string.appliance_dryer
        Appliance.EV_CHARGING -> R.string.appliance_ev
    }

val Appliance.emoji: String
    get() = when (this) {
        Appliance.WASHING_MACHINE -> "🧺"
        Appliance.DISHWASHER -> "🍽️"
        Appliance.DRYER -> "🌀"
        Appliance.EV_CHARGING -> "🚗"
    }

@get:StringRes
val RoofDirection.label: Int
    get() = when (this) {
        RoofDirection.SOUTH -> R.string.roof_south
        RoofDirection.SOUTH_EAST -> R.string.roof_south_east
        RoofDirection.SOUTH_WEST -> R.string.roof_south_west
        RoofDirection.EAST -> R.string.roof_east
        RoofDirection.WEST -> R.string.roof_west
        RoofDirection.EAST_WEST -> R.string.roof_east_west
    }

private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm")

fun ZonedDateTime.hhmm(): String = format(HOUR_MINUTE)
