package app.opentv.ui.vod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.opentv.R

/** A single genre selector, never a provider selector, for the merged cloud catalogue. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnifiedGenreBrowseControls(
    categories: List<UnifiedGenre>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    val title = categories.firstOrNull { it.id == selectedId }?.title
        ?: stringResource(R.string.cloud_categories)
    FilterChip(
        selected = selectedId != null,
        onClick = { filter = ""; open = true },
        label = { Text(title) },
        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    if (!open) return
    ModalBottomSheet(
        onDismissRequest = { open = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.cloud_filter_categories)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val shown = categories.filter { it.title.contains(filter.trim(), ignoreCase = true) }
            LazyColumn {
                item(key = "all") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.cloud_home)) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            open = false
                            onSelect(null)
                        },
                    )
                }
                items(shown, key = { it.id }) { category ->
                    ListItem(
                        headlineContent = { Text(category.title) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            open = false
                            onSelect(category.id)
                        },
                    )
                }
            }
        }
    }
}
