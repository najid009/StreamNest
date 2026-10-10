package com.ngi.streamnest.ui.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ngi.streamnest.HomeUiState
import com.ngi.streamnest.data.Channel
import com.ngi.streamnest.data.MatchItem
import com.ngi.streamnest.data.channelById
import com.ngi.streamnest.ui.common.InfoText
import com.ngi.streamnest.ui.common.MatchCard
import com.ngi.streamnest.ui.common.SectionTitle
import com.ngi.streamnest.ui.common.SkeletonBox

private val sportFilters = listOf(
    "all" to "সব",
    "football" to "ফুটবল",
    "cricket" to "ক্রিকেট",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onMatch: (MatchItem) -> Unit,
    onChannel: (Channel) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("all") }
    val data = state.data
    val haptic = LocalHapticFeedback.current

    val matches = data?.matches.orEmpty().filter { filter == "all" || it.sport == filter }
    val sportsChannels = data?.channels.orEmpty().filter { it.category == "sports" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "স্পোর্টস",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sportFilters.forEach { (key, label) ->
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                item { SectionTitle("ইভেন্ট আপডেট") }

                when {
                    data == null -> items(3) {
                        SkeletonBox(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(120.dp),
                        )
                    }
                    matches.isEmpty() -> item { InfoText("এখন কোনো ইভেন্ট নেই") }
                    else -> items(matches, key = { it.id }) { match ->
                        MatchCard(
                            match = match,
                            channelName = data?.channelById(match.channelId)?.name,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMatch(match)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }

                item { SectionTitle("স্পোর্টস চ্যানেল") }

                if (data != null && sportsChannels.isEmpty()) {
                    item { InfoText("কোনো স্পোর্টস চ্যানেল যুক্ত নেই") }
                } else {
                    items(sportsChannels, key = { it.id }) { channel ->
                        ChannelRow(
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
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = channel.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "▶ দেখুন",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

