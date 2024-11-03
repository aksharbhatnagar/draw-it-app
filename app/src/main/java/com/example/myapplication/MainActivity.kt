package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ColorPalettePopupBinding
import com.example.myapplication.utils.ColorUtils
import com.example.myapplication.utils.SaveUtils
import com.example.myapplication.views.DrawView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), DrawView.PageEventsListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawViewModel: DrawViewModel
    private lateinit var drawView: DrawView
    private var playbackJob: Job? = null

    private lateinit var storyboardLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawViewModel = ViewModelProvider(this)[DrawViewModel::class.java]
        drawView = binding.drawView

        drawView.setOnSavePathsListener(this)
        drawViewModel.setInitialPageIndex(this)
        setObservers()
        lifecycleScope.launch(Dispatchers.IO) {
            drawViewModel.loadPages(this@MainActivity)
        }
        setClickListeners()
        storyboardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val resultValue = data?.getIntExtra(STORYBOARD_RESULT_KEY, -1)
                if (resultValue != null && resultValue != -1) {
                    drawViewModel.setCurrentIndex(resultValue)
                }
            }
        }
    }

    private fun setObservers() {
        drawViewModel.pages.observe(this) { pages ->
            drawView.setPages(pages) // Update the custom view with new paths
        }
        drawViewModel.currentColor.observe(this) { color ->
            drawView.setPaintColor(color)
        }
        drawViewModel.currentPageIndex.observe(this) { index ->
            drawView.setCurrentPageIndex(index)
            SaveUtils.setCurrentPageIndex(this, index)
            // todo show current index/ total data
        }
//        drawViewModel.totalPages.observe(this) { total ->
//            // todo show total count in UI
//        }
        drawViewModel.drawMode.observe(this) {
            drawView.setMode(it)
            val isPencil = it == DrawViewModel.DrawMode.PENCIL
            val isEraser = it == DrawViewModel.DrawMode.ERASER
            val isPlayback = it == DrawViewModel.DrawMode.PLAYBACK
            binding.pencilButton.setImageResource(
                if (isPencil) R.drawable.ic_pencil_enabled else R.drawable.ic_pencil_disabled
            )
            binding.eraserButton.setImageResource(
                if (isEraser) R.drawable.ic_eraser_enabled else R.drawable.ic_eraser_disabled
            )
            if (isPlayback) {
                binding.playButton.setImageResource(R.drawable.ic_pause_enabled)
            } else {
                binding.playButton.setImageResource(R.drawable.ic_play_enabled)
            }
            setButtonsForPlayback(isPlayback)
        }
        drawViewModel.undoStack.observe(this) {
            binding.undoButton.isEnabled = it.isNotEmpty()
        }
        drawViewModel.redoStack.observe(this) {
            binding.redoButton.isEnabled = it.isNotEmpty()
        }
        drawViewModel.playbackPage.observe(this) {
            drawView.setPlaybackPage(it)
        }
    }

    private fun setClickListeners() {
        binding.addPageButton.setOnClickListener {
            drawViewModel.addPage(this)
        }
        binding.deletePageButton.setOnClickListener {
            drawViewModel.deletePage(this)?.let {
                saveDocument()
            }
        }
        binding.pageListButton.setOnClickListener {
            // Open Storyboard activity
            val canvasWidth = SaveUtils.getCanvasWidth(this)
            val canvasHeight = SaveUtils.getCanvasHeight(this)
            val intent = Intent(this, StoryboardActivity::class.java).apply {
                putExtra("canvas_width", canvasWidth)
                putExtra("canvas_height", canvasHeight)
            }
            storyboardLauncher.launch(intent)
        }
        binding.pencilButton.setOnClickListener {
            drawViewModel.setMode(DrawViewModel.DrawMode.PENCIL)
        }
        binding.eraserButton.setOnClickListener {
            drawViewModel.setMode(DrawViewModel.DrawMode.ERASER)
        }
        binding.undoButton.setOnClickListener {
            drawViewModel.undo(this)
            saveDocument()
        }
        binding.redoButton.setOnClickListener {
            drawViewModel.redo(this)
            saveDocument()
        }
        binding.playButton.setOnClickListener {
            val currMode = drawViewModel.getMode()
            if (currMode == DrawViewModel.DrawMode.PLAYBACK) {
                playbackJob?.cancel()
                drawViewModel.setMode(DrawViewModel.DrawMode.PENCIL)
            } else {
                drawViewModel.setMode(DrawViewModel.DrawMode.PLAYBACK)
                playbackJob = lifecycleScope.launch(Dispatchers.Main) {
                    drawViewModel.startPlayback()
                    drawViewModel.setMode(DrawViewModel.DrawMode.PENCIL)
                }
            }
        }
        binding.colorButton.setOnClickListener {
            showColorPalette(it)
        }
        binding.duplicatePageButton.setOnClickListener {
            drawViewModel.duplicatePage(this)?.let {
//                lifecycleScope.launch(Dispatchers.IO) {
//                    onSavePage(it)
//                }
            }
        }
        binding.buttonGenerate.setOnClickListener {
            val input = binding.inputNRandom.text.toString()
            val currentIndex = SaveUtils.getCurrentPageIndex(this)
            val currentStep = SaveUtils.getCurrentSteps(this)
            val canvasWidth = SaveUtils.getCanvasWidth(this)
            val canvasHeight = SaveUtils.getCanvasHeight(this)
            val n = input.toIntOrNull() ?: 0
            if (n > 0) {
                val randomPages = List(n) {
                    Page(currentIndex + it + 1).apply {
                        val shapePoints = drawViewModel.generateRandomShapeCoordinates(canvasWidth, canvasHeight)
                        addPath(Path(shapePoints.toMutableList(), ColorUtils.getRandomColor(), currentStep + 1))
                    }
                }
                drawViewModel.addPagesAtIndex(this, randomPages)
                saveDocument()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setButtonsForPlayback(isPlayback: Boolean) {
        binding.redoButton.isVisible = !isPlayback
        binding.undoButton.isVisible = !isPlayback
        binding.addPageButton.isVisible = !isPlayback
        binding.duplicatePageButton.isVisible = !isPlayback
        binding.deletePageButton.isVisible = !isPlayback
        binding.pageListButton.isVisible = !isPlayback
        binding.pencilButton.isVisible = !isPlayback
        binding.eraserButton.isVisible = !isPlayback
        binding.colorButton.isVisible = !isPlayback
    }

    private fun saveDocument() {
        lifecycleScope.launch(Dispatchers.IO) {
            drawViewModel.saveDocument(this@MainActivity)
        }
    }

    override fun onSavePage(page: Page) {
        saveDocument()
    }

    override fun onAddPoint(point: DrawView.Point) {
        drawViewModel.addPointToCurrentPath(point)
    }

    override fun onCreateNewPath() {
        drawViewModel.onNewStep(this)
        val stepNum = SaveUtils.getTotalSteps(this)
        drawViewModel.createNewPathInCurrentFrame(stepNum)
    }

    override fun onErase(point: DrawView.Point) {
        // todo define add and erase step types to narrow down inverse operation
        val step = SaveUtils.getCurrentSteps(this)
        drawViewModel.erasePoint(point, step)
    }

    override fun onEraseStart() {
        drawViewModel.onNewStep(this)
    }

    override fun onCanvasSize(w: Int, h: Int) {
        drawViewModel.saveCanvasSize(this, w, h)
    }

    private fun showColorPalette(anchor: View) {
        val popupBinding = ColorPalettePopupBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(popupBinding.root, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        popupBinding.color1.setOnClickListener {
            val color1 = ContextCompat.getColor(this, R.color.palette_color1)
            drawViewModel.setColor(color1)
            popupWindow.dismiss()
        }
        popupBinding.color2.setOnClickListener {
            val color2 = ContextCompat.getColor(this, R.color.palette_color2)
            drawViewModel.setColor(color2)
            popupWindow.dismiss()
        }
        popupBinding.color3.setOnClickListener {
            val color3 = ContextCompat.getColor(this, R.color.palette_color3)
            drawViewModel.setColor(color3)
            popupWindow.dismiss()
        }
        popupBinding.color4.setOnClickListener {
            val color4 = ContextCompat.getColor(this, R.color.palette_color4)
            drawViewModel.setColor(color4)
            popupWindow.dismiss()
        }
        popupWindow.isOutsideTouchable = true
        val xOffset = -4 * anchor.width
        popupWindow.showAsDropDown(anchor, xOffset, 0)

        binding.colorButton.setImageResource(R.drawable.ic_color_selected)
        popupWindow.setOnDismissListener {
            binding.colorButton.setImageResource(R.drawable.ic_color)
        }
    }

    companion object {
        const val STORYBOARD_RESULT_KEY = "selected_index"
    }
}
