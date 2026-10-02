/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.opentv.R
import app.opentv.core.ServiceLocator

/**
 * Management surface for native Cloud Providers.
 *
 * Provider code stays independent of Compose; this screen only reads the host registry. Repository
 * install/update controls will be added when the remote provider source is implemented.
 */
@Composable
fun CloudProvidersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val providers = remember(context) {
        ServiceLocator.get(context).providerRepository.providers().sortedBy { it.name.lowercase() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .screenPadding(),
    ) {
        ScreenHeader(stringResource(R.string.settings_addons_title), onBack = onBack)

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.cloud_providers_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.cloud_providers_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(10.dp))
        if (providers.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.cloud_providers_empty),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.cloud_providers_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(providers, key = { it.id }) { provider ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                provider.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                provider.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
