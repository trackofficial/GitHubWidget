package com.example.githubwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object GridRenderer {
    // Цвета уровней активности GitHub (в стиле оригинального дизайна)
    private val levels = mapOf(
        0 to Color.parseColor("#ebedf0"),
        1 to Color.parseColor("#c6e48b"),
        2 to Color.parseColor("#7bc96f"),
        3 to Color.parseColor("#239a3b"),
        4 to Color.parseColor("#196127")
    )

    /**
     * Отрисовывает сетку активности GitHub, адаптированную под заданную ширину.
     * @param cells список ячеек за каждый день
     * @param maxWidthPx максимальная ширина bitmap (например, ширина экрана)
     * @param minCellSize минимальный допустимый размер ячейки (px)
     * @param cellPad отступ между ячейками (px)
     */
    fun renderAutoSize(
        cells: List<DayCell>,
        maxWidthPx: Int = 520,
        minCellSize: Int = 10,
        cellPad: Int = 3
    ): Bitmap {
        val rows = 7
        val cols = (cells.size + rows - 1) / rows

        // Автоматический расчет размера ячеек
        val cellSize = ((maxWidthPx + cellPad).toFloat() / cols - cellPad).toInt()
            .coerceAtLeast(minCellSize)

        val width = cols * (cellSize + cellPad) - cellPad
        val height = rows * (cellSize + cellPad) - cellPad

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((i, cell) in cells.withIndex()) {
            val col = i / rows
            val row = i % rows
            paint.color = levels[cell.level] ?: Color.LTGRAY

            val x = col * (cellSize + cellPad)
            val y = row * (cellSize + cellPad)
            canvas.drawRect(
                x.toFloat(),
                y.toFloat(),
                (x + cellSize).toFloat(),
                (y + cellSize).toFloat(),
                paint
            )
        }

        return bitmap
    }
}