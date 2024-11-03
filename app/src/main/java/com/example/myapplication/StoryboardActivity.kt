package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.ActivityStoryboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StoryboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoryboardBinding
    private lateinit var viewModel: DrawViewModel
    private var canvasWidth = 1
    private var canvasHeight = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        canvasWidth = intent.getIntExtra("canvas_width", 1)
        canvasHeight = intent.getIntExtra("canvas_height", 1)

        viewModel = ViewModelProvider(this)[DrawViewModel::class.java]
        lifecycleScope.launch(Dispatchers.IO) {
            // todo check if can use fragment and shared viewModel
            viewModel.loadPages(this@StoryboardActivity)
        }
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        viewModel.pages.observe(this) { pages ->
            recyclerView.adapter = PageAdapter(pages, canvasWidth, canvasHeight) { position ->
                viewModel.setCurrentIndex(position)
                // Go back to the main activity
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.STORYBOARD_RESULT_KEY, position)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}
