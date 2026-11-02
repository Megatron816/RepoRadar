package com.example.githubprofileexplorer

import android.app.Application
import com.example.githubprofileexplorer.di.AppContainer

class GitHubExplorerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.onAppStart()
    }
}
