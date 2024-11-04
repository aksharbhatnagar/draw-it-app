package com.drawit.ab.utils

import android.os.SystemClock
import android.view.View

object ClickUtils {
    fun View.setDebouncedClickListener(debounceTime: Long = 300L, onClick: (View) -> Unit) {
        var lastClickTime = 0L

        this.setOnClickListener {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime > debounceTime) {
                lastClickTime = currentTime
                onClick(it)
            }
        }
    }
} // 5 6 4 3 7
