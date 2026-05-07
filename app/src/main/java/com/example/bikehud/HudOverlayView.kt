package com.example.bikehud

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class HudOverlayView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    var speedKmh: Float = 0f
        set(v) { field = v; invalidate() }

    var odoKm: Float = 0f
        set(v) { field = v; invalidate() }

    // Ratios (0..1). CalibrateActivity updates these and stores in SharedPreferences.
    var y3m: Float = 0.75f
        set(v) { field = v; invalidate() }
    var y5m: Float = 0.62f
        set(v) { field = v; invalidate() }
    var y10m: Float = 0.48f
        set(v) { field = v; invalidate() }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(8f, 2f, 2f, Color.BLACK)
    }

    private fun linePaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = 10f
        style = Paint.Style.STROKE
        alpha = 210
        setShadowLayer(6f, 1f, 1f, Color.BLACK)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        c.drawText("Hız: ${"%.1f".format(speedKmh)} km/s", 40f, 80f, textPaint)
        c.drawText("KM : ${"%.2f".format(odoKm)}", 40f, 150f, textPaint)

        drawDistanceLine(c, y3m * height, Color.RED, "3 m")
        drawDistanceLine(c, y5m * height, Color.YELLOW, "5 m")
        drawDistanceLine(c, y10m * height, Color.GREEN, "10 m")
    }

    private fun drawDistanceLine(c: Canvas, yPx: Float, color: Int, label: String) {
        val y = max(0f, min(height.toFloat(), yPx))
        val p = linePaint(color)
        val left = width * 0.12f
        val right = width * 0.88f
        c.drawLine(left, y, right, y, p)
        c.drawText(label, right - 140f, y - 14f, textPaint)
    }
}
