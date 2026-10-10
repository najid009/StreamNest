package com.ngi.streamnest.admin.ui.channels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.data.ChannelInput
import com.ngi.streamnest.admin.data.ChannelRow
import com.ngi.streamnest.admin.ui.common.AdminField
import com.ngi.streamnest.admin.ui.common.AdminTopBar
import com.ngi.streamnest.admin.ui.common.CATEGORY_OPTIONS
import com.ngi.streamnest.admin.ui.common.ChoiceRow
import com.ngi.streamnest.admin.ui.common.ConfirmDialog
import com.ngi.streamnest.admin.ui.common.MutedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    vm: AdminViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val channels by vm.channels.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = "চ্যানেল",
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন চ্যানেল")
            }
        },
    ) { padding ->
        if (channels.isEmpty()) {
            MutedText(
                text = if (busy) "লোড হচ্ছে..." else "কোনো চ্যানেল নেই। + চাপুন।",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = { onOpen(channel.id) },
                        onToggle = { vm.toggleChannel(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelItem(channel: ChannelRow, onClick: () -> Unit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${channel.category} • ক্রম ${channel.sortOrder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = channel.streamUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(checked = channel.isActive, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelEditScreen(
    vm: AdminViewModel,
    id: String,
    onDone: () -> Unit,
) {
    val channels by vm.channels.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val isNew = id == "new"
    val existing = channels.firstOrNull { it.id == id }

    var name by rememberSaveable(id) { mutableStateOf(existing?.name.orEmpty()) }
    var streamUrl by rememberSaveable(id) { mutableStateOf(existing?.streamUrl.orEmpty()) }
    var logoUrl by rememberSaveable(id) { mutableStateOf(existing?.logoUrl.orEmpty()) }
    var category by rememberSaveable(id) { mutableStateOf(existing?.category ?: "sports") }
    var active by rememberSaveable(id) { mutableStateOf(existing?.isActive ?: true) }
    var sortText by rememberSaveable(id) { mutableStateOf((existing?.sortOrder ?: 0).toString()) }
    var formError by remember(id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(id) { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = if (isNew) "নতুন চ্যানেল" else "চ্যানেল এডিট",
                onBack = onDone,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AdminField(value = name, onValueChange = { name = it }, label = "চ্যানেলের নাম")
            AdminField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = "স্ট্রিম URL (https://...index.m3u8)",
                keyboardType = KeyboardType.Uri,
            )
            AdminField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = "লোগো URL (ঐচ্ছিক)",
                keyboardType = KeyboardType.Uri,
            )

            Spacer(Modifier.height(8.dp))
            Text("ক্যাটাগরি", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ChoiceRow(options = CATEGORY_OPTIONS, selected = category, onSelect = { category = it })

            AdminField(
                value = sortText,
                onValueChange = { sortText = it.filter { c -> c.isDigit() } },
                label = "ক্রম (ছোট সংখ্যা আগে আসবে)",
                keyboardType = KeyboardType.Number,
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("সক্রিয় (অ্যাপে দেখাবে)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = active, onCheckedChange = { active = it })
            }

            formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val url = streamUrl.trim()
                    val problem = when {
                        name.isBlank() -> "চ্যানেলের নাম লিখুন"
                        !url.startsWith("https://") && !url.startsWith("http://") -> "সঠিক স্ট্রিম URL দিন (http/https)"
                        else -> null
                    }
                    formError = problem
                    if (problem == null) {
                        vm.saveChannel(
                            id = if (isNew) null else id,
                            input = ChannelInput(
                                name = name.trim(),
                                logoUrl = logoUrl.trim().ifBlank { null },
                                streamUrl = url,
                                category = category,
                                isActive = active,
                                sortOrder = sortText.toIntOrNull() ?: 0,
                            ),
                            onDone = onDone,
                        )
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("সংরক্ষণ করুন")
            }

            if (!isNew) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("চ্যানেল মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "চ্যানেল মুছবেন?",
            text = "\"${existing?.name ?: name}\" স্থায়ীভাবে মুছে যাবে।",
            onConfirm = {
                confirmDelete = false
                vm.deleteChannel(id, onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
