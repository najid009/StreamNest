package com.ngi.streamnest.admin.ui.settings

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.data.ApiSettingRow
import com.ngi.streamnest.admin.ui.common.AdminField
import com.ngi.streamnest.admin.ui.common.AdminTopBar
import com.ngi.streamnest.admin.ui.common.MutedText
import com.ngi.streamnest.admin.ui.common.SectionCard
import com.ngi.streamnest.admin.ui.common.formatDhaka

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(vm: AdminViewModel) {
    val settings by vm.apiSettings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    val cricket = settings["cricket"]
    val football = settings["football"]

    var cricketEnabled by remember(cricket) { mutableStateOf(cricket?.enabled == true) }
    var cricketKey by remember(cricket) { mutableStateOf(cricket?.apiKey.orEmpty()) }
    var footballEnabled by remember(football) { mutableStateOf(football?.enabled == true) }
    var footballKey by remember(football) { mutableStateOf(football?.apiKey.orEmpty()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AdminTopBar(title = "API ও অ্যাকাউন্ট") },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "ক্রিকেট ইভেন্ট (CricketData.org)") {
                ToggleRow("সিঙ্ক চালু", cricketEnabled) { cricketEnabled = it }
                AdminField(
                    value = cricketKey,
                    onValueChange = { cricketKey = it },
                    label = "API key",
                    password = true,
                )
                StatusLines(cricket)
                MutedText("Free plan: দিনে ১০০ hit। স্বয়ংক্রিয় সিঙ্ক প্রতি ৩০ মিনিটে।")
            }

            SectionCard(title = "ফুটবল ইভেন্ট (football-data.org)") {
                ToggleRow("সিঙ্ক চালু", footballEnabled) { footballEnabled = it }
                AdminField(
                    value = footballKey,
                    onValueChange = { footballKey = it },
                    label = "API key (X-Auth-Token)",
                    password = true,
                )
                StatusLines(football)
                MutedText("Free plan: মিনিটে ১০ call, ১২টি লিগ। স্কোর কিছুটা দেরিতে আসতে পারে।")
            }

            Button(
                onClick = {
                    vm.saveApiSettings(
                        cricketEnabled = cricketEnabled,
                        cricketKey = cricketKey,
                        footballEnabled = footballEnabled,
                        footballKey = footballKey,
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("API সেটিংস সংরক্ষণ করুন")
            }

            OutlinedButton(
                onClick = { vm.syncNow() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("এখনই ইভেন্ট সিঙ্ক করুন")
            }

            SectionCard(title = "অ্যাকাউন্ট") {
                Text(
                    text = "লগইন: ${session?.email.orEmpty()}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text("সাইন আউট")
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusLines(row: ApiSettingRow?) {
    Text(
        text = "শেষ সফল সিঙ্ক: ${formatDhaka(row?.lastSyncedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!row?.lastError.isNullOrBlank()) {
        Text(
            text = "সর্বশেষ ত্রুটি: ${row?.lastError}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
