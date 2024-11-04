package com.drawit.ab.repo

import android.content.Context
import com.drawit.ab.models.Document
import com.drawit.ab.models.Page
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SaveRepository {

    private const val PREFS_NAME = "my_prefs"
    private const val KEY_NUMBER_OF_PAGES = "number_of_pages"
    private const val KEY_CURRENT_PAGE_INDEX = "current_page_index" // 0-based
    private const val KEY_TOTAL_STEPS = "total_steps"
    private const val KEY_CURRENT_STEPS = "current_steps"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"
    const val SPEED_FAST = 50L
    const val SPEED_SLOW = 200L
    const val SPEED_NORMAL = 100L

    suspend fun savePageToFile(context: Context, page: Page) {
        withContext(Dispatchers.IO) {
            val gson = Gson()
            val json = gson.toJson(page)

            // Create a file name based on the page index
            val fileName = "${page.index}.json" // Example: "1.json"

            // overwrite JSON string to the file in app-specific internal storage
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(json.toByteArray())
            }
        }
    }

    suspend fun saveDocument(context: Context, document: Document) {
        withContext(Dispatchers.IO) {
            val gson = Gson()
            val json = gson.toJson(document)

            // Create a file name based on the page index
            val fileName = "document.json" // Example: "1.json"

            // overwrite JSON string to the file in app-specific internal storage
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(json.toByteArray())
            }
        }
    }

    suspend fun getAllPages(context: Context): List<Page> {
        val pages = mutableListOf<Page>()
        val fileName = "document.json"
        try {
            val inputStream = context.openFileInput(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            pages.addAll(Gson().fromJson(json, Document::class.java).getPages())
        } catch (e: Exception) {
            e.printStackTrace()
            return pages
        }

        return pages
    }

    suspend fun deletePage(context: Context, index: Long): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val fileName = "$index.json"
                context.deleteFile(fileName)
            }
        } catch (e: Exception) {
            false
        }
    }

    fun saveNumberOfPages(context: Context, numberOfPages: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(KEY_NUMBER_OF_PAGES, numberOfPages)
            apply()
        }
    }

    fun setCurrentPageIndex(context: Context, index: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_CURRENT_PAGE_INDEX, index).apply()
    }

    fun getCurrentPageIndex(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_CURRENT_PAGE_INDEX, 0)
    }

    fun setTotalSteps(context: Context, total: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_TOTAL_STEPS, total).apply()
    }

    fun setCurrentSteps(context: Context, current: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_CURRENT_STEPS, current).apply()
    }

    fun getTotalSteps(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_TOTAL_STEPS, 0)
    }

    fun getCurrentSteps(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_CURRENT_STEPS, 0)
    }

    fun setPlaybackSpeed(context: Context, speed: Long) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.edit().putLong(KEY_PLAYBACK_SPEED, speed).apply()
    }
    fun getPlaybackSpeed(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getLong(KEY_PLAYBACK_SPEED, SPEED_NORMAL)
    }
}
