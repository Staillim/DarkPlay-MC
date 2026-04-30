package com.darkplaymc.app.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkplaymc.app.R
import com.darkplaymc.app.presentation.albums.AlbumsScreen
import com.darkplaymc.app.presentation.artists.ArtistsScreen
import com.darkplaymc.app.presentation.playlists.PlaylistsScreen
import com.darkplaymc.app.presentation.songs.SongsScreen
import com.darkplaymc.app.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: PlayerViewModel, navController: NavController) {
    val tabLabels = listOf(
        stringResource(R.string.tab_songs),
        stringResource(R.string.tab_playlists),
        stringResource(R.string.tab_artists),
        stringResource(R.string.tab_albums)
    )

    val pagerState = rememberPagerState(pageCount = { tabLabels.size })
    val coroutineScope = rememberCoroutineScope()
    val currentSong by vm.currentSong.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Clear search when navigating away from songs tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 0) {
            showSearch = false
            searchQuery = ""
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        AnimatedVisibility(visible = showSearch, enter = fadeIn(), exit = fadeOut()) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_songs)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        AnimatedVisibility(visible = !showSearch, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 0) {
                            IconButton(onClick = {
                                showSearch = !showSearch
                                if (!showSearch) searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (showSearch) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // iOS-style tab row: text + underline indicator
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = pagerState.currentPage == index
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                            animationSpec = tween(200),
                            label = "tab_text_$index"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    letterSpacing = (-0.1f).sp
                                ),
                                color = textColor
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = 20.dp, height = 2.dp)
                                    .background(
                                        if (isSelected) Color.White else Color.Transparent,
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> SongsScreen(vm = vm, navController = navController, searchQuery = searchQuery)
                1 -> PlaylistsScreen(vm = vm, navController = navController)
                2 -> ArtistsScreen(vm = vm, navController = navController)
                3 -> AlbumsScreen(vm = vm, navController = navController)
            }
        }
    }
}
