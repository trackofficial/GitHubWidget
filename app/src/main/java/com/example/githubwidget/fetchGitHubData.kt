package com.example.githubwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

suspend fun fetchGitHubData(id: String, context: Context, token: String): GitHubProfile =
    withContext(Dispatchers.IO) {

        fun fetchJson(url: String): JSONObject {
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "GitHubWidget")

            // ✅ Авторизация через токен
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            val code = connection.responseCode
            Log.d("GitHubWidget", "Запрос: $url → Код: $code")

            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("GitHubWidget", "Ошибка API: $code\n$errorBody")
                throw RuntimeException("GitHub API error $code for $url")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(response)
        }

        val profileJson = fetchJson("https://api.github.com/users/$id")
        val login = profileJson.getString("login")
        val name = profileJson.optString("name", "")
        val avatarUrl = profileJson.optString("avatar_url", "")
        val followers = profileJson.optInt("followers", 0)

        runCatching {
            if (avatarUrl.isNotBlank()) {
                URL(avatarUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { bmp ->
                        File(context.filesDir, "avatar.png").outputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            }
        }

        val gridJson = fetchJson("https://github-contributions-api.deno.dev/$id.json")
        val weeks = gridJson.getJSONArray("contributions")
        val raw = mutableListOf<DayCell>()
        for (i in 0 until weeks.length()) {
            val week = weeks.getJSONArray(i)
            for (j in 0 until week.length()) {
                val cell = week.getJSONObject(j)
                raw += DayCell(
                    date = cell.getString("date"),
                    count = cell.getInt("contributionCount"),
                    level = 0
                )
            }
        }

        val totalContributions = raw.sumOf { it.count }
        context.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            .edit()
            .putInt("total_contributions", totalContributions)
            .apply()

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

        val rows = 7
        val maxCols = 53
        val actualCols = (cells.size + rows - 1) / rows
        val padCols = maxCols - actualCols
        val padded = List(padCols * rows) { DayCell("", 0, 0) } + cells
        val cellSize = 49
        val cellPad = 6
        val columnSet = listOf(18, 18, 18)

        columnSet.forEachIndexed { page, colsPerPage ->
            val bmp = GridRenderer.renderPage(padded, page, rows, colsPerPage, cellSize, cellPad)
            File(context.filesDir, "grid_page_$page.png")
                .outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }

        context.getSharedPreferences("gh_widget", Context.MODE_PRIVATE).edit().apply {
            putString("user_default", id)
            putInt("page_count", columnSet.size)
            putInt("cell_size_px", cellSize)
            putInt("cell_pad_px", cellPad)
            apply()
        }

        GitHubProfile(login, name, avatarUrl, followers, totalContributions)
    }