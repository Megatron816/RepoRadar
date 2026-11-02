package com.example.githubprofileexplorer.data.model

data class DeviceCodeResponse(
    val device_code: String?,
    val user_code: String?,
    val verification_uri: String?,
    val interval: Long = 5,
    val expires_in: Long = 900
)

data class OAuthTokenResponse(
    val access_token: String?,
    val error: String?,
    val interval: Long? = null
)
