package com.example.githubprofileexplorer.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object PortfolioPdfShare {
    fun uri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareIntent(context: Context, file: File): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun previewIntent(context: Context, file: File): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            type = "application/pdf"
            data = uri(context, file)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
