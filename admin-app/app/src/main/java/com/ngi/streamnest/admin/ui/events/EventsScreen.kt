package com.ngi.streamnest.admin.ui.events

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.data.CustomEventInput
import com.ngi.streamnest.admin.ui.common.AdminField
import com.ngi.streamnest.admin.ui.common.AdminTopBar
import com.ngi.streamnest.admin.ui.common.ChoiceRow
import com.ngi.streamnest.admin.ui.common.ConfirmDialog
import com.ngi.streamnest.admin.ui.common.MutedText
import com.ngi.streamnest.admin.ui.common.RadioList
import com.ngi.streamnest.admin.ui.common.STATUS_OPTIONS
import com.ngi.streamnest.admin.ui.common.formatDhaka
import com.ngi.streamnest.admin.ui.common.sportLabel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val SPORT_OPTIONS = listOf(
    "football" to "ফুটবল",
    "cricket" to "ক্রিকেট",
)

/** "2026-10-12 20:30" (Dhaka time) -> ISO with +06:00 */
private fun parseDhakaTime(text: String): String? = runCatching {
    LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        .atOffset(ZoneOffset.ofHours(6))
        .toString()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    vm: AdminViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val channels by vm.channels.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = "ইভেন্ট",
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন ইভেন্ট")
            }
        },
    ) { padding ->
        if (matches.isEmpty()) {
            MutedText(
                text = if (busy) "লোড হচ্ছে..." else "কোনো ইভেন্ট নেই। + চাপুন বা API সিঙ্ক করুন।",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(matches, key = { it.id }) { match ->
                    val channelName = channels.firstOrNull { it.id == match.channelId }?.name
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(match.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sportLabel(match.sport),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = match.status.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = match.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${formatDhaka(match.startTime)}  •  ${match.score ?: "স্কোর নেই"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "চ্যানেল: ${channelName ?: "সেট করা হয়নি"}" +
                                    if (match.externalId == null) "  • কাস্টম" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (channelName == null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    vm: AdminViewModel,
    id: String,
    onDone: () -> Unit,
) {
    val matches by vm.matches.collectAsStateWithLifecycle()
    val channels by vm.channels.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    val isNew = id == "new"
    val existing = matches.firstOrNull { it.id == id }
    val isCustom = isNew || existing?.externalId == null

    var sport by rememberSaveable(id) { mutableStateOf(existing?.sport ?: "football") }
    var title by rememberSaveable(id) { mutableStateOf(existing?.title.orEmpty()) }
    var league by rememberSaveable(id) { mutableStateOf(existing?.league.orEmpty()) }
    var startText by rememberSaveable(id) { mutableStateOf("") }
    var status by rememberSaveable(id) { mutableStateOf(existing?.status ?: "upcoming") }
    var score by rememberSaveable(id) { mutableStateOf(existing?.score.orEmpty()) }
    var channelId by rememberSaveable(id) { mutableStateOf(existing?.channelId) }
    var formError by remember(id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(id) { mutableStateOf(false) }

    val channelOptions = buildList<Pair<String?, String>> {
        add(null to "কোনো চ্যানেল নেই")
        channels.forEach { add(it.id to it.name) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = if (isNew) "নতুন ইভেন্ট" else "ইভেন্ট এডিট",
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
            if (isCustom) {
                if (isNew) {
                    Text("খেলা", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    ChoiceRow(options = SPORT_OPTIONS, selected = sport, onSelect = { sport = it })
                    AdminField(value = title, onValueChange = { title = it }, label = "ইভেন্টের নাম (যেমন: Team A vs Team B)")
                    AdminField(value = league, onValueChange = { league = it }, label = "লিগ/সিরিজ (ঐচ্ছিক)")
                    AdminField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = "শুরুর সময় (yyyy-MM-dd HH:mm, ঢাকা সময়)",
                        keyboardType = KeyboardType.Number,
                    )
                } else {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    MutedText("কাস্টম ইভেন্ট • ${sportLabel(sport)} • ${formatDhaka(existing?.startTime)}")
                }
            } else {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                MutedText("${sportLabel(sport)} • ${league.ifBlank { "লিগ নেই" }} • ${formatDhaka(existing?.startTime)}")
                MutedText("স্বয়ংক্রিয় সিঙ্ক থেকে এসেছে। এখানে শুধু চ্যানেল, অবস্থা ও স্কোর বদলানো যাবে।")
            }

            Spacer(Modifier.height(8.dp))
            Text("চ্যানেল", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            RadioList(options = channelOptions, selected = channelId, onSelect = { channelId = it })

            Spacer(Modifier.height(8.dp))
            Text("অবস্থা", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ChoiceRow(options = STATUS_OPTIONS, selected = status, onSelect = { status = it })

            AdminField(
                value = score,
                onValueChange = { score = it },
                label = "স্কোর (ঐচ্ছিক, যেমন: 2 - 1)",
            )

            formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (isNew) {
                        val problem = when {
                            title.isBlank() -> "ইভেন্টের নাম লিখুন"
                            startText.isNotBlank() && parseDhakaTime(startText) == null ->
                                "সময়ের ফরম্যাট ঠিক নেই (yyyy-MM-dd HH:mm)"
                            else -> null
                        }
                        formError = problem
                        if (problem == null) {
                            vm.createEvent(
                                CustomEventInput(
                                    sport = sport,
                                    title = title.trim(),
                                    league = league.trim().ifBlank { null },
                                    startTime = if (startText.isBlank()) null else parseDhakaTime(startText),
                                    status = status,
                                    score = score.trim().ifBlank { null },
                                    channelId = channelId,
                                ),
                                onDone = onDone,
                            )
                        }
                    } else if (existing != null) {
                        vm.updateEvent(
                            match = existing,
                            channelId = channelId,
                            status = status,
                            score = score.trim().ifBlank { null },
                            onDone = onDone,
                        )
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("সংরক্ষণ করুন")
            }

            if (!isNew && isCustom) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("ইভেন্ট মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "ইভেন্ট মুছবেন?",
            text = "\"$title\" মুছে যাবে।",
            onConfirm = {
                confirmDelete = false
                vm.deleteEvent(id, onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

