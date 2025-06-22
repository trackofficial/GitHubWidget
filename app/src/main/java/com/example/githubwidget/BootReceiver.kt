package com.example.githubwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Перезапускаем наш регулярный воркер
            GitHubDataWorker.schedule(ctx)
        }
    }
}