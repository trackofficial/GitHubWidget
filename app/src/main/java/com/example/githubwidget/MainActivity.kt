package com.example.githubwidget

import android.graphics.*
import android.os.Bundle
import android.util.Log
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
    private lateinit var ivAvatar: ImageView
    private lateinit var ivActivityGrid: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editUserId = findViewById(R.id.editUserId)
        btnLoad  = findViewById(R.id.btnLoad)
        tvName = findViewById(R.id.tvName)
        tvLogin = findViewById(R.id.tvLogin)
        ivAvatar = findViewById(R.id.ivAvatar)
        ivActivityGrid = findViewById(R.id.ivActivityGrid)

        val prefs = getSharedPreferences("gh_widget", MODE_PRIVATE)
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
                Toast.makeText(this, "Введите GitHub ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAndDisplayProfile(userId: String) {
        lifecycleScope.launch {
            try {
                val profile = fetchGitHubData(userId, this@MainActivity)
                tvName.text = if (profile.name.isNotBlank()) profile.name else "No name"
                tvLogin.text = "${profile.login}"

                val avatarFile = File(filesDir, "avatar.png")
                if (avatarFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                    ivAvatar.setImageBitmap(bmp?.toCircularBitmap())
                } else {
                    Log.w("MainActivity", "avatar.png не найден")
                    ivAvatar.setImageResource(R.drawable.avatar_placeholder)
                }


                val prefs = getSharedPreferences("gh_widget", MODE_PRIVATE)
                val pageCount = prefs.getInt("page_count", 3)

                val bitmaps = (0 until pageCount).mapNotNull { page ->
                    File(filesDir, "grid_page_$page.png")
                        .takeIf { it.exists() }
                        ?.let { BitmapFactory.decodeFile(it.absolutePath) }
                }

                if (bitmaps.isNotEmpty()) {
                    val totalWidth = bitmaps.sumOf { it.width }
                    val maxHeight = bitmaps.maxOf { it.height }
                    val combined = Bitmap.createBitmap(totalWidth, maxHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(combined)
                    var xOffset = 0
                    for (bmp in bitmaps) {
                        canvas.drawBitmap(bmp, xOffset.toFloat(), 0f, null)
                        xOffset += bmp.width
                    }
                    ivActivityGrid.setImageBitmap(combined)
                } else {
                    ivActivityGrid.setImageResource(R.drawable.grid_placeholder)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при загрузке профиля", e)
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
        val rectF = RectF(rect)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, null, rect, paint)

        return output
    }
}