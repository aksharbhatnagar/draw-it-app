package com.example.myapplication.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.myapplication.Page

class StoryboardView(context: Context, attrs: AttributeSet): View(context, attrs) {
    private val paint = Paint().apply {
        color = 0xFF000000.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f // change later
    }
    private var originalCanvasWidth = 1
    private var originalCanvasHeight = 1
    private var displayPage: Page? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw paths from the current page
        val currPage = displayPage ?: return
        for (path in currPage.getPaths()) {
            paint.color = path.color
            // canvas.drawPath(pathData.path, paint)
            // setBitmapPoints(pathData.getPathPoints())
            if (path.isVisible) {
                drawPathOnCanvas(canvas, path.getPathPoints(), path.color)
            }
        }
    }

    private fun drawPathOnCanvas(canvas: Canvas, points: List<DrawView.Point>, color: Int) {
        paint.color = color
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            if (!end.isErased) {
                canvas.drawLine(scaledX(start.x), scaledY(start.y), scaledX(end.x), scaledY(end.y), paint)
            }
        }
    }

    private fun scaledX(x: Float): Float {
        return (x / originalCanvasWidth * width)
    }
    private fun scaledY(y: Float): Float {
        return (y / originalCanvasHeight * height)
    }

    fun setPage(page: Page) {
        displayPage = page
    }

    fun setCanvasSize(w: Int, h: Int) {
        originalCanvasWidth = w
        originalCanvasHeight = h
    }
}
