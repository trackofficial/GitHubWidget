package com.example.githubwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var etGithubId: EditText
    private lateinit var btnConfirm: Button
    private lateinit var profileBlock: LinearLayout
    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvFollowers: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etGithubId   = findViewById(R.id.etGithubId)
        btnConfirm   = findViewById(R.id.btnConfirm)
        profileBlock = findViewById(R.id.profileBlock)
        ivProfile    = findViewById(R.id.ivProfile)
        tvName       = findViewById(R.id.tvName)
        tvFollowers  = findViewById(R.id.tvFollowers)

        btnConfirm.setOnClickListener {
            val user = etGithubId.text.toString().trim()
            if (user.isEmpty()) {
                etGithubId.error = "Обязательное поле"
                return@setOnClickListener
            }

            profileBlock.visibility = View.GONE
            Toast.makeText(this, "Загружаю данные...", Toast.LENGTH_SHORT).show()

            // Сохраняем имя пользователя
            getSharedPreferences("gh_widget", Context.MODE_PRIVATE).edit()
                .putString("user_default", user)
                .putInt("page_default", 0)
                .apply()

            lifecycleScope.launch {
                try {
                    val profile = fetchGitHubData(user, this@MainActivity)
                    showProfile(profile)
                    updateAllWidgets()
                    Toast.makeText(this@MainActivity, "Виджет обновлён", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showProfile(profile: GitHubProfile) {
        val avatar = File(cacheDir, "avatar.png")
        if (avatar.exists()) {
            BitmapFactory.decodeFile(avatar.absolutePath)?.let {
                ivProfile.setImageBitmap(it)
            }
        }
        tvName.text = profile.name.ifBlank { profile.login }
        tvFollowers.text = "Подписчиков: ${profile.followers}"
        profileBlock.visibility = View.VISIBLE
    }

    private fun updateAllWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, GitHubWidgetProvider::class.java))
        ids.forEach { id ->
            GitHubWidgetProvider.updateOne(this, manager, id)
        }
    }
}