package com.example.ui.screens.channel

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.common.ErrorState
import com.example.ui.common.LoadingState
import com.example.ui.common.VideoCard

@Composable
fun ChannelDetailScreen(
    viewModel: ChannelViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is ChannelUiState.Loading -> {
                LoadingState(message = "جارٍ تحميل معلومات القناة...")
            }
            is ChannelUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadChannel() }
                )
            }
            is ChannelUiState.Success -> {
                val channel = state.channel

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        // Channel Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (channel.bannerUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = channel.bannerUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Back Button
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopStart)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                        }

                        // Channel Profile Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = channel.avatarUrl,
                                contentDescription = channel.name,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = channel.subscriberCountText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subscribe Button
                            Button(
                                onClick = { viewModel.toggleSubscription() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (channel.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = channel.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = "مقاطع الفيديو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    items(channel.videos, key = { it.id }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
