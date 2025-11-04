package net.chasmine.oneline.data.git

/**
 * Git認証情報
 *
 * @param username ユーザー名（GitHubの場合はユーザーID）
 * @param token アクセストークン（GitHubの場合はPersonal Access Token）
 */
data class GitAuth(
    val username: String,
    val token: String
)
