package com.example.myapplication.utils

import android.content.Context
import android.util.Log
import com.example.myapplication.Document
import com.example.myapplication.Page
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SaveUtils {

    private const val PREFS_NAME = "my_prefs"
    private const val KEY_NUMBER_OF_PAGES = "number_of_pages"
    private const val KEY_CURRENT_PAGE_INDEX = "current_page_index" // 0-based
    private const val KEY_TOTAL_STEPS = "total_steps"
    private const val KEY_CURRENT_STEPS = "current_steps"
    private const val KEY_CANVAS_WIDTH = "canvas_width"
    private const val KEY_CANVAS_HEIGHT = "canvas_height"

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
            Log.e("akshar", "saved page number ${page.index + 1}")
        }
    }

    suspend fun saveDocument(context: Context, document: Document) {
        Log.e("akshar", "calling saveDocument")
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

    suspend fun getPathFromJson(context: Context, index: Long): Page? {
        return withContext(Dispatchers.IO) {
            val fileName = "$index.json"
            try {
                val inputStream = context.openFileInput(fileName)
                val json = inputStream.bufferedReader().use { it.readText() }
                Gson().fromJson(json, Page::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getAllPages(context: Context): List<Page> {
        val numberOfPages = getNumberOfPages(context)
        Log.e("akshar", "saved pages count = $numberOfPages")
        val pages = mutableListOf<Page>()
//        for ( i in 0 until numberOfPages) {
//            val fileName = "$i.json"
//            try {
//                val inputStream = context.openFileInput(fileName)
//                val json = inputStream.bufferedReader().use { it.readText() }
//                pages.add(Gson().fromJson(json, Page::class.java))
//            } catch (e: Exception) {
//                e.printStackTrace()
//                return pages
//            }
//        }
        val fileName = "document.json"
        try {
            val inputStream = context.openFileInput(fileName)
            val json = inputStream.bufferedReader().use { it.readText() }
            pages.addAll(Gson().fromJson(json, Document::class.java).getPages())
        } catch (e: Exception) {
            e.printStackTrace()
            return pages
        }
        Log.e("akshar", "got saved pages count = ${pages.size}")

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
        Log.e("akshar", "saving number of pages $numberOfPages")
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(KEY_NUMBER_OF_PAGES, numberOfPages)
            apply()
        }
    }

    fun getNumberOfPages(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_NUMBER_OF_PAGES, 0)
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

    fun getCanvasHeight(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_CANVAS_HEIGHT, 1)
    }

    fun getCanvasWidth(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_CANVAS_WIDTH, 1)
    }

    fun setCanvasWidth(context: Context, w: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_CANVAS_WIDTH, w).apply()
    }

    fun setCanvasHeight(context: Context, h: Int) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_CANVAS_HEIGHT, h).apply()
    }
}
