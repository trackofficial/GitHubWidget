package com.example.githubwidget

data class GitHubProfile(
    val login: String,
    val name: String,
    val avatarUrl: String,
    val followers: Int,
    val totalContributions: Int
)