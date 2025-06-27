package com.example.githubwidget

import android.graphics.*

object GridRenderer {

    private val levelColors = listOf(
        Color.parseColor("#1FF8F9FF"),
        Color.parseColor("#196127"),
        Color.parseColor("#239a3b"),
        Color.parseColor("#7bc96f"),
        Color.parseColor("#8CF67A")
    )

    fun renderPage(
        cells: List<DayCell>,
        page: Int,
        rows: Int,
        colsPerPage: Int,
        cellSize: Int,
        cellPad: Int
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = cellSize * 0.25f
        val totalCols = (cells.size + rows - 1) / rows
        val startCol = page * colsPerPage
        val endCol = minOf(startCol + colsPerPage, totalCols)
        val actualCols = endCol - startCol
        val width = actualCols * (cellSize + cellPad) - cellPad
        val height = rows * (cellSize + cellPad) - cellPad
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        for (col in 0 until actualCols) {
            val globalCol = startCol + col
            for (row in 0 until rows) {
                val idx = row + globalCol * rows
                if (idx >= cells.size) continue
                val cell = cells[idx]
                val x = col * (cellSize + cellPad)
                val y = row * (cellSize + cellPad)
                paint.color = levelColors.getOrElse(cell.level) { levelColors[0] }
                canvas.drawRoundRect(
                    RectF(x.toFloat(), y.toFloat(), (x + cellSize).toFloat(), (y + cellSize).toFloat()),
                    radius,
                    radius,
                    paint
                )
            }
        }

        return bmp
    }
}