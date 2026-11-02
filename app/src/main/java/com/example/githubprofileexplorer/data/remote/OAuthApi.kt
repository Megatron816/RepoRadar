package com.example.githubprofileexplorer.data.remote

import com.example.githubprofileexplorer.data.model.DeviceCodeResponse
import com.example.githubprofileexplorer.data.model.OAuthTokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/** GitHub OAuth device flow. These endpoints return form-encoded data unless JSON is requested. */
interface OAuthApi {

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/device/code")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String = "read:user"
    ): Response<DeviceCodeResponse>

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("login/oauth/access_token")
    suspend fun pollToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<OAuthTokenResponse>
}
