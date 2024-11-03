package com.example.myapplication

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.utils.SaveUtils
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
        SaveUtils.saveNumberOfPages(context, count)
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
        SaveUtils.saveNumberOfPages(context, count)
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
        SaveUtils.saveNumberOfPages(context, count)
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
        SaveUtils.deletePage(context, index)
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
        SaveUtils.saveNumberOfPages(context, count)
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
        val allPages = SaveUtils.getAllPages(context)
        if (allPages.isNotEmpty()) {
            _pages.postValue(allPages)
        }
    }

    fun erasePoint(point: DrawView.Point, step: Int) {
        val currentPages = _pages.value?.toMutableList() ?: return
        val currentPageIndex = currentPageIndex.value ?: return
        val minX = point.x - ERASER_SIZE
        val maxX = point.x + ERASER_SIZE
        val minY = point.y - ERASER_SIZE
        val maxY = point.y + ERASER_SIZE

        if (currentPageIndex in currentPages.indices) {
            val currentPage = currentPages[currentPageIndex]
            for (path in currentPage.getPaths()) {
                if (!path.isVisible) {
                    continue
                }
                for (pathPoint in path.getPathPoints()) {
                    if (pathPoint.x in minX..maxX && pathPoint.y in minY..maxY) {
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
        SaveUtils.savePageToFile(context, page)
    }

    fun setColor(color: Int) {
        _currentColor.value = color
    }

    fun setInitialPageIndex(context: Context) {
        val currIndex = SaveUtils.getCurrentPageIndex(context)
        _currentPageIndex.value = currIndex
    }

    fun setMode(mode: DrawMode) {
        _drawMode.value = mode
    }

    fun getMode(): DrawMode {
        return _drawMode.value ?: DrawMode.PENCIL
    }

    fun setCurrentIndex(index: Int) {
        _currentPageIndex.value = index
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
        SaveUtils.setCurrentSteps(context, currStep)
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
        SaveUtils.setCurrentSteps(context, step)
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
        var totalSteps = SaveUtils.getTotalSteps(context)
        totalSteps++
        SaveUtils.setTotalSteps(context, totalSteps)

        val currStep = totalSteps
        SaveUtils.setCurrentSteps(context, currStep)

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

    fun saveCanvasSize(context: Context, w: Int, h: Int) {
        SaveUtils.setCanvasWidth(context, w)
        SaveUtils.setCanvasHeight(context, h)
    }

    fun generateRandomShapeCoordinates(canvasWidth: Int, canvasHeight: Int): List<DrawView.Point> {
        val shapeType = if (Random.nextBoolean()) "circle" else "square"
        val size = Random.nextFloat() * (minOf(canvasWidth, canvasHeight) / 4)
        val centerX = Random.nextFloat() * (canvasWidth - size) + size / 2
        val centerY = Random.nextFloat() * (canvasHeight - size) + size / 2
        val points = mutableListOf<DrawView.Point>()

        return when (shapeType) {
            "circle" -> {
                // Generate points for a circle

                val numPoints = 36 // Number of points to approximate the circle
                for (i in 0 until numPoints) {
                    val angle = (2 * Math.PI / numPoints * i).toFloat()
                    val x = centerX + size * cos(angle)
                    val y = centerY + size * sin(angle)
                    points.add(DrawView.Point(x, y))
                }
                points
            }
            "square" -> {
                // Generate points for a square with additional points along the edges
                val halfSize = size / 2

                val numPointsPerEdge = 5 // Number of points per edge, excluding corners
                // Top edge
                for (i in 0..numPointsPerEdge) {
                    val x = centerX - halfSize + (size * i / numPointsPerEdge)
                    val y = centerY - halfSize
                    points.add(DrawView.Point(x, y))
                }
                // Right edge
                for (i in 0..numPointsPerEdge) {
                    val x = centerX + halfSize
                    val y = centerY - halfSize + (size * i / numPointsPerEdge)
                    points.add(DrawView.Point(x, y))
                }
                // Bottom edge
                for (i in 0..numPointsPerEdge) {
                    val x = centerX + halfSize - (size * i / numPointsPerEdge)
                    val y = centerY + halfSize
                    points.add(DrawView.Point(x, y))
                }
                // Left edge
                for (i in 0..numPointsPerEdge) {
                    val x = centerX - halfSize;
                    val y = centerY + halfSize - (size * i / numPointsPerEdge)
                    points.add(DrawView.Point(x, y))
                }
                points
            }
            else -> emptyList()
        }
    }

    suspend fun saveDocument(context: Context) {
        val allPages = _pages.value?.toList() ?: return
        val doc = Document(allPages)
        SaveUtils.saveDocument(context, doc)
    }

    var ERASER_SIZE = 40

    enum class DrawMode {
        PENCIL, ERASER, PLAYBACK
    }
    // todo check if can support page add and delete in undo/redo easily
}
