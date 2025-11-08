package com.example.githubwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import com.example.githubwidget.WidgetAssets.toCircle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class GitHubWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_NEXT = "gh_next"
        private const val ACTION_PREV = "gh_prev"

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, GitHubWidgetProvider::class.java))
            ids.forEach { updateOne(ctx, mgr, it) }
        }

        fun updateOne(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            Log.d("GitHubWidget", "updateOne widgetId=$widgetId")
            val prefs = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            val user = prefs.getString("user_default", "") ?: ""
            val page = prefs.getInt("page_default", 0)
            val pageCount = prefs.getInt("page_count", 1)

            val avatarBmp = File(ctx.filesDir, "avatar.png").takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)?.toCircle()
            }

            val gridBmp = File(ctx.filesDir, "grid_page_$page.png").takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }

            val views = RemoteViews(ctx.packageName, R.layout.widget_container).apply {
                removeAllViews(R.id.container)

                addView(R.id.container,
                    RemoteViews(ctx.packageName, R.layout.widget_header).apply {
                        setTextViewText(R.id.tvUsername, user)
                        setImageViewResource(R.id.ivAvatar, android.R.color.transparent)
                        avatarBmp?.let { setImageViewBitmap(R.id.ivAvatar, it) }
                    }
                )
                addView(R.id.container,
                    RemoteViews(ctx.packageName, R.layout.widget_grid).apply {
                        setImageViewResource(R.id.ivGrid, android.R.color.transparent)
                        gridBmp?.let { setImageViewBitmap(R.id.ivGrid, it) }

                        setOnClickPendingIntent(R.id.prevZone, pendingIntent(ctx, ACTION_PREV))
                        setOnClickPendingIntent(R.id.nextZone, pendingIntent(ctx, ACTION_NEXT))

                        removeAllViews(R.id.dotRow)
                        for (i in 0 until pageCount) {
                            val dot = RemoteViews(ctx.packageName, R.layout.dot_item)
                            dot.setImageViewResource(
                                R.id.dot,
                                if (i == page) R.drawable.dot_active else R.drawable.dot_inactive
                            )
                            addView(R.id.dotRow, dot)
                        }
                    }
                )
            }

            Log.d("GitHubWidget", "Pushing widget update")
            mgr.updateAppWidget(widgetId, views)
        }

        private fun pendingIntent(ctx: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                action.hashCode(),
                Intent(ctx, GitHubWidgetProvider::class.java).apply { this.action = action },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        super.onUpdate(ctx, mgr, ids)
        val user = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            .getString("user_default", null) ?: return

        val token = BuildConfig.GITHUB_TOKEN // ✅ Добавляем токен

        Log.d("GitHubWidget", "onUpdate — fetch profile & updateAll")
        CoroutineScope(Dispatchers.Default).launch {
            fetchGitHubData(user, ctx, token) // ✅ Передаём токен
            ids.forEach { updateOne(ctx, mgr, it) }
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        Log.d("GitHubWidget", "onReceive action=${intent.action}")

        val prefs = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
        var page = prefs.getInt("page_default", 0)
        val pageCount = prefs.getInt("page_count", 1)

        when (intent.action) {
            ACTION_NEXT -> page = (page + 1) % pageCount
            ACTION_PREV -> page = (page - 1 + pageCount) % pageCount
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAll(ctx)
                return
            }
            else -> return
        }

        prefs.edit().putInt("page_default", page).apply()
        updateAll(ctx)
    }
}