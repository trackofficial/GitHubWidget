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

            // Загружаем аватар из filesDir
            val avatarFile = File(ctx.filesDir, "avatar.png")
            val avatarBmp = avatarFile.takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)?.toCircle()
            }

            // Загружаем сетку из filesDir
            val gridFile = File(ctx.filesDir, "grid_page_$page.png")
            val gridBmp = gridFile.takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }

            val views = RemoteViews(ctx.packageName, R.layout.widget_container).apply {
                removeAllViews(R.id.container)

                // Header
                addView(R.id.container,
                    RemoteViews(ctx.packageName, R.layout.widget_header).apply {
                        setTextViewText(R.id.tvUsername, user)
                        setImageViewResource(R.id.ivAvatar, android.R.color.transparent)
                        avatarBmp?.let { setImageViewBitmap(R.id.ivAvatar, it) }
                    }
                )

                // Grid + Dots
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

            Log.d("GitHubWidget", "Обновляем RemoteViews и пушим на виджет")
            mgr.updateAppWidget(widgetId, views)
        }

        private fun pendingIntent(ctx: Context, action: String): PendingIntent {
            val intent = Intent(ctx, GitHubWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                ctx,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        super.onUpdate(ctx, mgr, ids)
        val prefs = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
        val user = prefs.getString("user_default", null) ?: return

        Log.d("GitHubWidget", "onUpdate — обновляем данные профиля и картинку сетки")
        CoroutineScope(Dispatchers.Default).launch {
            fetchGitHubData(user, ctx)
            ids.forEach { updateOne(ctx, mgr, it) }
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val prefs = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
        var page = prefs.getInt("page_default", 0)
        val pageCount = prefs.getInt("page_count", 1)

        Log.d("GitHubWidget", "onReceive action=${intent.action}")
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