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
import java.time.DayOfWeek
import java.time.LocalDate
import javax.net.ssl.HttpsURLConnection

suspend fun fetchGitHubData(id: String, context: Context, token: String): GitHubProfile =
    withContext(Dispatchers.IO) {

        fun fetchJson(url: String): JSONObject {
            val connection = URL(url).openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "GitHubWidget")

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

        // 1. Получаем данные профиля
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

        // 2. ✅ ИСПРАВЛЕНИЕ: запрашиваем "последний год" как на GitHub (?y=last)
        val gridJson = fetchJson("https://github-contributions-api.jogruber.de/v4/$id?y=last")
        val contributions = gridJson.getJSONArray("contributions")
        val raw = mutableListOf<DayCell>()

        for (i in 0 until contributions.length()) {
            val cell = contributions.getJSONObject(i)
            raw += DayCell(
                date = cell.getString("date"),
                count = cell.getInt("count"),
                level = cell.optInt("level", 0)
            )
        }

        val totalContributions = raw.sumOf { it.count }
        context.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            .edit()
            .putInt("total_contributions", totalContributions)
            .apply()

        // 3. ✅ ИСПРАВЛЕНИЕ: правильно выравниваем сетку по дням недели
        val rows = 7
        val maxCols = 53
        val maxCells = maxCols * rows // 371

        // Сортируем по дате на всякий случай
        val sorted = raw.sortedBy { LocalDate.parse(it.date) }

        // Определяем сегодняшнюю дату и начало периода (53 недели назад, выровненное по воскресенью)
        val today = LocalDate.now()
        val startDate = today.minusWeeks(53).let { date ->
            // GitHub начинает сетку с воскресенья (Sunday)
            val daysSinceSunday = date.dayOfWeek.value % 7
            date.minusDays(daysSinceSunday.toLong())
        }

        // Фильтруем только нужный диапазон (на случай, если API вернёт лишнее)
        val filtered = sorted.filter {
            val date = LocalDate.parse(it.date)
            !date.isBefore(startDate) && !date.isAfter(today)
        }

        // Вычисляем, сколько пустых ячеек нужно в начале для выравнивания
        // Если startDate — воскресенье, padding = 0; если понедельник — 1, и т.д.
        val firstDayPadding = startDate.dayOfWeek.value % 7

        // Создаём полную сетку: пустые ячейки + данные + пустые ячейки в конце (если нужно)
        val fullGrid = List(firstDayPadding) { DayCell("", 0, 0) } + filtered

        // Обрезаем или дополняем до ровно 371 ячейки
        val padded = if (fullGrid.size >= maxCells) {
            fullGrid.take(maxCells)
        } else {
            fullGrid + List(maxCells - fullGrid.size) { DayCell("", 0, 0) }
        }

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