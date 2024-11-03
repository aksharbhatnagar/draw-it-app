package com.example.myapplication.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.myapplication.DrawViewModel
import com.example.myapplication.Page
import com.example.myapplication.utils.ColorUtils
import kotlin.math.pow
import kotlin.math.sqrt

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
    private var eraserSize = 40f
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
        private const val MIN_DISTANCE = 5.0f // min distance to add next point in path
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
                canvas.drawCircle(it.x, it.y, eraserSize, eraserBorderPaint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pageEventsListener?.onCanvasSize(w, h)
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
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.e("akshar", "actiono down")
                val point = Point(x, y)
                lastPoint = point
                pageEventsListener?.onCreateNewPath() // todo check if we can draw dots
                // pages[currentPageIndex].addPath(Path(mutableListOf()))
                // invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (lastPoint != null && sqrt((x - lastPoint!!.x).pow(2) + (y - lastPoint!!.y).pow(2)) >= MIN_DISTANCE) {
                    val newPoint = Point(x, y)
                    // pages[currentPageIndex].addPointToPath(lastPoint!!)
                    pageEventsListener?.onAddPoint(newPoint)
                    lastPoint = newPoint

                    // invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                Log.e("akshar", "action up")
                Log.e("akshar", "added path to current page")

                Log.e("akshar", "action up touchPencil calling saveDocument")
                if (currentPageIndex in pages.indices) {
                    pageEventsListener?.onSavePage(pages[currentPageIndex])
                }
            }
        }
        return true
    }

    private fun onTouchEraser(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val point = Point(x, y)
                pageEventsListener?.onErase(point)
                pageEventsListener?.onEraseStart()
                lastPoint = point
            }
            MotionEvent.ACTION_MOVE -> {
                val point = Point(x, y)
                pageEventsListener?.onErase(point)
                lastPoint = point
            }
            MotionEvent.ACTION_UP -> {
                Log.e("akshar", "action up touch eraser calling saveDocument")
                pageEventsListener?.onSavePage(pages[currentPageIndex])
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
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            }
        }
    }

    private fun drawPathOnCanvas(canvas: Canvas, points: List<Point>, color: Int) {
        paint.color = color
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            if (!end.isErased) {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint)
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
        fun onSavePage(page: Page)
        fun onErase(point: Point)
        fun onEraseStart()
        fun onCanvasSize(w: Int, h: Int)
    }

    data class Point(
        val x: Float,
        val y: Float,
        var isErased: Boolean = false,
        var erasedStep: Int = -1
    )
}
