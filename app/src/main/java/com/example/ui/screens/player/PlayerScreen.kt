@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.media3.common.util.UnstableApi::class
)

package com.example.ui.screens.player

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbDownOffAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.common.AddToPlaylistDialog
import com.example.ui.common.DownloadOptionsBottomSheet
import com.example.ui.common.ErrorState
import com.example.ui.common.FormatUtils
import com.example.ui.common.LoadingState
import com.example.ui.common.VideoCard

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onEnterPiP: () -> Unit,
    isInPiPMode: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isBackgroundPlaying by viewModel.isBackgroundPlaying.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(14200) }

    var showDownloadSheet by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Video Player Viewport (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            when (val state = uiState) {
                is PlayerUiState.Success -> {
                    val videoId = state.detail.item.id
                    // Real Internet YouTube Web Player with zero scraping errors
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&enablejsapi=1&fs=1&playsinline=1&modestbranding=1&rel=0"
                                loadUrl(embedUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is PlayerUiState.Loading -> {
                    LoadingState(message = "جارٍ تشغيل الفيديو من يوتيوب...")
                }
                is PlayerUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadVideoDetails() }
                    )
                }
            }

            // Top collapse button (YouTube style Arrow Down)
            if (!isInPiPMode) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تصغير المشغل",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (isInPiPMode) return

        // YouTube Content Body
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is PlayerUiState.Success -> {
                    val detail = state.detail
                    val item = detail.item

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            // Title & Description Expander
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val viewsAndDate = buildString {
                                    if (item.views > 0) append("${FormatUtils.formatViews(item.views)} مشاهدة • ")
                                    append(item.uploadedDate.ifBlank { "مؤخراً" })
                                    append(" ...المزيد")
                                }

                                Text(
                                    text = viewsAndDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isDescriptionExpanded && item.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }

                            // YouTube Channel Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (item.uploaderId.isNotEmpty()) onChannelClick(item.uploaderId)
                                        }
                                ) {
                                    AsyncImage(
                                        model = item.uploaderAvatarUrl,
                                        contentDescription = item.uploaderName,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.uploaderName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = item.subscriberCount.ifEmpty { "1.2M مشترك" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // YouTube Subscribe Button (Black/White or Red)
                                Button(
                                    onClick = { viewModel.toggleSubscription() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFF0000),
                                        contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurface else Color.White
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    if (isSubscribed) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.subscribed), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(stringResource(R.string.subscribe), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // YouTube Horizontal Action Pills Carousel
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Like & Dislike pill group
                                item {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable {
                                                    isLiked = !isLiked
                                                    if (isLiked) {
                                                        isDisliked = false
                                                        likeCount++
                                                    } else {
                                                        likeCount--
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                                contentDescription = "إعجاب",
                                                tint = if (isLiked) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = FormatUtils.formatViews(likeCount.toLong()),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(18.dp)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                        )

                                        IconButton(
                                            onClick = {
                                                isDisliked = !isDisliked
                                                if (isDisliked && isLiked) {
                                                    isLiked = false
                                                    likeCount--
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                                contentDescription = "لم يعجبني",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Share pill
                                item {
                                    YouTubeActionPill(
                                        icon = Icons.Default.Share,
                                        label = "مشاركة",
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "${item.title} https://youtu.be/${item.id}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفيديو"))
                                        }
                                    )
                                }

                                // Download pill
                                item {
                                    YouTubeActionPill(
                                        icon = Icons.Default.FileDownload,
                                        label = "تنزيل",
                                        onClick = { showDownloadSheet = true }
                                    )
                                }

                                // Save to playlist pill
                                item {
                                    YouTubeActionPill(
                                        icon = Icons.Default.BookmarkBorder,
                                        label = "حفظ",
                                        onClick = { showPlaylistDialog = true }
                                    )
                                }

                                // Background Play pill
                                item {
                                    YouTubeActionPill(
                                        icon = Icons.Default.Headphones,
                                        label = "خلفية",
                                        isActive = isBackgroundPlaying,
                                        onClick = { viewModel.toggleBackgroundPlayback() }
                                    )
                                }

                                // Picture-in-Picture pill
                                item {
                                    YouTubeActionPill(
                                        icon = Icons.Default.PictureInPictureAlt,
                                        label = "نافذة عائمة",
                                        onClick = onEnterPiP
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // YouTube Comments Preview Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clickable { showCommentsSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "التعليقات • 1.4K",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("ع", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "محتوى رائع جداً ومفيد! شكراً على التوضيح والتبسيط.",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Text(
                                text = "فيديوهات مقترحة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }

                        // Related Videos list (real YouTube videos)
                        items(detail.relatedVideos, key = { it.id }) { related ->
                            VideoCard(
                                video = related,
                                onClick = { onVideoClick(related.id) }
                            )
                        }
                    }

                    // Comments Modal BottomSheet
                    if (showCommentsSheet) {
                        CommentsBottomSheet(onDismiss = { showCommentsSheet = false })
                    }

                    // Download sheet
                    if (showDownloadSheet) {
                        DownloadOptionsBottomSheet(
                            videoStreams = detail.videoStreams,
                            audioStreams = detail.audioStreams,
                            onDismiss = { showDownloadSheet = false },
                            onDownloadSelected = { url, quality, isAudio ->
                                viewModel.downloadMedia(url, quality, isAudio)
                            }
                        )
                    }

                    // Playlist selection
                    if (showPlaylistDialog) {
                        AddToPlaylistDialog(
                            playlists = playlists,
                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                            onAddToPlaylist = { viewModel.addToPlaylist(it) },
                            onDismiss = { showPlaylistDialog = false }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun YouTubeActionPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) Color(0xFFFF0000) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CommentsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newCommentText by remember { mutableStateOf("") }
    val comments = remember {
        listOf(
            "علي الأحمد" to "شرح ممتاز جداً ومفيد! استمر يا بطل.",
            "م. سارة المهدي" to "أفضل قناة تقدم هذه المواضيع باحترافية عالية.",
            "خالد التميمي" to "شكراً جزيلاً، وفرت علي ساعات من البحث والتجربة.",
            "فهد العتيبي" to "الفيديو 10/10 وضوح وصوت ومونتاج متقن.",
            "سلطان الحربي" to "يا ليت تنزل جزء ثاني عن هذا الموضوع قريباً."
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "التعليقات (1,420)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(comments) { (author, comment) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(author.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Add comment input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("إضافة تعليق علني...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { newCommentText = "" },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                ) {
                    Text("نشر", fontSize = 12.sp)
                }
            }
        }
    }
}
