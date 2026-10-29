package com.example.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.common.EmptyState
import com.example.ui.common.LoadingState
import com.example.ui.common.VideoCard

@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onVideoClick: (String) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val feedVideos by viewModel.feedVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Screen Title
        Text(
            text = stringResource(R.string.nav_subscriptions),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        )

        if (subscriptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_subscriptions),
                subtitle = "يمكنك متابعة قنواتك المفضلة بالنقر على زر 'اشتراك' في صفحة المشغل بدون الحاجة لتسجيل أي حساب!",
                icon = Icons.Default.Subscriptions,
                modifier = Modifier.padding(top = 40.dp)
            )
        } else {
            // Horizontal list of subscribed channels
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(subscriptions, key = { it.channelId }) { sub ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onChannelClick(sub.channelId) }
                            .padding(vertical = 4.dp)
                    ) {
                        AsyncImage(
                            model = sub.avatarUrl,
                            contentDescription = sub.channelName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = sub.channelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.size(68.dp, 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subscriptions Videos Feed
            if (isLoading) {
                LoadingState(message = "جارٍ تحديث خلاصة القنوات...")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(feedVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onVideoClick(video.id) },
                            onChannelClick = {
                                if (video.uploaderId.isNotEmpty()) onChannelClick(video.uploaderId)
                            }
                        )
                    }
                }
            }
        }
    }
}
