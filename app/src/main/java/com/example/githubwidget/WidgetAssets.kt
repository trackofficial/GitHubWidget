package com.example.githubwidget

import android.graphics.*
import kotlin.math.min

object WidgetAssets {
    fun createAvatarPlaceholder(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
        }
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return bmp
    }

    fun Bitmap.toCircle(): Bitmap {
        val size = min(width, height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val shader = BitmapShader(this, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }
}