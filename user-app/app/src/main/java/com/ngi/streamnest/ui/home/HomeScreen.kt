package com.ngi.streamnest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ngi.streamnest.HomeUiState
import com.ngi.streamnest.data.Channel
import com.ngi.streamnest.data.FeaturedItem
import com.ngi.streamnest.data.MatchItem
import com.ngi.streamnest.data.channelById
import com.ngi.streamnest.ui.common.ChannelCard
import com.ngi.streamnest.ui.common.InfoText
import com.ngi.streamnest.ui.common.MatchCard
import com.ngi.streamnest.ui.common.SectionTitle
import com.ngi.streamnest.ui.common.SkeletonBox
import com.ngi.streamnest.ui.common.WarningText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onFeatured: (FeaturedItem) -> Unit,
    onChannel: (Channel) -> Unit,
    onMatch: (MatchItem) -> Unit,
) {
    val data = state.data
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "StreamNest",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading && data != null,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                data?.error?.let { message ->
                    item { WarningText(message) }
                }

                if (data == null) {
                    item { SkeletonSection() }
                }

                if (data != null) {
                    // ১. ফিচার্ড: বিজ্ঞাপন (বাইরের লিংক) বা অ্যাপের চ্যানেল/ইভেন্ট
                    if (data.featured.isNotEmpty()) {
                        item { SectionTitle("ফিচার্ড") }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(data.featured, key = { it.id }) { featured ->
                                    FeaturedCard(featured) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFeatured(featured)
                                    }
                                }
                            }
                        }
                    }

                    // ২. ইভেন্ট
                    item { SectionTitle("ইভেন্ট") }
                    if (data.matches.isEmpty()) {
                        item { InfoText("এখন কোনো ইভেন্ট নেই") }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(data.matches.take(10), key = { it.id }) { match ->
                                    MatchCard(
                                        match = match,
                                        channelName = data.channelById(match.channelId)?.name,
                                        onClick = { onMatch(match) },
                                        modifier = Modifier.width(280.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ৩. লাইভ চ্যানেল
                    item { SectionTitle("লাইভ চ্যানেল") }
                    if (data.channels.isEmpty()) {
                        item { InfoText("এখন কোনো চ্যানেল নেই") }
                    } else {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(data.channels, key = { it.id }) { channel ->
                                    ChannelCard(
                                        channel = channel,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onChannel(channel)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // ৪. কনটেন্ট (আপাতত placeholder)
                    item { SectionTitle("কনটেন্ট") }
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            InfoText("শীঘ্রই আসছে")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        SkeletonBox(Modifier.fillMaxWidth().height(170.dp))
        Spacer16()
        SkeletonBox(Modifier.fillMaxWidth().height(24.dp))
        Spacer16()
        Row16 {
            SkeletonBox(Modifier.width(140.dp).height(130.dp))
            SkeletonBox(Modifier.width(140.dp).height(130.dp))
        }
    }
}

@Composable
private fun Spacer16() = Box(Modifier.height(16.dp))

@Composable
private fun Row16(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun FeaturedCard(item: FeaturedItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(300.dp).height(170.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC000000)),
                        ),
                    ),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            )
        }
    }
}
