package com.ngi.streamnest.admin.ui.featured

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ngi.streamnest.admin.AdminViewModel
import com.ngi.streamnest.admin.data.FeaturedInput
import com.ngi.streamnest.admin.data.FeaturedRow
import com.ngi.streamnest.admin.ui.common.AdminField
import com.ngi.streamnest.admin.ui.common.AdminTopBar
import com.ngi.streamnest.admin.ui.common.ChoiceRow
import com.ngi.streamnest.admin.ui.common.ConfirmDialog
import com.ngi.streamnest.admin.ui.common.MutedText
import com.ngi.streamnest.admin.ui.common.RadioList

private val ACTION_OPTIONS = listOf(
    "url" to "বাইরের লিংক (বিজ্ঞাপন)",
    "channel" to "চ্যানেল",
    "match" to "ইভেন্ট",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedScreen(
    vm: AdminViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val featured by vm.featured.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = "ফিচার্ড ও বিজ্ঞাপন",
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "রিফ্রেশ")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "নতুন ফিচার্ড")
            }
        },
    ) { padding ->
        if (featured.isEmpty()) {
            MutedText(
                text = if (busy) "লোড হচ্ছে..." else "কোনো ফিচার্ড আইটেম নেই। + চাপুন।",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(featured, key = { it.id }) { item ->
                    FeaturedItemCard(
                        item = item,
                        onClick = { onOpen(item.id) },
                        onToggle = { vm.toggleFeatured(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedItemCard(item: FeaturedRow, onClick: () -> Unit, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(64.dp).padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "ধরন: ${actionLabel(item.actionType)} • ক্রম ${item.sortOrder}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = item.isActive, onCheckedChange = { onToggle() })
        }
    }
}

private fun actionLabel(type: String): String = when (type) {
    "url" -> "লিংক"
    "channel" -> "চ্যানেল"
    "match" -> "ইভেন্ট"
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedEditScreen(
    vm: AdminViewModel,
    id: String,
    onDone: () -> Unit,
) {
    val featured by vm.featured.collectAsStateWithLifecycle()
    val channels by vm.channels.collectAsStateWithLifecycle()
    val matches by vm.matches.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    val isNew = id == "new"
    val existing = featured.firstOrNull { it.id == id }

    var title by rememberSaveable(id) { mutableStateOf(existing?.title.orEmpty()) }
    var imageUrl by rememberSaveable(id) { mutableStateOf(existing?.imageUrl.orEmpty()) }
    var actionType by rememberSaveable(id) { mutableStateOf(existing?.actionType ?: "url") }
    var actionValue by rememberSaveable(id) { mutableStateOf(existing?.actionValue.orEmpty()) }
    var active by rememberSaveable(id) { mutableStateOf(existing?.isActive ?: true) }
    var sortText by rememberSaveable(id) { mutableStateOf((existing?.sortOrder ?: 0).toString()) }
    var formError by remember(id) { mutableStateOf<String?>(null) }
    var confirmDelete by remember(id) { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AdminTopBar(
                title = if (isNew) "নতুন ফিচার্ড" else "ফিচার্ড এডিট",
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
            AdminField(value = title, onValueChange = { title = it }, label = "শিরোনাম")
            AdminField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = "ছবির URL (ব্যানার)",
                keyboardType = KeyboardType.Uri,
            )
            if (imageUrl.startsWith("http")) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "ব্যানার প্রিভিউ",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("ক্লিক করলে কী হবে?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            ChoiceRow(
                options = ACTION_OPTIONS,
                selected = actionType,
                onSelect = {
                    if (it != actionType) actionValue = ""
                    actionType = it
                },
            )

            when (actionType) {
                "url" -> AdminField(
                    value = actionValue,
                    onValueChange = { actionValue = it },
                    label = "লিংক (https://...)",
                    keyboardType = KeyboardType.Uri,
                )
                "channel" -> {
                    Text("চ্যানেল বেছে নিন", style = MaterialTheme.typography.bodyMedium)
                    val options = channels.map { ch -> Pair<String?, String>(ch.id, ch.name) }
                    if (options.isEmpty()) MutedText("আগে চ্যানেল যোগ করুন")
                    RadioList(options = options, selected = actionValue, onSelect = { actionValue = it.orEmpty() })
                }
                "match" -> {
                    Text("ইভেন্ট বেছে নিন", style = MaterialTheme.typography.bodyMedium)
                    val options = matches.map { m -> Pair<String?, String>(m.id, "${m.title} (${m.status})") }
                    if (options.isEmpty()) MutedText("কোনো ইভেন্ট নেই")
                    RadioList(options = options, selected = actionValue, onSelect = { actionValue = it.orEmpty() })
                }
            }

            AdminField(
                value = sortText,
                onValueChange = { sortText = it.filter { c -> c.isDigit() } },
                label = "ক্রম",
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
                    val problem = when {
                        title.isBlank() -> "শিরোনাম লিখুন"
                        !imageUrl.trim().startsWith("http") -> "সঠিক ছবির URL দিন"
                        actionValue.isBlank() -> "ক্লিকের গন্তব্য ঠিক করুন"
                        else -> null
                    }
                    formError = problem
                    if (problem == null) {
                        vm.saveFeatured(
                            id = if (isNew) null else id,
                            input = FeaturedInput(
                                title = title.trim(),
                                imageUrl = imageUrl.trim(),
                                actionType = actionType,
                                actionValue = actionValue.trim(),
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
                    Text("আইটেম মুছুন", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "আইটেম মুছবেন?",
            text = "\"${existing?.title ?: title}\" মুছে যাবে।",
            onConfirm = {
                confirmDelete = false
                vm.deleteFeatured(id, onDone)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
