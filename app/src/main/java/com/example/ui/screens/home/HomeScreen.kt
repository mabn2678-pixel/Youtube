package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.common.VideoCard
import com.example.ui.common.YouTubeShortsRow

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    authManager: com.example.data.auth.AuthManager,
    onVideoClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onChannelClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    var showAccountSheet by remember { mutableStateOf(false) }

    val categories = listOf(
        "all" to "الكل",
        "music" to "موسيقى",
        "gaming" to "ألعاب فيديو",
        "live" to "مباشر",
        "news" to "أخبار",
        "podcasts" to "بودكاست",
        "tech" to "تقنية",
        "quran" to "قرآن كريم",
        "documentaries" to "وثائقيات",
        "automotive" to "سيارات"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // YouTube App Bar: Red Icon + Brand Logo + (Cast, Notifications, Search, Profile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // YouTube Brand Identity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = { viewModel.selectCategory("all") })
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp, 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF0000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "YouTube",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Top action buttons: Cast, Notifications, Search, User Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Cast,
                        contentDescription = "بث إلى شاشة",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "الإشعارات",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        // Notification dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000))
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("youtube_search_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_hint),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Profile Avatar Circle
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (currentUser != null) MaterialTheme.colorScheme.primary else Color(0xFFFF0000))
                        .clickable { showAccountSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = currentUser?.avatarUrl ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        coil.compose.AsyncImage(
                            model = avatarUrl,
                            contentDescription = "الملف الشخصي",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = currentUser?.name?.take(1)?.uppercase() ?: "G",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showAccountSheet) {
            com.example.ui.common.AccountBottomSheet(
                authManager = authManager,
                onDismiss = { showAccountSheet = false }
            )
        }

        // Category Chips Bar (YouTube style horizontal pill chips)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (key, label) ->
                val isSelected = (selectedCategory == key)
                AssistChip(
                    onClick = { viewModel.selectCategory(key) },
                    label = {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) Color(0xFF272727) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = null
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        // Main Video Feed + Shorts Shelf
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingState(message = "جارٍ الاتصال بيوتيوب وجلب أحدث الفيديوهات...")
                }
                is HomeUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadHomeData(selectedCategory) }
                    )
                }
                is HomeUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("youtube_home_feed"),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(state.videos, key = { _, v -> v.id }) { index, video ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) },
                                onChannelClick = {
                                    if (video.uploaderId.isNotEmpty()) onChannelClick(video.uploaderId)
                                }
                            )

                            // Insert Shorts Carousel after the 2nd video just like the YouTube app
                            if (index == 1 && state.shorts.isNotEmpty()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                YouTubeShortsRow(
                                    shorts = state.shorts,
                                    onShortClick = onVideoClick
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
