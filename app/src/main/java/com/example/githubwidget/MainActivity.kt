package com.example.githubwidget

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var editUserId: EditText
    private lateinit var btnLoad: Button
    private lateinit var tvName: TextView
    private lateinit var tvLogin: TextView
    private lateinit var tvFollowers: TextView
    private lateinit var ivAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editUserId = findViewById(R.id.editUserId)
        btnLoad = findViewById(R.id.btnLoad)
        tvName = findViewById(R.id.tvName)
        tvLogin = findViewById(R.id.tvLogin)
        tvFollowers = findViewById(R.id.tvFollowers)
        ivAvatar = findViewById(R.id.ivAvatar)

        val prefs = getSharedPreferences("gh_widget", MODE_PRIVATE)
        val savedUser = prefs.getString("user_default", "")

        if (!savedUser.isNullOrBlank()) {
            editUserId.setText(savedUser)
            loadAndDisplayProfile(savedUser)
        }

        btnLoad.setOnClickListener {
            val userId = editUserId.text.toString().trim()
            if (userId.isNotEmpty()) {
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
                tvLogin.text = "GitHub ID: ${profile.login}"
                tvFollowers.text = "Followers: ${profile.followers}"

                val avatarFile = File(filesDir, "avatar.png")
                if (avatarFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                    ivAvatar.setImageBitmap(bmp)
                } else {
                    Log.w("MainActivity", "avatar.png не найден в filesDir")
                    ivAvatar.setImageResource(R.drawable.avatar_placeholder)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка при загрузке профиля", e)
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}