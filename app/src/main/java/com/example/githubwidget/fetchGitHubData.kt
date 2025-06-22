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
        // 1. Загружаем профиль пользователя
        val profileStr = URL("https://api.github.com/users/$id").readText()
        val json = JSONObject(profileStr)
        val login = json.getString("login")
        val name = json.optString("name", "")
        val avatarUrl = json.optString("avatar_url", "")
        val followers = json.optInt("followers", 0)

        // 2. Загружаем и сохраняем аватар
        try {
            URL(avatarUrl).openStream().use { input ->
                BitmapFactory.decodeStream(input)?.let { bmp ->
                    File(context.cacheDir, "avatar.png").outputStream().use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GitHubAPI", "Ошибка при загрузке аватара: ${e.message}")
        }

        // 3. Загружаем сетку активности
        val gridStr = URL("https://github-contributions-api.deno.dev/$id.json").readText()
        val weeks = JSONObject(gridStr).getJSONArray("contributions")
        val rawCells = mutableListOf<DayCell>()

        for (i in 0 until weeks.length()) {
            val week = weeks.getJSONArray(i)
            for (j in 0 until week.length()) {
                val obj = week.getJSONObject(j)
                rawCells += DayCell(
                    date = obj.getString("date"),
                    count = obj.optInt("contributionCount", 0),
                    level = 0 // временно, позже пересчитаем
                )
            }
        }

        // 4. Вычисляем уровень активности вручную на основе count
        val max = rawCells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        val cells = rawCells.map {
            val level = when {
                it.count == 0 -> 0
                it.count < max * 0.25 -> 1
                it.count < max * 0.5 -> 2
                it.count < max * 0.75 -> 3
                else -> 4
            }
            it.copy(level = level)
        }

        // 5. Рендерим сетку и сохраняем её
        val screenWidth = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
        val gridBmp = GridRenderer.renderAutoSize(cells, screenWidth, 12, 3)
        File(context.cacheDir, "grid.png").outputStream().use {
            gridBmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        // 6. Возвращаем профиль
        GitHubProfile(login, name, avatarUrl, followers)
    }