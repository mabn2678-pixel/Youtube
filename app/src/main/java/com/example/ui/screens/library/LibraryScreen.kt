package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.entities.DownloadEntity
import com.example.data.local.entities.PlaylistEntity
import com.example.data.local.entities.PlaylistItemEntity
import com.example.data.local.entities.WatchHistoryEntity
import com.example.data.model.DownloadStatus
import com.example.ui.common.EmptyState
import com.example.ui.common.FormatUtils

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    authManager: com.example.data.auth.AuthManager? = null,
    onVideoClick: (String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val downloadList by viewModel.downloadList.collectAsState()
    val playlistList by viewModel.playlistList.collectAsState()
    val viewingPlaylist by viewModel.viewingPlaylist.collectAsState()
    val selectedPlaylistItems by viewModel.selectedPlaylistItems.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var showAccountSheet by remember { mutableStateOf(false) }

    val user = authManager?.currentUser?.collectAsState()?.value

    val tabTitles = listOf(
        stringResource(R.string.history),
        stringResource(R.string.downloads),
        stringResource(R.string.playlists)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_library),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Google Account Sync Icon
            if (authManager != null) {
                IconButton(onClick = { showAccountSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "حساب Google",
                        tint = if (user != null) Color(0xFF2BA640) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Contextual action for history or playlist
            if (selectedTab == 0 && historyList.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllHistory() }) {
                    Text(stringResource(R.string.clear_history), color = MaterialTheme.colorScheme.primary)
                }
            } else if (selectedTab == 2 && viewingPlaylist == null) {
                IconButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "قائمة جديدة", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Tabs
        if (viewingPlaylist == null) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = (selectedTab == index),
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // If viewing a single playlist
                viewingPlaylist != null -> {
                    PlaylistDetailView(
                        playlist = viewingPlaylist!!,
                        items = selectedPlaylistItems,
                        onBack = { viewModel.closePlaylistView() },
                        onVideoClick = onVideoClick,
                        onRemoveItem = { videoId ->
                            viewModel.removePlaylistItem(viewingPlaylist!!.id, videoId)
                        },
                        onDeletePlaylist = {
                            viewModel.deletePlaylist(viewingPlaylist!!.id)
                        }
                    )
                }

                // 0: Watch History
                selectedTab == 0 -> {
                    if (historyList.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.no_history),
                            subtitle = "الفيديوهات التي تشاهدها ستظهر هنا مع تذكر نقطة التوقف بدقة",
                            icon = Icons.Default.History
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(historyList, key = { it.videoId }) { item ->
                                HistoryItemCard(
                                    item = item,
                                    onClick = { onVideoClick(item.videoId) },
                                    onDelete = { viewModel.removeHistoryItem(item.videoId) }
                                )
                            }
                        }
                    }
                }

                // 1: Downloads
                selectedTab == 1 -> {
                    if (downloadList.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.no_downloads),
                            subtitle = "يمكنك تحميل أي فيديو أو استخراج الصوت منه للمشاهدة بدون إنترنت أثناء السفر",
                            icon = Icons.Default.FileDownload
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(downloadList, key = { it.id }) { download ->
                                DownloadItemCard(
                                    download = download,
                                    onPlay = {
                                        viewModel.playOfflineDownload(download)
                                        onVideoClick(download.videoId)
                                    },
                                    onDelete = { viewModel.deleteDownload(download) }
                                )
                            }
                        }
                    }
                }

                // 2: Playlists
                selectedTab == 2 -> {
                    if (playlistList.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.no_playlists),
                            subtitle = "قم بإنشاء قوائم تشغيل محلية مخصصة لتنظيم المحتوى المفضل بدون حساب",
                            icon = Icons.Default.PlaylistPlay
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(playlistList, key = { it.id }) { playlist ->
                                PlaylistItemRow(
                                    playlist = playlist,
                                    onClick = { viewModel.openPlaylist(playlist) },
                                    onDelete = { viewModel.deletePlaylist(playlist.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialog for creating new playlist
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text(stringResource(R.string.create_playlist)) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistTitle,
                        onValueChange = { newPlaylistTitle = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistTitle.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistTitle.trim())
                                newPlaylistTitle = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    ) {
                        Text("إنشاء")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showAccountSheet && authManager != null) {
            com.example.ui.common.AccountBottomSheet(
                authManager = authManager,
                onDismiss = { showAccountSheet = false }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with progress bar
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Progress line at bottom of thumbnail
                if (item.durationSeconds > 0) {
                    val progressRatio = (item.playbackPositionMs.toFloat() / (item.durationSeconds * 1000f)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.channelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.playbackPositionMs > 0) {
                    Text(
                        text = "تم التوقف عند ${FormatUtils.formatDurationMs(item.playbackPositionMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadEntity,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (download.isAudioOnly) Icons.Default.Audiotrack else Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${download.channelTitle} • ${download.quality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_download),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${download.progress}% - ${stringResource(R.string.downloading)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (download.status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "جاهز للتشغيل بلا إنترنت",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = playlist.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "قائمة تشغيل محلية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف القائمة", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: PlaylistEntity,
    items: List<PlaylistItemEntity>,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onDeletePlaylist: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = playlist.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(text = "${items.size} مقاطع فيديو", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDeletePlaylist) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف القائمة", tint = MaterialTheme.colorScheme.error)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        if (items.isEmpty()) {
            EmptyState(
                title = "القائمة فارغة",
                subtitle = "أضف مقاطع فيديو إلى هذه القائمة من خلال زر الحفظ في شاشة المشغل"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.videoId }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVideoClick(item.videoId) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.channelTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onRemoveItem(item.videoId) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
