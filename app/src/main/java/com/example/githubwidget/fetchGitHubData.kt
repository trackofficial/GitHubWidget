package com.example.githubwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

suspend fun fetchGitHubData(id: String, context: Context): GitHubProfile =
    withContext(Dispatchers.IO) {
        // 1. Загружаем профиль
        val profileJson = JSONObject(URL("https://api.github.com/users/$id").readText())
        val login  = profileJson.getString("login")
        val name = profileJson.optString("name", "")
        val avatarUrl = profileJson.optString("avatar_url", "")
        val followers = profileJson.optInt("followers", 0)

        // 2. Скачиваем и сохраняем аватар
        runCatching {
            if (avatarUrl.isNotBlank()) {
                Log.d("GitHubWidget", "Скачиваю аватар: $avatarUrl")
                URL(avatarUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        File(context.filesDir, "avatar.png")
                            .outputStream().use { out ->
                                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                    }
                }
            }
        }.onFailure {
            Log.e("GitHubWidget", "Ошибка при загрузке аватара", it)
        }

        // 3. Загружаем активность и собираем DayCell
        val gridJson = JSONObject(URL("https://github-contributions-api.deno.dev/$id.json").readText())
        val weeks    = gridJson.getJSONArray("contributions")
        val raw = mutableListOf<DayCell>()
        for (i in 0 until weeks.length()) {
            val week = weeks.getJSONArray(i)
            for (j in 0 until week.length()) {
                val cell = week.getJSONObject(j)
                raw += DayCell(
                    date  = cell.getString("date"),
                    count = cell.getInt("contributionCount"),
                    level = 0
                )
            }
        }

        // 4. Считаем общее число contributions
        val totalContributions = raw.sumOf { it.count }
        // Сохраняем в prefs, чтобы виджет мог прочитать
        context.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            .edit()
            .putInt("total_contributions", totalContributions)
            .apply()

        // 5. Вычисляем уровни и подготавливаем padding
        val max = raw.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        val cells = raw.map {
            val lvl = when {
                it.count == 0 -> 0
                it.count < max * 0.25 -> 1
                it.count < max * 0.5 -> 2
                it.count < max * 0.75 -> 3
                else -> 4
            }
            it.copy(level = lvl)
        }
        val rows= 7
        val maxCols = 53
        val actualCols= (cells.size + rows - 1) / rows
        val padCols = maxCols - actualCols
        val padded = List(padCols * rows) { DayCell("", 0, 0) } + cells

        // 6. Рендерим страницы сетки
        val cellSize = 49
        val cellPad = 6
        val columnSet= listOf(18, 18, 18)
        columnSet.forEachIndexed { page, colsPerPage ->
            val bmp = GridRenderer.renderPage(padded, page, rows, colsPerPage, cellSize, cellPad)
            File(context.filesDir, "grid_page_$page.png")
                .outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }

        // 7. Сохраняем остальные параметры
        context.getSharedPreferences("gh_widget", Context.MODE_PRIVATE).edit().apply {
            putString("user_default", id)
            putInt("page_count", columnSet.size)
            putInt("cell_size_px", cellSize)
            putInt("cell_pad_px", cellPad)
            apply()
        }

        // 8. Возвращаем профиль вместе с totalContributions
        GitHubProfile(login, name, avatarUrl, followers, totalContributions)
    }