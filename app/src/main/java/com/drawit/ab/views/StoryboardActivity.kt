package com.drawit.ab.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.drawit.ab.PageAdapter
import com.drawit.ab.databinding.ActivityStoryboardBinding
import com.drawit.ab.viewmodel.DrawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StoryboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStoryboardBinding
    private lateinit var viewModel: DrawViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DrawViewModel::class.java]

        binding.loadProgress.isVisible = true
        lifecycleScope.launch(Dispatchers.IO) {
            // todo check if can use fragment and shared viewModel
            viewModel.loadPages(this@StoryboardActivity)
            withContext(Dispatchers.Main) {
                binding.loadProgress.isVisible = false
            }
        }
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        viewModel.pages.observe(this) { pages ->
            recyclerView.adapter = PageAdapter(pages) { position ->
                viewModel.setCurrentIndex(this, position)
                // Go back to the main activity
                val resultIntent = Intent()
                resultIntent.putExtra(MainActivity.STORYBOARD_RESULT_KEY, position)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}
