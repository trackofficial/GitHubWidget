package com.example.githubwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editUserId: EditText
    private lateinit var btnLoad: ImageButton
    private lateinit var tvName: TextView
    private lateinit var tvLogin: TextView
    private lateinit var tvTotalContrib: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var ivActivityGrid: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editUserId = findViewById(R.id.editUserId)
        btnLoad = findViewById(R.id.btnLoad)
        tvName = findViewById(R.id.tvName)
        tvLogin = findViewById(R.id.tvLogin)
        tvTotalContrib = findViewById(R.id.tvTotalContrib)
        ivAvatar = findViewById(R.id.ivAvatar)
        ivActivityGrid = findViewById(R.id.ivActivityGrid)

        val prefs   = getSharedPreferences("gh_widget", MODE_PRIVATE)
        val savedId = prefs.getString("user_default", "").orEmpty()
        if (savedId.isNotBlank()) {
            editUserId.setText(savedId)
            loadAndDisplayProfile(savedId)
        }

        btnLoad.setOnClickListener {
            val userId = editUserId.text.toString().trim()
            if (userId.isNotBlank()) {
                prefs.edit().putString("user_default", userId).apply()
                loadAndDisplayProfile(userId)
            } else {
                Toast.makeText(this, "Write your ID", Toast.LENGTH_SHORT).show()
            }
        }

        scheduleWidgetUpdate()
    }

    private fun loadAndDisplayProfile(userId: String) {
        lifecycleScope.launch {
            try {
                val profile = fetchGitHubData(userId, this@MainActivity)
                tvName.text = profile.name.ifBlank { "No name" }
                tvLogin.text = "${profile.login}"
                tvTotalContrib.text = "Contributions • ${profile.totalContributions}"
                // Аватар
                File(filesDir, "avatar.png").takeIf { it.exists() }?.let {
                    BitmapFactory.decodeFile(it.absolutePath)?.let { bmp ->
                        ivAvatar.setImageBitmap(bmp.toCircularBitmap())
                    }
                }

                val pages = (0 until 3).mapNotNull { page ->
                    File(filesDir, "grid_page_$page.png").takeIf { it.exists() }
                        ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                }
                if (pages.isNotEmpty()) {
                    val totalW = pages.sumOf { it.width }
                    val maxH = pages.maxOf { it.height }
                    val combined = Bitmap.createBitmap(totalW, maxH, pages[0].config)
                    val canvas = Canvas(combined)
                    var x = 0
                    pages.forEach {
                        canvas.drawBitmap(it, x.toFloat(), 0f, null)
                        x += it.width
                    }
                    ivActivityGrid.setImageBitmap(combined)
                }

                GitHubWidgetProvider.updateAll(this@MainActivity)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun Bitmap.toCircularBitmap(): Bitmap {
        val size = minOf(width, height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, null, rect, paint)

        return output
    }

    private fun scheduleWidgetUpdate() {
        val intent = Intent(this, GitHubWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pending = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5_000L,
            30 * 60 * 1000L,
            pending
        )
    }
}