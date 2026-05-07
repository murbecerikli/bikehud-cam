package com.example.bikehud

import android.location.Location

class GpsOdometer {
    private var last: Location? = null
    private var filteredSpeedKmh = 0f
    var odoMeters: Double = 0.0
        private set

    fun update(loc: Location): Pair<Float, Double> {
        val rawSpeed = (loc.speed * 3.6f).coerceAtLeast(0f)
        filteredSpeedKmh = lowPass(filteredSpeedKmh, rawSpeed, 0.15f)

        last?.let { prev ->
            val d = prev.distanceTo(loc).toDouble()
            // filter out tiny jitter and huge jumps
            if (d in 0.3..50.0) odoMeters += d
        }
        last = loc
        return filteredSpeedKmh to odoMeters
    }

    private fun lowPass(old: Float, new: Float, alpha: Float) =
        old + alpha * (new - old)
}
