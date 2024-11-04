package com.drawit.ab.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.drawit.ab.R
import com.drawit.ab.models.Page
import com.drawit.ab.views.DrawView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object GifUtils {

    private fun generateGIF(bitmaps: List<Bitmap>, delayMs: Int): ByteArray? {
        val bos = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder().apply {
            setDelay(delayMs)
        }
        encoder.start(bos)
        for (bitmap in bitmaps) {
            encoder.addFrame(bitmap)
        }
        encoder.finish()
        return bos.toByteArray()
    }

    fun saveGif(context: Context, bitmaps: List<Bitmap>, delayMs: Int) {
        try {
            val filename = "test.gif"
            val file = File(context.filesDir, filename)

            val outStream = FileOutputStream(file)
            outStream.write(generateGIF(bitmaps, delayMs))
            outStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createBitmapsFromPages(context: Context, pages: List<Page>, width: Int, height: Int): List<Bitmap> {
        val paint = Paint().apply {
            color = 0xFF000000.toInt()
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 5f // change later
        }
        val bitmaps = mutableListOf<Bitmap>()
        val backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.draw_background_small)

        for (page in pages) {
            // Create a Bitmap for each page
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawBitmap(backgroundBitmap, 0f, 0f, null) // Set background color (optional)

            for (path in page.getPaths()) {
                if (path.isVisible) {
                    // Set paint color
                    paint.color = path.color

                    // Draw the path on the canvas
                    drawPathOnCanvas(canvas, path.getPathPoints(), paint, width, height)
                }
            }
            // Add the created Bitmap to the list
            bitmaps.add(bitmap)
        }
        return bitmaps
    }

    private fun drawPathOnCanvas(canvas: Canvas, points: List<DrawView.Point>, paint: Paint, width: Int, height: Int) {
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

    fun shareGif(context: Context) {
        val file = File(context.filesDir, "test.gif")

        // Create a content URI for the file
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        // Create the share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/gif"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share GIF"))
    }
}
