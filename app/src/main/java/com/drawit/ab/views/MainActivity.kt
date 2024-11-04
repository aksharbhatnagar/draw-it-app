package com.drawit.ab.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
import com.drawit.ab.R
import com.drawit.ab.databinding.ActivityMainBinding
import com.drawit.ab.databinding.ColorPalettePopupBinding
import com.drawit.ab.databinding.PlaybackSpeedPopupBinding
import com.drawit.ab.models.Page
import com.drawit.ab.models.Path
import com.drawit.ab.utils.ColorUtils
import com.drawit.ab.repo.SaveRepository
import com.drawit.ab.utils.ClickUtils.setDebouncedClickListener
import com.drawit.ab.utils.GifUtils
import com.drawit.ab.viewmodel.DrawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        drawViewModel.setInitialSpeed(this)
        setObservers()
        binding.loadProgress.isVisible = true
        lifecycleScope.launch(Dispatchers.IO) {
            drawViewModel.loadPages(this@MainActivity)
            withContext(Dispatchers.Main) {
                binding.loadProgress.isVisible = false
            }
        }
        setClickListeners()
        storyboardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val resultValue = data?.getIntExtra(STORYBOARD_RESULT_KEY, -1)
                if (resultValue != null && resultValue != -1) {
                    drawViewModel.setCurrentIndex(this, resultValue)
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
            SaveRepository.setCurrentPageIndex(this, index)
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
        binding.addPageButton.setDebouncedClickListener {
            drawViewModel.addPage(this)
        }
        binding.deletePageButton.setDebouncedClickListener {
            drawViewModel.deletePage(this)?.let {
                saveDocument()
            }
        }
        binding.pageListButton.setDebouncedClickListener {
            // Open Storyboard activity
            val storyboardIntent = Intent(this, StoryboardActivity::class.java)
            storyboardLauncher.launch(storyboardIntent)
        }
        binding.pencilButton.setDebouncedClickListener {
            drawViewModel.setMode(DrawViewModel.DrawMode.PENCIL)
        }
        binding.eraserButton.setDebouncedClickListener {
            drawViewModel.setMode(DrawViewModel.DrawMode.ERASER)
        }
        binding.undoButton.setDebouncedClickListener {
            drawViewModel.undo(this)
            saveDocument()
        }
        binding.redoButton.setDebouncedClickListener {
            drawViewModel.redo(this)
            saveDocument()
        }
        binding.playButton.setDebouncedClickListener {
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
        binding.colorButton.setDebouncedClickListener {
            showColorPalette(it)
        }
        binding.duplicatePageButton.setDebouncedClickListener {
            drawViewModel.duplicatePage(this)
        }
        binding.buttonGenerate.setDebouncedClickListener {
            val input = binding.inputNRandom.text.toString()
            val currentIndex = SaveRepository.getCurrentPageIndex(this)
            val currentStep = SaveRepository.getCurrentSteps(this)
            val n = input.toIntOrNull() ?: 0
            if (n > 0) {
                val randomPages = List(n) {
                    Page(currentIndex + it + 1).apply {
                        val shapePoints = drawViewModel.generateRandomShapeCoordinates()
                        addPath(Path(shapePoints.toMutableList(), ColorUtils.getRandomColor(), currentStep + 1))
                    }
                }
                drawViewModel.addPagesAtIndex(this, randomPages)
                saveDocument()
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
        binding.deleteAllButton.setDebouncedClickListener {
            drawViewModel.deleteAll()
            saveDocument()
        }
        binding.setSpeed.setDebouncedClickListener {
            showPlaybackSpeedPopup(it)
        }
        binding.shareButton.setDebouncedClickListener(1000) {
            binding.loadProgress.isVisible = true
            lifecycleScope.launch(Dispatchers.IO) {
                val bitmaps =
                    GifUtils.createBitmapsFromPages(this@MainActivity, drawViewModel.pages.value ?: listOf(), 500, 500)
                val delay = drawViewModel.getPlaybackSpeed()
                GifUtils.saveGif(this@MainActivity, bitmaps, delay.toInt())
                GifUtils.shareGif(this@MainActivity)
                withContext(Dispatchers.Main) {
                    binding.loadProgress.isVisible = false
                }
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
        binding.deleteAllButton.isVisible = !isPlayback
        binding.pencilButton.isVisible = !isPlayback
        binding.eraserButton.isVisible = !isPlayback
        binding.colorButton.isVisible = !isPlayback
        binding.setSpeed.isVisible = !isPlayback
        binding.buttonGenerate.isVisible = !isPlayback
        binding.inputNRandom.isVisible = !isPlayback
        binding.shareButton.isVisible = !isPlayback
    }

    private fun saveDocument() {
        lifecycleScope.launch(Dispatchers.IO) {
            drawViewModel.saveDocument(this@MainActivity)
        }
    }

    override fun onSave() {
        saveDocument()
    }

    override fun onAddPoint(point: DrawView.Point) {
        drawViewModel.addPointToCurrentPath(point)
    }

    override fun onCreateNewPath() {
        drawViewModel.onNewStep(this)
        val stepNum = SaveRepository.getTotalSteps(this)
        drawViewModel.createNewPathInCurrentFrame(stepNum)
    }

    override fun onErase(point: DrawView.Point) {
        // todo define add and erase step types to narrow down inverse operation
        val step = SaveRepository.getCurrentSteps(this)
        drawViewModel.erasePoint(point, step)
    }

    override fun onEraseStart() {
        drawViewModel.onNewStep(this)
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
        popupBinding.color5.setOnClickListener {
            val color5 = ContextCompat.getColor(this, R.color.palette_color5)
            drawViewModel.setColor(color5)
            popupWindow.dismiss()
        }
        popupWindow.isOutsideTouchable = true
        val xOffset = -5 * anchor.width
        val yOffset = -anchor.height - (resources.displayMetrics.heightPixels * 0.1).toInt()

        popupWindow.showAsDropDown(anchor, xOffset, yOffset)

        binding.colorButton.setImageResource(R.drawable.ic_color_selected)
        popupWindow.setOnDismissListener {
            binding.colorButton.setImageResource(R.drawable.ic_color)
        }
    }

    private fun showPlaybackSpeedPopup(anchor: View) {
        val popupBinding = PlaybackSpeedPopupBinding.inflate(layoutInflater)
        val popupWindow = PopupWindow(popupBinding.root, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        popupBinding.speedSlow.setOnClickListener {
            drawViewModel.setPlaybackSpeed(this, SaveRepository.SPEED_SLOW)
            popupWindow.dismiss()
        }
        popupBinding.speedNormal.setOnClickListener {
            drawViewModel.setPlaybackSpeed(this, SaveRepository.SPEED_NORMAL)
            popupWindow.dismiss()
        }
        popupBinding.speedFast.setOnClickListener {
            drawViewModel.setPlaybackSpeed(this, SaveRepository.SPEED_FAST)
            popupWindow.dismiss()
        }
        when (drawViewModel.getPlaybackSpeed()) {
            SaveRepository.SPEED_FAST -> {
                popupBinding.tickMarkFast.isVisible = true
            }
            SaveRepository.SPEED_SLOW -> {
                popupBinding.tickMarkSlow.isVisible = true
            }
            else -> {
                popupBinding.tickMarkNormal.isVisible = true
            }
        }
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchor)
    }

    companion object {
        const val STORYBOARD_RESULT_KEY = "selected_index"
    }
}

/*
 * Copyright (c) 2024 Akshar Bhatnagar
 *
 * All rights reserved. This code is proprietary and may not be used, modified,
 * or distributed without explicit permission from the copyright owner.
 */
