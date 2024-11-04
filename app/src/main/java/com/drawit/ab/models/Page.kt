package com.drawit.ab.models

import com.drawit.ab.views.DrawView

class Page(val index: Int) {
    private val paths: MutableList<Path> = mutableListOf()

    fun addPath(path: Path) {
        paths.add(path)
    }

    fun getPaths() = paths

    fun addPointToPath(point: DrawView.Point) {
        paths.lastOrNull()?.add(point)
    }

    fun setPaths(paths: List<Path>) {
        this.paths.clear()
        this.paths.addAll(paths)
    }
}
