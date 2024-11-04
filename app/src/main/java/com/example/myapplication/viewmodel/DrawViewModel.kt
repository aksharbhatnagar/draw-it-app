package com.example.myapplication.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.models.Document
import com.example.myapplication.models.Page
import com.example.myapplication.models.Path
import com.example.myapplication.repo.SaveRepository
import com.example.myapplication.views.DrawView
import kotlinx.coroutines.delay
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class DrawViewModel: ViewModel() {
    private val _drawMode = MutableLiveData(DrawMode.PENCIL)
    val drawMode: LiveData<DrawMode> = _drawMode

    private val _currentPageIndex = MutableLiveData(0)
    val currentPageIndex: LiveData<Int> = _currentPageIndex

    private val _currentColor = MutableLiveData(0xFF000000.toInt())
    val currentColor : LiveData<Int> = _currentColor

    private val _pages = MutableLiveData(listOf(Page(0)))
    val pages: LiveData<List<Page>> get() = _pages

    private val _undoStack = MutableLiveData(ArrayDeque<Int>())
    private val _redoStack = MutableLiveData(ArrayDeque<Int>())
    val undoStack: LiveData<ArrayDeque<Int>> get() = _undoStack
    val redoStack: LiveData<ArrayDeque<Int>> get() = _redoStack

    private val _playbackPage = MutableLiveData<Page>()
    val playbackPage: LiveData<Page> = _playbackPage

    private val currentPage: Page?
        get() {
            val allPages = _pages.value
            if (allPages == null) {
                return null
            } else {
                val currIndex = _currentPageIndex.value
                if (currIndex == null) {
                    return null
                } else if (currIndex in allPages.indices) {
                    return allPages[currIndex]
                }
                return null
            }
        }

    fun addPagesAtIndex(context: Context, pages: List<Page>) {
        val currentPages = _pages.value?.toMutableList() ?: mutableListOf()
        val currentPageIndex = currentPageIndex.value ?: return
        currentPages.addAll(currentPageIndex + 1, pages)

        // Increment the number of pages
        val count = currentPages.size
        Log.e("akshar", "addPagesAtIndex added ${pages.size}")
        SaveRepository.saveNumberOfPages(context, count)
        val currIndex = _currentPageIndex.value ?: 0
        _currentPageIndex.value = min(currIndex + count, currentPages.size - 1)

        _pages.value = currentPages

        clearStacks()
    }

    fun addPage(context: Context) {
        val currentPages = _pages.value?.toMutableList() ?: mutableListOf()
        val currentPageIndex = currentPageIndex.value ?: return
        currentPages.add(Page(currentPageIndex + 1))

        // Increment the number of pages
        val count = currentPages.size
        Log.e("akshar", "addPage saving page count = $count")
        SaveRepository.saveNumberOfPages(context, count)
        val currIndex = _currentPageIndex.value ?: 0
        _currentPageIndex.value = min(currIndex + 1, currentPages.size - 1)

        _pages.value = currentPages

        clearStacks()
    }

    fun duplicatePage(context: Context): Page? {
        val currentPages = _pages.value?.toMutableList() ?: mutableListOf()

        val currPage = currentPage ?: return null
        val currPageIndex = currPage.index

        val newPage = Page(currPageIndex + 1).apply { setPaths(currPage.getPaths()) }
        // duplicate paths to new page
        currentPages.add(newPage)

        // Increment the number of pages
        val count = currentPages.size
        Log.e("akshar", "addPage saving page count = ${count}")
        SaveRepository.saveNumberOfPages(context, count)
        val currIndex = _currentPageIndex.value ?: 0
        _currentPageIndex.value = currIndex + 1

        _pages.value = currentPages

        clearStacks()
        return newPage
    }

    private fun clearStacks() {
        val newUndoStack = _undoStack.value
        newUndoStack?.clear()
        _undoStack.value = newUndoStack

        val newRedoStack = _redoStack.value
        newRedoStack?.clear()
        _redoStack.value = newRedoStack
    }

    suspend fun deletePageFromMemory(context: Context, index: Long) {
        SaveRepository.deletePage(context, index)
    }

    fun deletePage(context: Context): Page? {
        val currentPages = _pages.value?.toMutableList() ?: return null
        if (currentPages.size == 1) {
            val removedPage = currentPages.removeFirstOrNull()
            _pages.value = listOf(Page(0))
            return removedPage
        }
        val removedPage = currentPages.removeLast()
        val currIndex = _currentPageIndex.value ?: 0
        _currentPageIndex.value = max(currIndex - 1, 0)

        // Decrement the number of pages
        val count = currentPages.size
        Log.e("akshar", "deletePage saving page count = ${count}")
        SaveRepository.saveNumberOfPages(context, count)
        _pages.value = currentPages

        clearStacks()
        return removedPage
    }

    fun addPointToCurrentPath(point: DrawView.Point) {
        val currentPages = _pages.value?.toMutableList() ?: return
        val currentPageIndex = currentPageIndex.value ?: return
        if (currentPageIndex in currentPages.indices) {
            currentPages[currentPageIndex].addPointToPath(point)
        }
        _pages.value = currentPages
    }

    fun createNewPathInCurrentFrame(step: Int) {
        val currentPages = _pages.value?.toMutableList() ?: return
        val currentPageIndex = currentPageIndex.value ?: return
        if (currentPageIndex in currentPages.indices) {
            currentPages[currentPageIndex].addPath(Path(mutableListOf(), currentColor.value!!, step))
        }
        _pages.value = currentPages
    }

    suspend fun loadPages(context: Context) {
        val allPages = SaveRepository.getAllPages(context)
        if (allPages.isNotEmpty()) {
            _pages.postValue(allPages)
            val currIndex = _currentPageIndex.value ?: 0
            _currentPageIndex.postValue(currIndex.coerceIn(0, allPages.size - 1))
        }
    }

    fun erasePoint(point: DrawView.Point, step: Int) {
        val currentPages = _pages.value?.toMutableList() ?: return
        val currentPageIndex = currentPageIndex.value ?: return
        val minX = point.relativeX - ERASER_SIZE
        val maxX = point.relativeX + ERASER_SIZE
        val minY = point.relativeY - ERASER_SIZE
        val maxY = point.relativeY + ERASER_SIZE

        if (currentPageIndex in currentPages.indices) {
            val currentPage = currentPages[currentPageIndex]
            for (path in currentPage.getPaths()) {
                if (!path.isVisible) {
                    continue
                }
                for (pathPoint in path.getPathPoints()) {
                    if (pathPoint.relativeX in minX..maxX && pathPoint.relativeY in minY..maxY) {
                        pathPoint.isErased = true
                        pathPoint.erasedStep = step
                    }
                }
            }
            currentPages[currentPageIndex] = currentPage
            _pages.value = currentPages
        }
    }

    suspend fun savePage(context: Context, page: Page) {
        SaveRepository.savePageToFile(context, page)
    }

    fun setColor(color: Int) {
        _currentColor.value = color
    }

    fun setInitialPageIndex(context: Context) {
        val currIndex = SaveRepository.getCurrentPageIndex(context)
        _currentPageIndex.value = currIndex
    }

    fun setMode(mode: DrawMode) {
        _drawMode.value = mode
    }

    fun getMode(): DrawMode {
        return _drawMode.value ?: DrawMode.PENCIL
    }

    fun setCurrentIndex(context: Context, index: Int) {
        _currentPageIndex.value = index
        SaveRepository.setCurrentPageIndex(context, index)
    }

    suspend fun startPlayback() {
        val allPages = _pages.value ?: return
        for (page in allPages) {
            _playbackPage.value = page
            delay(100)
        }
        delay(500)
    }

    fun undo(context: Context) {
        val newUndoStack = _undoStack.value ?: return
        if (newUndoStack.isEmpty()) {
            return
        }
        val step = newUndoStack.pop()
        val currStep = newUndoStack.peekLast() ?: 0
        SaveRepository.setCurrentSteps(context, currStep)
        val currPage = currentPage ?: return
        for (path in currPage.getPaths()) {
            if (path.addedStep == step) { // if point was added at that step
                path.isVisible = false
            } else {
                for (point in path.getPathPoints()) {
                    if (point.erasedStep == step) { // if point was erased at that step
                        point.isErased = false
                    }
                }
            }
        }
        // pop from undo stack and push to redo stack
        _undoStack.value = newUndoStack
        val newRedoStack = _redoStack.value ?: return
        newRedoStack.push(step)
        _redoStack.value = newRedoStack

        updateCurrentPage(currPage)
    }

    fun redo(context: Context) {
        val newRedoStack = _redoStack.value ?: return
        if (newRedoStack.isEmpty()) {
            return
        }
        val step = newRedoStack.pop()
        SaveRepository.setCurrentSteps(context, step)
        val currPage = currentPage ?: return

        for (path in currPage.getPaths()) {
            if (path.addedStep == step) { // if path was added at that step
                path.isVisible = true
            }
            else {
                for (point in path.getPathPoints()) {
                    if (point.erasedStep == step) { // if point was erased at that step
                        point.isErased = true
                    }
                }
            }
        }
        // pop from redo stack and push to undo stack
        _redoStack.value = newRedoStack
        val newUndoStack = _undoStack.value ?: return
        newUndoStack.push(step)
        _undoStack.value = newUndoStack

        updateCurrentPage(currPage)
    }

    fun onNewStep(context: Context) {
        var totalSteps = SaveRepository.getTotalSteps(context)
        totalSteps++
        SaveRepository.setTotalSteps(context, totalSteps)

        val currStep = totalSteps
        SaveRepository.setCurrentSteps(context, currStep)

        // clear redo stack
        val newRedoStack = _redoStack.value
        newRedoStack?.clear()
        _redoStack.value = newRedoStack
        val newUndoStack = _undoStack.value
        newUndoStack?.push(totalSteps)
        _undoStack.value = newUndoStack
    }

    private fun updateCurrentPage(currPage: Page) {
        val allPages = _pages.value?.toMutableList() ?: return
        val currIndex = currentPageIndex.value ?: return
        if (currIndex in allPages.indices) {
            allPages[currIndex] = currPage
            _pages.value = allPages
        }
    }

    fun deleteAll() {
        _pages.value = listOf(Page(0))
        _currentPageIndex.value = 0
        clearStacks()
    }

    fun generateRandomShapeCoordinates(): List<DrawView.Point> {
        val shapeType = if (Random.nextBoolean()) "circle" else "square"
        val size = Random.nextFloat() * 0.5f
        val centerX = Random.nextFloat().coerceIn(0.25f, 0.75f)
        val centerY = Random.nextFloat().coerceIn(0.25f, 0.75f)

        return when (shapeType) {
            "circle" -> {
                val numPoints = 36 // Number of points to approximate the circle
                generateCirclePoints(DrawView.Point(centerX, centerY), size, numPoints)
            }
            "square" -> {
                generateSquareEdgePoints(DrawView.Point(centerX, centerY), size, 5)
            }
            else -> emptyList()
        }
    }

    private fun generateCirclePoints(center: DrawView.Point, radius: Float, numPoints: Int): List<DrawView.Point> {
        return List(numPoints + 1) { i ->
            val angle = 2 * Math.PI / numPoints * i
            DrawView.Point(center.relativeX + radius * cos(angle).toFloat(), center.relativeY + radius * sin(angle).toFloat())
        }
    }

    private fun generateSquareEdgePoints(center: DrawView.Point, size: Float, pointsPerEdge: Int): List<DrawView.Point> {
        val halfSize = size / 2
        val points = mutableListOf<DrawView.Point>()

        // Define corners
        val topLeft = DrawView.Point(center.relativeX - halfSize, center.relativeY - halfSize)
        val topRight = DrawView.Point(center.relativeX + halfSize, center.relativeY - halfSize)
        val bottomRight =DrawView.Point(center.relativeX + halfSize, center.relativeY + halfSize)
        val bottomLeft = DrawView.Point(center.relativeX - halfSize, center.relativeY + halfSize)

        // Generate points for each edge, including corners
        for (edge in 0..3) { // 0: Top, 1: Right, 2: Bottom, 3: Left
            for (i in 0..pointsPerEdge) {
                val t = i.toFloat() / pointsPerEdge // Normalized value from 0 to 1

                val point = when (edge) {
                    0 -> DrawView.Point(
                        topLeft.relativeX + (topRight.relativeX - topLeft.relativeX) * t,
                        topLeft.relativeY // Top edge
                    )
                    1 -> DrawView.Point(
                        topRight.relativeX, // Right edge
                        topRight.relativeY + (bottomRight.relativeY - topRight.relativeY) * t
                    )
                    2 -> DrawView.Point(
                        bottomRight.relativeX - (bottomRight.relativeX - bottomLeft.relativeX) * t,
                        bottomRight.relativeY // Bottom edge
                    )
                    3 -> DrawView.Point(
                        bottomLeft.relativeX, // Left edge
                        bottomLeft.relativeY - (bottomLeft.relativeY - topLeft.relativeY) * t
                    )
                    else -> throw IllegalArgumentException("Invalid edge")
                }
                points.add(point)
            }
        }
        return points.distinct()
    }

    suspend fun saveDocument(context: Context) {
        val allPages = _pages.value?.toList() ?: return
        val doc = Document(allPages)
        SaveRepository.saveDocument(context, doc)
    }

    var ERASER_SIZE = 0.03f

    enum class DrawMode {
        PENCIL, ERASER, PLAYBACK
    }
    // todo check if can support page add and delete in undo/redo easily
}
