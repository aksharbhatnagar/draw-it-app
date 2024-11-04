package com.example.myapplication.utils

import android.graphics.Color
import kotlin.random.Random

object ColorUtils {
    fun decreaseOpacity(color: Int, factor: Float = 0.3f): Int {
        val alpha = Color.alpha(color)
        val newAlpha = (alpha * factor).toInt() // Decrease alpha by the given factor
        return Color.argb(newAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    fun getRandomColor(): Int {
        val red = Random.nextInt(256)    // Random value between 0 and 255
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)

        return Color.rgb(red, green, blue)
    }
}
