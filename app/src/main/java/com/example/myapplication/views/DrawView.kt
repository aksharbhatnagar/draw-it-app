package com.example.myapplication.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.myapplication.viewmodel.DrawViewModel
import com.example.myapplication.models.Page
import com.example.myapplication.utils.ColorUtils
import kotlin.math.abs

class DrawView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var pageEventsListener: PageEventsListener? = null
    private val paint = Paint().apply {
        color = 0xFF000000.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 5f // change later
    }
    private val eraserBorderPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var eraserSize = 0.03f
    private var pages: List<Page> = listOf()

    private var lastPoint: Point? = null

    private var currentPageIndex = 0
    private var bitmap: Bitmap? = null

    private var drawMode = DrawViewModel.DrawMode.PENCIL

    private var playbackPage: Page? = null

    // Zooming variables
    private var scaleFactor = 1f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    companion object {
        private const val SCALE_FACTOR = 1000 // for fixed-point representation
        private const val MIN_DISTANCE = 0.015f // min relative distance to add next point in path
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//        canvas.save()
//        canvas.scale(scaleFactor, scaleFactor) // Apply scaling to the canvas

        if (drawMode == DrawViewModel.DrawMode.PLAYBACK) {
            val page = playbackPage ?: return
            for (path in page.getPaths()) {
                paint.color = path.color
                if (path.isVisible) {
                    drawPathOnCanvas(canvas, path.getPathPoints(), path.color)
                }
            }
            return
        }

        Log.e("akshar", "DrawView ondraw, pages size : ${pages.size}")
        if (pages.isEmpty()) {
            return
        }
        // if need to draw all pages
//        for (i in pages.indices) {
//            if (i != currentPageIndex) {
//                for (pathData in pages[i].getPaths()) {
//                    paint.color = pathData.color
//                    paint.setShadowLayer(10f, 0f, 0f, Color.argb(50, Color.red(pathData.color), Color.green(pathData.color), Color.blue(pathData.color)))
//                    // canvas.drawPath(pathData.path, paint)
//                    setBitmapPoints(pathData.getPathPoints(), pathData.color)
//                }
//            }
//        }

        // Draw paths from the current page
        if (currentPageIndex in pages.indices) {
            for (path in pages[currentPageIndex].getPaths()) {
                Log.e("akshar", "DrawView ondraw, drawing page $currentPageIndex")
                paint.color = path.color
                // canvas.drawPath(pathData.path, paint)
                // setBitmapPoints(pathData.getPathPoints())
                if (path.isVisible) {
                    drawPathOnCanvas(canvas, path.getPathPoints(), path.color)
                }
            }
        }

        // draw blurred paths of previous page
        if (currentPageIndex >= 1) {
            Log.e("akshar", "DrawView ondraw, drawing page ${currentPageIndex - 1}")
            if (currentPageIndex - 1 in pages.indices) {
                for (path in pages[currentPageIndex - 1].getPaths()) {
                    // paint.color = ColorUtils.decreaseOpacity(pathData.color)
                    // paint.setShadowLayer(10f, 0f, 0f, Color.argb(50, Color.red(pathData.color), Color.green(pathData.color), Color.blue(pathData.color)))
                    // canvas.drawPath(pathData.path, paint)
                    // setBitmapPoints(pathData.getPathPoints())
                    if (path.isVisible) {
                        drawPathOnCanvas(
                            canvas,
                            path.getPathPoints(),
                            ColorUtils.decreaseOpacity(path.color)
                        )
                    }
                }
            }
        }

//        bitmap?.let {
//            Log.e("akshar", "draw bitmap ")
//            canvas.drawBitmap(it, 0f, 0f, paint)
//        }
        // Draw paths from other pages with a blurred effect

//        paths.forEach { // draw each path with its color
//            val points = it.getPathPoints()
//            points.forEachIndexed { index, _ ->
//                if (index < points.size - 1) {
//                    val start = points[index]
//                    val end = points[index + 1]
//                    canvas.drawLine(start.x, start.y, end.x, end.y, paint)
//                }
//            }
//        }

        if (drawMode == DrawViewModel.DrawMode.ERASER) {
            lastPoint?.let {
                canvas.drawCircle(it.relativeX * width, it.relativeY * height, eraserSize * width, eraserBorderPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return true
        return when (drawMode) {
            DrawViewModel.DrawMode.PENCIL -> {
                onTouchPencil(event)
            }
            DrawViewModel.DrawMode.ERASER -> {
                onTouchEraser(event)
            }
            else -> {
                true
            }
        }
    }

    private fun onTouchPencil(event: MotionEvent): Boolean {
        val relativeX = event.x / width
        val relativeY = event.y / height

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.e("akshar", "actiono down")
                val point = Point(relativeX, relativeY)
                lastPoint = point
                pageEventsListener?.onCreateNewPath() // todo check if we can draw dots
                pageEventsListener?.onAddPoint(point)
            }
            MotionEvent.ACTION_MOVE -> {
                lastPoint?.let {
                    if (relativeX - abs(it.relativeX - relativeX) >= MIN_DISTANCE && abs(it.relativeY - relativeX) >= MIN_DISTANCE) {
                        val newPoint = Point(relativeX, relativeY)
                        // pages[currentPageIndex].addPointToPath(lastPoint!!)
                        pageEventsListener?.onAddPoint(newPoint)
                        lastPoint = newPoint
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.e("akshar", "action up")
                Log.e("akshar", "added path to current page")

                Log.e("akshar", "action up touchPencil calling saveDocument")
                if (currentPageIndex in pages.indices) {
                    pageEventsListener?.onSave()
                }
            }
        }
        return true
    }

    private fun onTouchEraser(event: MotionEvent): Boolean {
        val relativeX = event.x / width
        val relativeY = event.y / height

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val point = Point(relativeX, relativeY)
                pageEventsListener?.onErase(point)
                pageEventsListener?.onEraseStart()
                lastPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                val point = Point(relativeX, relativeY)
                pageEventsListener?.onErase(point)
                lastPoint = point
            }
            MotionEvent.ACTION_UP -> {
                Log.e("akshar", "action up touch eraser calling saveDocument")
                pageEventsListener?.onSave()
            }
        }
        return true
    }

    private fun createBitmap(width: Int, height: Int) {
        // Create a bitmap of the same size as the view
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun setBitmapPoints(points: List<Point>) {
        // bitmap?.eraseColor(Color.BLACK)
        // Log.e("akshar", "set bitmap points called, points size : ${points.size}")
//        for (point in points) {
//            if (point.x.toInt() in 0 until width && point.y.toInt() in 0 until height) {
//                Log.e("akshar", "added point to bitmap")
//                bitmap?.setPixel(point.x.toInt(), point.y.toInt(), color)
//            }
//        }

        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

//            // Set the color for the line
//            paint.color = start.color // You can customize how to set colors

            // Draw line between points
            bitmap?.let {
                val canvas = Canvas(it)
                canvas.drawLine(start.relativeX, start.relativeY, end.relativeX, end.relativeY, paint)
            }
        }
    }

    private fun drawPathOnCanvas(canvas: Canvas, points: List<Point>, color: Int) {
        paint.color = color
        if (points.size == 1) { // draw dot and return
            val dot = points.first()
            canvas.drawPoint(dot.relativeX * width, dot.relativeY * height, paint)
        }
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            if (!end.isErased) {
                canvas.drawLine(
                    start.relativeX * width,
                    start.relativeY * height,
                    end.relativeX * width,
                    end.relativeY * height,
                paint)
            }
        }
    }

    fun setPages(pages: List<Page>) {
        this.pages = pages
        invalidate()
    }

    fun setPaintColor(color: Int) {
        paint.color = color
    }

    fun setCurrentPageIndex(index: Int) {
        currentPageIndex = index
    }

    fun setOnSavePathsListener(listener: PageEventsListener) {
        this.pageEventsListener = listener
    }

    fun setMode(mode: DrawViewModel.DrawMode) {
        this.drawMode = mode
        invalidate()
    }

    fun setEraserSize(size: Float) {
        eraserSize = size
    }

    fun setPlaybackPage(page: Page) {
        playbackPage = page
        invalidate()
    }

    interface PageEventsListener {
        fun onAddPoint(point: Point)
        fun onCreateNewPath()
        fun onSave()
        fun onErase(point: Point)
        fun onEraseStart()
    }

    data class Point(
        val relativeX: Float,
        val relativeY: Float,
        var isErased: Boolean = false,
        var erasedStep: Int = -1
    )
}
