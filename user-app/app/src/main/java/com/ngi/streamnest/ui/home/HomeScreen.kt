package com.ngi.streamnest.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ngi.streamnest.HomeUiState
import com.ngi.streamnest.data.Channel
import com.ngi.streamnest.data.FeaturedItem
import com.ngi.streamnest.data.HomeData
import com.ngi.streamnest.data.MatchItem
import com.ngi.streamnest.data.channelById
import com.ngi.streamnest.ui.common.ChannelCard
import com.ngi.streamnest.ui.common.InfoText
import com.ngi.streamnest.ui.common.MatchCard
import com.ngi.streamnest.ui.common.SectionTitle
import com.ngi.streamnest.ui.common.WarningText

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onFeatured: (FeaturedItem) -> Unit,
    onChannel: (Channel) -> Unit,
    onMatch: (MatchItem) -> Unit,
) {
    val data: HomeData? = state.data
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "StreamNest",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "লাইভ চ্যানেল ও স্পোর্টস ইভেন্ট",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                }
            }
        }

        data?.error?.let { message ->
            item { WarningText(message) }
        }

        if (data == null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
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
                            FeaturedCard(featured) { onFeatured(featured) }
                        }
                    }
                }
            }

            // ২. ইভেন্ট (হোম-এ সংক্ষিপ্ত)
            if (data.matches.isNotEmpty()) {
                item { SectionTitle("ইভেন্ট") }
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

            // ৩. চ্যানেল
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
                            ChannelCard(channel = channel, onClick = { onChannel(channel) })
                        }
                    }
                }
            }

            // ৪. কনটেন্ট (আপাতত placeholder)
            item { SectionTitle("কনটেন্ট") }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    InfoText("শীঘ্রই আসছে")
                }
            }
        }
    }
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
