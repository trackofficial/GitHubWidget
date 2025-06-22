package com.example.githubwidget

import android.graphics.*

object WidgetAssets {
    fun createAvatarPlaceholder(size: Int): Bitmap {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawCircle(size/2f, size/2f, size/2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
            })
        }
    }

    fun createFullGrid(rows: Int, cols: Int): Bitmap {
        val cell = 6
        val pad = 2
        val w = cols * (cell + pad) - pad
        val h = rows * (cell + pad) - pad
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            val c = Canvas(this)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
            for (r in 0 until rows) {
                for (col in 0 until cols) {
                    val x = col * (cell + pad)
                    val y = r * (cell + pad)
                    c.drawRect(x.toFloat(), y.toFloat(), (x + cell).toFloat(), (y + cell).toFloat(), p)
                }
            }
        }
    }

    fun sliceGrid(full: Bitmap, page: Int): Bitmap {
        val half = full.width / 2
        val x = if (page == 0) 0 else half
        return Bitmap.createBitmap(full, x, 0, half, full.height)
    }
}
