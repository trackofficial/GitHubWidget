package com.example.githubwidget

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GitHubDataWorker(appContext: Context, params: WorkerParameters)
    : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
        val user = prefs.getString("user_default", null) ?: return@withContext Result.failure()

        // ✅ Получаем токен из BuildConfig
        val token = BuildConfig.GITHUB_TOKEN

        return@withContext try {
            fetchGitHubData(user, applicationContext, token) // ✅ Передаём токен
            GitHubWidgetProvider.updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<GitHubDataWorker>(1, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "gh_update",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}