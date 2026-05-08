package com.darkplaymc.app.presentation.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.darkplaymc.app.presentation.albums.AlbumDetailScreen
import com.darkplaymc.app.presentation.artists.ArtistDetailScreen
import com.darkplaymc.app.presentation.player.FullPlayerScreen
import com.darkplaymc.app.presentation.player.MiniPlayerBar
import com.darkplaymc.app.presentation.playlists.PlaylistDetailScreen
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel
import com.darkplaymc.app.ui.theme.DarkPlayMCTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: PlayerViewModel by viewModels()

    // Request appropriate read permission based on SDK version
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by ViewModel observing allSongs */ }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification permission result */ }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestAudioPermissionIfNeeded()
        requestNotificationPermissionIfNeeded()

        setContent {
            DarkPlayMCTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val showFullPlayer by vm.showFullPlayer.collectAsState()
                    val currentSong by vm.currentSong.collectAsState()

                    val sheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        skipHiddenState = false
                    )
                    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

                    LaunchedEffect(showFullPlayer) {
                        if (showFullPlayer && sheetState.currentValue != SheetValue.Expanded) {
                            sheetState.expand()
                        } else if (!showFullPlayer && sheetState.currentValue == SheetValue.Expanded) {
                            sheetState.partialExpand()
                        }
                    }

                    LaunchedEffect(sheetState.currentValue) {
                        if (sheetState.currentValue == SheetValue.Expanded) {
                            vm.openFullPlayer()
                        } else if (sheetState.currentValue == SheetValue.PartiallyExpanded) {
                            vm.closeFullPlayer()
                        }
                    }

                    LaunchedEffect(currentSong) {
                        if (currentSong != null && sheetState.currentValue == SheetValue.Hidden) {
                            sheetState.partialExpand()
                        }
                    }

                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        sheetPeekHeight = if (currentSong != null) 88.dp else 0.dp,
                        sheetDragHandle = null,
                        sheetContent = {
                            Box(modifier = Modifier.fillMaxHeight()) {
                                FullPlayerScreen(vm = vm)
                                
                                val miniPlayerAlpha by animateFloatAsState(
                                    targetValue = if (sheetState.targetValue == SheetValue.Expanded) 0f else 1f,
                                    label = "miniPlayerAlpha"
                                )
                                
                                if (miniPlayerAlpha > 0f) {
                                    Box(modifier = Modifier.align(Alignment.TopCenter).graphicsLayer(alpha = miniPlayerAlpha)) {
                                        MiniPlayerBar(vm = vm)
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            NavHost(navController = navController, startDestination = "main") {
                                composable("main") {
                                    MainScreen(vm = vm, navController = navController)
                                }
                                composable("playlist/{id}") { back ->
                                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                                    PlaylistDetailScreen(
                                        playlistId = id,
                                        vm = vm,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("artist/{name}") { back ->
                                    val name = Uri.decode(back.arguments?.getString("name") ?: "")
                                    ArtistDetailScreen(
                                        artistName = name,
                                        vm = vm,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                                composable("album/{id}") { back ->
                                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: 0L
                                    AlbumDetailScreen(
                                        albumId = id,
                                        vm = vm,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestAudioPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
