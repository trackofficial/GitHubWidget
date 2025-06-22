package com.example.githubwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import java.io.File

class GitHubWidgetProvider : AppWidgetProvider() {
    companion object {
        fun updateOne(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val prefs = ctx.getSharedPreferences("gh_widget", Context.MODE_PRIVATE)
            val user = prefs.getString("user_default", null) ?: return

            val avatar = File(ctx.cacheDir, "avatar.png").takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            } ?: WidgetAssets.createAvatarPlaceholder(32)

            val grid = File(ctx.cacheDir, "grid.png").takeIf { it.exists() }?.let {
                BitmapFactory.decodeFile(it.absolutePath)
            }

            val views = RemoteViews(ctx.packageName, R.layout.widget_container).apply {
                removeAllViews(R.id.container)

                val header = RemoteViews(ctx.packageName, R.layout.widget_header).apply {
                    setTextViewText(R.id.tvUsername, user)
                    setImageViewBitmap(R.id.ivAvatar, avatar)
                }
                addView(R.id.container, header)

                val body = RemoteViews(ctx.packageName, R.layout.widget_grid).apply {
                    grid?.let { setImageViewBitmap(R.id.ivGrid, it) }
                }
                addView(R.id.container, body)
            }

            mgr.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateOne(ctx, mgr, it) }
    }

    override fun onReceive(ctx: Context, intent: android.content.Intent) {
        super.onReceive(ctx, intent)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, GitHubWidgetProvider::class.java))
        ids.forEach { updateOne(ctx, mgr, it) }
    }
}