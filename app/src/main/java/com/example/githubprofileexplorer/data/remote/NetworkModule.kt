package com.example.githubprofileexplorer.data.remote

import android.content.Context
import com.example.githubprofileexplorer.data.auth.SecureTokenStore
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds the OkHttp/Retrofit stack. Responses from api.github.com go through an HTTP disk cache so
 * GitHub can answer unchanged data with `304 Not Modified`, which does not count against the rate limit.
 */
class NetworkModule(context: Context, private val tokenStore: SecureTokenStore) {

    private val headerInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder().header("User-Agent", USER_AGENT)
        if (original.header("Accept") == null) {
            builder.header("Accept", "application/vnd.github+json")
        }
        val isApiHost = original.url.host == API_HOST
        if (isApiHost) {
            tokenStore.read()?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
        }
        val response = chain.proceed(builder.build())
        if (isApiHost) RateLimitMonitor.update(response.header("X-RateLimit-Remaining"))
        response
    }

    private val apiClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "github-http"), CACHE_SIZE_BYTES))
        .addInterceptor(headerInterceptor)
        .build()

    // Large ZIP streams must not go through the response cache.
    private val downloadClient: OkHttpClient = apiClient.newBuilder()
        .cache(null)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiRetrofit = retrofit(API_BASE_URL, apiClient)
    private val downloadRetrofit = retrofit(API_BASE_URL, downloadClient)
    private val oauthRetrofit = retrofit(OAUTH_BASE_URL, apiClient)

    val githubApi: GithubApi by lazy { apiRetrofit.create(GithubApi::class.java) }
    val repoApi: GithubRepoApi by lazy { apiRetrofit.create(GithubRepoApi::class.java) }
    val graphQlApi: GithubGraphQlApi by lazy { apiRetrofit.create(GithubGraphQlApi::class.java) }
    val downloadApi: GithubDownloadApi by lazy { downloadRetrofit.create(GithubDownloadApi::class.java) }
    val oauthApi: OAuthApi by lazy { oauthRetrofit.create(OAuthApi::class.java) }

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private companion object {
        const val API_HOST = "api.github.com"
        const val API_BASE_URL = "https://api.github.com/"
        const val OAUTH_BASE_URL = "https://github.com/"
        const val USER_AGENT = "GitHubProfileExplorer/1.0"
        const val CACHE_SIZE_BYTES = 10L * 1024 * 1024
    }
}
