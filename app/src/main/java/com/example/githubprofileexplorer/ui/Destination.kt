package com.example.githubprofileexplorer.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.githubprofileexplorer.R
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Bookmark
import com.example.githubprofileexplorer.ui.components.AppIcons

/** Every feature of the app, in the order it appears in the sidebar. */
enum class Destination(@StringRes val titleRes: Int) {
    Profile(R.string.nav_profile),
    Repositories(R.string.nav_repositories),
    Languages(R.string.nav_languages),
    Contributions(R.string.nav_contributions),
    Compare(R.string.nav_compare),
    Downloads(R.string.nav_downloads),
    ReadingQueue(R.string.nav_reading_queue),
    ProfileHistory(R.string.nav_profile_history),
    Account(R.string.nav_account);

    val icon: ImageVector
        get() = when (this) {
            Profile -> Icons.Default.Person
            Repositories -> Icons.AutoMirrored.Filled.List
            Languages -> AppIcons.Code
            Contributions -> Icons.Default.DateRange
            Compare -> AppIcons.Compare
            Downloads -> AppIcons.Download
            ReadingQueue -> Icons.Default.Bookmark
            ProfileHistory -> Icons.Default.History
            Account -> Icons.Default.AccountCircle
        }
}
