package com.example.githubprofileexplorer.ui

import android.content.ActivityNotFoundException
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.githubprofileexplorer.R
import com.example.githubprofileexplorer.data.local.DownloadStatus
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.remote.RateLimitMonitor
import com.example.githubprofileexplorer.ui.account.AccountScreen
import com.example.githubprofileexplorer.ui.account.AuthState
import com.example.githubprofileexplorer.ui.account.AuthViewModel
import com.example.githubprofileexplorer.ui.compare.CompareScreen
import com.example.githubprofileexplorer.ui.compare.CompareViewModel
import com.example.githubprofileexplorer.ui.contributions.ContributionsScreen
import com.example.githubprofileexplorer.ui.contributions.ContributionsViewModel
import com.example.githubprofileexplorer.ui.downloads.DownloadsEvent
import com.example.githubprofileexplorer.ui.downloads.DownloadsScreen
import com.example.githubprofileexplorer.ui.downloads.DownloadsViewModel
import com.example.githubprofileexplorer.ui.languages.LanguagesScreen
import com.example.githubprofileexplorer.ui.local.LocalFeaturesViewModel
import com.example.githubprofileexplorer.ui.local.ProfileHistoryScreen
import com.example.githubprofileexplorer.ui.local.ReadingQueueScreen
import com.example.githubprofileexplorer.ui.profile.ProfileScreen
import com.example.githubprofileexplorer.ui.profile.ProfileViewModel
import com.example.githubprofileexplorer.ui.portfolio.PortfolioViewModel
import com.example.githubprofileexplorer.ui.portfolio.PortfolioScreen
import com.example.githubprofileexplorer.ui.architecture.ArchitectureScreen
import com.example.githubprofileexplorer.ui.architecture.ArchitectureViewModel
import com.example.githubprofileexplorer.ui.profile.SearchState
import com.example.githubprofileexplorer.data.export.PortfolioPdfShare
import com.example.githubprofileexplorer.ui.portfolio.PortfolioExportState
import com.example.githubprofileexplorer.ui.portfolio.PortfolioMessage
import com.example.githubprofileexplorer.ui.repos.RepoDetailScreen
import com.example.githubprofileexplorer.ui.repos.RepositoriesScreen
import com.example.githubprofileexplorer.ui.repos.downloadKey
import com.example.githubprofileexplorer.data.local.ReadingStatus
import com.example.githubprofileexplorer.data.model.GitHubOwner
import kotlinx.coroutines.launch

private val PermanentDrawerMinWidth = 840.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModelFactory: ViewModelProvider.Factory,
    deepLinkUsername: String?
) {
    val profileVm: ProfileViewModel = viewModel(factory = viewModelFactory)
    val authVm: AuthViewModel = viewModel(factory = viewModelFactory)
    val contributionsVm: ContributionsViewModel = viewModel(factory = viewModelFactory)
    val compareVm: CompareViewModel = viewModel(factory = viewModelFactory)
    val downloadsVm: DownloadsViewModel = viewModel(factory = viewModelFactory)
    val localVm: LocalFeaturesViewModel = viewModel(factory = viewModelFactory)
    val portfolioVm: PortfolioViewModel = viewModel(factory = viewModelFactory)
    val architectureVm: ArchitectureViewModel = viewModel(factory = viewModelFactory)

    val profileState by profileVm.uiState.collectAsStateWithLifecycle()
    val authState by authVm.state.collectAsStateWithLifecycle()
    val contributionsState by contributionsVm.state.collectAsStateWithLifecycle()
    val compareState by compareVm.state.collectAsStateWithLifecycle()
    val history by downloadsVm.history.collectAsStateWithLifecycle()
    val progress by downloadsVm.progress.collectAsStateWithLifecycle()
    val readingQueue by localVm.readingQueue.collectAsStateWithLifecycle()
    val profileHistory by localVm.profileHistory.collectAsStateWithLifecycle()
    val portfolioState by portfolioVm.state.collectAsStateWithLifecycle()
    val selectedProjects by portfolioVm.selectedProjects.collectAsStateWithLifecycle()
    val portfolioExportState = portfolioState.exportState
    val architectureState by architectureVm.state.collectAsStateWithLifecycle()
    val remainingRequests by RateLimitMonitor.remaining.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = Destination.fromRoute(backStackEntry?.destination?.route)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val signedIn = authState is AuthState.SignedIn
    val profile = profileState.profile
    val selectedRepo = profileState.selectedRepo
    val showingDetail = destination == Destination.Repositories && selectedRepo != null
    val activeDownloads = remember(history) {
        history.filter { it.status == DownloadStatus.DOWNLOADING }.map { "${it.owner}/${it.repoName}" }.toSet()
    }
    val queuedRepos = remember(readingQueue) { readingQueue.map { "${it.owner}/${it.repoName}" }.toSet() }

    val startedMessage = stringResource(R.string.download_started)
    val viewLabel = stringResource(R.string.view)
    val fileMissingMessage = stringResource(R.string.download_file_missing)
    val noViewerMessage = stringResource(R.string.download_no_viewer)
    val noShareMessage = stringResource(R.string.download_no_share)
    val sharePortfolioLabel = stringResource(R.string.share_portfolio)
    val portfolioExportError = stringResource(R.string.portfolio_export_error)
    val portfolioSaveError = stringResource(R.string.portfolio_save_error)
    val portfolioRemoveError = stringResource(R.string.portfolio_remove_error)
    val portfolioInvalidSelection = stringResource(R.string.portfolio_invalid_selection)

    LaunchedEffect(deepLinkUsername) {
        if (deepLinkUsername != null) {
            profileVm.handleDeepLink(deepLinkUsername)
            navController.navigate(Destination.Profile.route) {
                popUpTo(Destination.Home.route)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(profile?.user?.login, profile?.repos) {
        profile?.let { portfolioVm.open(it.user) }
    }

    LaunchedEffect(destination, profile?.user?.login, signedIn) {
        val login = profile?.user?.login
        if (destination == Destination.Contributions && login != null) {
            contributionsVm.load(login, signedIn)
        }
    }

    LaunchedEffect(portfolioExportState) {
        when (val export = portfolioExportState) {
            is PortfolioExportState.Ready -> {
                try {
                    context.startActivity(
                        Intent.createChooser(
                            PortfolioPdfShare.shareIntent(context, export.file),
                            sharePortfolioLabel
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    snackbarHostState.showSnackbar(noShareMessage)
                }

                portfolioVm.clearExportState()
            }
            PortfolioExportState.Error -> {
                snackbarHostState.showSnackbar(portfolioExportError)
                portfolioVm.clearExportState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(portfolioState.message) {
        val message = when (portfolioState.message) {
            PortfolioMessage.INVALID_SELECTION -> portfolioInvalidSelection
            PortfolioMessage.SAVE_FAILED -> portfolioSaveError
            PortfolioMessage.REMOVE_FAILED -> portfolioRemoveError
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            portfolioVm.clearMessage()
        }
    }

    LaunchedEffect(downloadsVm) {
        downloadsVm.events.collect { event ->
            when (event) {
                is DownloadsEvent.Started -> scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "$startedMessage: ${event.repoName}",
                        actionLabel = viewLabel
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        navController.navigate(Destination.Downloads.route)
                    }
                }

                is DownloadsEvent.OpenFile -> if (!openDownloadedFile(context, event.uri)) {
                    scope.launch { snackbarHostState.showSnackbar(noViewerMessage) }
                }

                is DownloadsEvent.ShareFile -> if (!shareDownloadedFile(context, event.uri)) {
                    scope.launch { snackbarHostState.showSnackbar(noShareMessage) }
                }

                DownloadsEvent.FileMissing -> scope.launch { snackbarHostState.showSnackbar(fileMissingMessage) }
            }
        }
    }

    val goTo: (Destination) -> Unit = { target ->
        navController.navigate(target.route) {
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = !drawerState.isOpen && (destination != Destination.Home || showingDetail)) {
        if (showingDetail) {
            profileVm.selectRepo(null)
        } else {
            navController.navigate(Destination.Home.route) {
                popUpTo(Destination.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val openRepo: (GitHubRepo) -> Unit = { repo ->
        profileVm.selectRepo(repo)
        navController.navigate(Destination.Repositories.route) {
            launchSingleTop = true
        }
    }

    val appContent: @Composable (showMenuButton: Boolean) -> Unit = { showMenuButton ->
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (showingDetail) selectedRepo.name
                            else stringResource(destination.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        when {
                            showingDetail -> IconButton(onClick = { profileVm.selectRepo(null) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }

                            showMenuButton -> IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_menu))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            NavHost(
                navController = navController,
                startDestination = Destination.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Destination.Home.route) {
                    HomeScreen(
                        onSearch = { navController.navigate(Destination.Profile.route) },
                        onPortfolio = { navController.navigate(Destination.Portfolio.route) },
                        onCompare = { navController.navigate(Destination.Compare.route) },
                        recentProfiles = profileHistory,
                        onOpenRecentProfile = { item ->
                            profileVm.openUsername(item.login)
                            navController.navigate(Destination.Profile.route)
                        },
                        onSeeAllRecentProfiles = {
                            navController.navigate(Destination.ProfileHistory.route)
                        },
                        modifier = contentModifier
                    )
                }
                composable(Destination.Portfolio.route) {
                    PortfolioScreen(
                        profile = profile,
                        state = portfolioState,
                        selectedProjects = selectedProjects,
                        onOpenProfile = { navController.navigate(Destination.Profile.route) },
                        onLanguageChange = portfolioVm::setLanguage,
                        onTopicChange = portfolioVm::setTopic,
                        onMinimumStarsChange = portfolioVm::setMinimumStars,
                        onRecentDaysChange = portfolioVm::setRecentDays,
                        onIncludeArchivedChange = portfolioVm::setIncludeArchived,
                        onIncludeForksChange = portfolioVm::setIncludeForks,
                        onSortChange = portfolioVm::setSort,
                        onClearFilters = portfolioVm::clearFilters,
                        onToggleSaved = portfolioVm::toggleSaved,
                        onToggleProject = portfolioVm::toggleProject,
                        onRefresh = profileVm::search,
                        onExport = { profile?.let(portfolioVm::export) },
                        modifier = contentModifier
                    )
                }
                composable(Destination.Architecture.route) {
                    ArchitectureScreen(
                        state = architectureState,
                        onQueryChange = architectureVm::setQuery,
                        onModulesOnlyChange = architectureVm::setModulesOnly,
                        onEcosystemChange = architectureVm::setEcosystem,
                        onOpenSnapshot = architectureVm::open,
                        onDeleteSnapshot = architectureVm::delete,
                        modifier = contentModifier
                    )
                }
                composable(Destination.Profile.route) {
                    ProfileScreen(
                    state = profileState,
                    activeDownloads = activeDownloads,
                    queuedRepos = queuedRepos,
                    onQueryChange = profileVm::onQueryChange,
                    onSearch = profileVm::search,
                    onExampleClick = profileVm::openUsername,
                    onRetryRepos = profileVm::retryRepositories,
                    onOpenRepo = openRepo,
                    onSeeAllRepos = { navController.navigate(Destination.Repositories.route) },
                    onDownload = downloadsVm::download,
                    onSaveToggle = { repo ->
                        if (repo.downloadKey() in queuedRepos) localVm.removeRepo(repo) else localVm.saveRepo(repo)
                    },
                    modifier = contentModifier
                    )
                }

                composable(Destination.Repositories.route) {
                    if (selectedRepo != null) {
                        RepoDetailScreen(
                            repo = selectedRepo,
                            readme = profileState.readme,
                            downloading = selectedRepo.downloadKey() in activeDownloads,
                            onDownload = { downloadsVm.download(selectedRepo) },
                            onExploreArchitecture = {
                                architectureVm.scan(selectedRepo.ownerLogin, selectedRepo.name, selectedRepo.defaultBranch)
                                navController.navigate(Destination.Architecture.route)
                            },
                            saved = selectedRepo.downloadKey() in queuedRepos,
                            onSaveToggle = {
                                if (selectedRepo.downloadKey() in queuedRepos) localVm.removeRepo(selectedRepo)
                                else localVm.saveRepo(selectedRepo)
                            },
                            modifier = contentModifier
                        )
                    } else {
                        RepositoriesScreen(
                            profile = profile,
                            reloading = (profileState.search as? SearchState.Success)?.reloadingRepos == true,
                            activeDownloads = activeDownloads,
                            onRetryRepos = profileVm::retryRepositories,
                            onOpenRepo = openRepo,
                            onDownload = downloadsVm::download,
                            queuedRepos = queuedRepos,
                            onSaveToggle = { repo ->
                                if (repo.downloadKey() in queuedRepos) localVm.removeRepo(repo) else localVm.saveRepo(repo)
                            },
                            onGoToProfile = { navController.navigate(Destination.Profile.route) },
                            modifier = contentModifier
                        )
                    }
                }

                composable(Destination.Languages.route) { LanguagesScreen(
                    profile = profile,
                    onGoToProfile = { navController.navigate(Destination.Profile.route) },
                    modifier = contentModifier
                ) }

                composable(Destination.Contributions.route) { ContributionsScreen(
                    profile = profile,
                    state = contributionsState,
                    onSignIn = { navController.navigate(Destination.Account.route) },
                    onRetry = {
                        profile?.let { contributionsVm.load(it.user.login, signedIn, force = true) }
                    },
                    onGoToProfile = { navController.navigate(Destination.Profile.route) },
                    modifier = contentModifier
                ) }

                composable(Destination.Compare.route) { CompareScreen(
                    state = compareState,
                    onLeftChange = compareVm::onLeftChange,
                    onRightChange = compareVm::onRightChange,
                    onCompare = compareVm::compare,
                    modifier = contentModifier
                ) }

                composable(Destination.Downloads.route) { DownloadsScreen(
                    history = history,
                    progress = progress,
                    onOpen = downloadsVm::open,
                    onShare = downloadsVm::share,
                    onRetry = { downloadsVm.retry(it.id) },
                    onDelete = { downloadsVm.delete(it.id) },
                    onClearAll = downloadsVm::clearAll,
                    modifier = contentModifier
                ) }

                composable(Destination.ReadingQueue.route) { ReadingQueueScreen(
                    items = readingQueue,
                    onOpen = { item ->
                        profileVm.selectRepo(item.toRepo())
                        navController.navigate(Destination.Repositories.route)
                    },
                    onStatusChange = localVm::updateStatus,
                    modifier = contentModifier
                ) }

                composable(Destination.ProfileHistory.route) { ProfileHistoryScreen(
                    items = profileHistory,
                    onOpen = { item ->
                        profileVm.openUsername(item.login)
                        navController.navigate(Destination.Profile.route)
                    },
                    onRefresh = { item ->
                        localVm.refresh(item) { login ->
                            profileVm.openUsername(login)
                            navController.navigate(Destination.Profile.route)
                        }
                    },
                    modifier = contentModifier
                ) }

                composable(Destination.Account.route) { AccountScreen(
                    state = authState,
                    isConfigured = authVm.isConfigured,
                    remainingRequests = remainingRequests,
                    onSignIn = authVm::signIn,
                    onCancel = authVm::cancel,
                    onSignOut = authVm::signOut,
                    modifier = contentModifier
                ) }
            }

        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= PermanentDrawerMinWidth) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(Modifier.width(300.dp)) {
                        DrawerBody(
                            selected = destination,
                            activeDownloads = activeDownloads.size,
                            remainingRequests = remainingRequests,
                            onSelect = goTo
                        )
                    }
                }
            ) { appContent(false) }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        DrawerBody(
                            selected = destination,
                            activeDownloads = activeDownloads.size,
                            remainingRequests = remainingRequests,
                            onSelect = goTo
                        )
                    }
                }
            ) { appContent(true) }
        }
    }
}

private fun com.example.githubprofileexplorer.data.local.ReadingQueueEntity.toRepo() =
    GitHubRepo(
        name = repoName,
        description = description,
        language = language,
        htmlUrl = htmlUrl,
        stargazersCount = stars,
        forksCount = forks,
        defaultBranch = defaultBranch,
        fullName = "$owner/$repoName",
        owner = GitHubOwner(owner)
    )

@Composable
private fun DrawerBody(
    selected: Destination,
    activeDownloads: Int,
    remainingRequests: Int?,
    onSelect: (Destination) -> Unit
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.drawer_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Destination.entries.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(stringResource(item.titleRes)) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    badge = if (item == Destination.Downloads && activeDownloads > 0) {
                        { Text(activeDownloads.toString()) }
                    } else null,
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            if (remainingRequests != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        stringResource(R.string.api_requests_remaining, remainingRequests),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (remainingRequests < 10) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp)
        ) {
            Text(
                stringResource(R.string.developed_by),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.developer_email),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Opens the saved ZIP in a viewer app, falling back to the system Downloads screen. */
private fun openDownloadedFile(context: Context, uriString: String): Boolean {
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriString.toUri(), "application/zip")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(view)
        true
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            true
        } catch (e2: ActivityNotFoundException) {
            false
        }
    }
}

private fun shareDownloadedFile(context: Context, uriString: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uriString.toUri())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share_download))
        )
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
