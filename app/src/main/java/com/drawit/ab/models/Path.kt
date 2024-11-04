package com.drawit.ab.models

import com.drawit.ab.views.DrawView

data class Path(
    private val points: MutableList<DrawView.Point>,
    val color: Int = 0xFFFF8C00.toInt(),
    val addedStep: Int,
    var isVisible: Boolean = true
) {
    fun add(point: DrawView.Point) {
        points.add(point)
    }

    fun getPathPoints(): List<DrawView.Point> = points
}
