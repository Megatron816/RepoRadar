package com.example.githubprofileexplorer.data.remote

import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.model.GithubUser
import com.example.githubprofileexplorer.data.model.GraphQlRequest
import com.example.githubprofileexplorer.data.model.GraphQlResponse
import com.example.githubprofileexplorer.data.model.ReadmeResponse
import com.example.githubprofileexplorer.data.model.RepositoryMetadata
import com.example.githubprofileexplorer.data.model.GitTreeResponse
import com.example.githubprofileexplorer.data.model.GitContentResponse
import com.example.githubprofileexplorer.data.model.GitHubCommit
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Profile and repository listing endpoints. */
interface GithubApi {

    @Headers("Accept: application/vnd.github+json")
    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): Response<GithubUser>

    @Headers("Accept: application/vnd.github+json")
    @GET("users/{username}/repos")
    suspend fun getRepositories(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated",
        @Query("page") page: Int = 1
    ): Response<List<GitHubRepo>>
}

/** Per-repository endpoints. */
interface GithubRepoApi {

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<ReadmeResponse>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<RepositoryMetadata>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/git/trees/{treeSha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("treeSha", encoded = true) treeSha: String,
        @Query("recursive") recursive: Int = 1
    ): Response<GitTreeResponse>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref", encoded = true) ref: String
    ): Response<GitContentResponse>

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): Response<List<GitHubCommit>>
}

/** Streams a repository snapshot as a ZIP archive. GitHub answers with a redirect to its CDN. */
interface GithubDownloadApi {

    @Streaming
    @GET("repos/{owner}/{repo}/zipball/{ref}")
    suspend fun downloadZip(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String
    ): Response<ResponseBody>
}

/** GraphQL endpoint (always requires a signed-in token). */
interface GithubGraphQlApi {

    @Headers("Accept: application/vnd.github+json")
    @POST("graphql")
    suspend fun graphql(@Body request: GraphQlRequest): Response<GraphQlResponse>
}
