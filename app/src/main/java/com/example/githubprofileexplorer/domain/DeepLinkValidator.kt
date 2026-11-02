package com.example.githubprofileexplorer.domain

import android.net.Uri
import com.example.githubprofileexplorer.data.repository.ProfileRepository
import java.net.URI

/** Accepts only `https://github.com/<valid-username>[/...]` links. */
object DeepLinkValidator {

    fun username(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme != "https" || uri.host != "github.com") null
        else uri.path.trim('/').split('/').firstOrNull()?.takeIf(ProfileRepository::isValidUsername)
    }.getOrNull()

    fun username(uri: Uri?): String? {
        if (uri == null || uri.scheme != "https" || uri.host != "github.com") return null
        return uri.pathSegments.firstOrNull()?.takeIf(ProfileRepository::isValidUsername)
    }
}
