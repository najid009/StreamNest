package com.ngi.streamnest.admin.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.ui.common.AdminTopBar
import com.ngi.streamnest.admin.ui.common.MutedText
import com.ngi.streamnest.admin.ui.common.SectionCard
import com.ngi.streamnest.admin.ui.common.formatDhaka

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: AdminViewModel,
    onNavigate: (String) -> Unit,
) {
    val channels by vm.channels.collectAsStateWithLifecycle()
    val featured by vm.featured.collectAsStateWithLifecycle()
    val matches by vm.matches.collectAsStateWithLifecycle()
    val settings by vm.apiSettings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    val liveCount = matches.count { it.status == "live" }
    val upcomingCount = matches.count { it.status == "upcoming" }
    val activeChannels = channels.count { it.isActive }
    val activeFeatured = featured.count { it.isActive }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = "ড্যাশবোর্ড",
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("চ্যানেল (সক্রিয়)", "$activeChannels / ${channels.size}", Modifier.weight(1f))
                StatCard("ফিচার্ড (সক্রিয়)", "$activeFeatured / ${featured.size}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("লাইভ ইভেন্ট", liveCount.toString(), Modifier.weight(1f))
                StatCard("আসছে ইভেন্ট", upcomingCount.toString(), Modifier.weight(1f))
            }

            SectionCard(title = "ইভেন্ট সিঙ্ক") {
                val cricket = settings["cricket"]
                val football = settings["football"]
                SyncLine("ক্রিকেট", enabled = cricket?.enabled == true, lastSync = cricket?.lastSyncedAt, error = cricket?.lastError)
                Spacer(Modifier.height(6.dp))
                SyncLine("ফুটবল", enabled = football?.enabled == true, lastSync = football?.lastSyncedAt, error = football?.lastError)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.syncNow() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("এখনই সিঙ্ক করুন")
                }
                MutedText("স্বয়ংক্রিয় সিঙ্ক প্রতি ৩০ মিনিটে চলে (free API limit মেনে)।")
            }

            SectionCard(title = "দ্রুত কাজ") {
                OutlinedButton(onClick = { onNavigate("channel/new") }, modifier = Modifier.fillMaxWidth()) {
                    Text("নতুন চ্যানেল যোগ করুন")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onNavigate("featured/new") }, modifier = Modifier.fillMaxWidth()) {
                    Text("নতুন ফিচার্ড / বিজ্ঞাপন যোগ করুন")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onNavigate("event/new") }, modifier = Modifier.fillMaxWidth()) {
                    Text("নতুন ইভেন্ট যোগ করুন")
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncLine(label: String, enabled: Boolean, lastSync: String?, error: String?) {
    Column {
        Text(
            text = "$label: ${if (enabled) "চালু" else "বন্ধ"}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "শেষ সিঙ্ক: ${formatDhaka(lastSync)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!error.isNullOrBlank()) {
            Text(
                text = "ত্রুটি: $error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
