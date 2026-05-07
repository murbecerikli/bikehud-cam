package com.example.bikehud

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class CalibrateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrate)

        val prefs = getSharedPreferences("bikehud", Context.MODE_PRIVATE)

        fun bind(id: Int, key: String, def: Float) {
            val sb = findViewById<SeekBar>(id)
            val cur = prefs.getFloat(key, def)
            sb.max = 1000
            sb.progress = (cur * 1000).toInt()
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    prefs.edit().putFloat(key, progress / 1000f).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        bind(R.id.sb3m, "y3m", 0.75f)
        bind(R.id.sb5m, "y5m", 0.62f)
        bind(R.id.sb10m, "y10m", 0.48f)
    }
}
