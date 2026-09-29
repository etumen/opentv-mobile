/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import app.opentv.ui.LayoutClass
import app.opentv.ui.LocalLayoutClass
import app.opentv.ui.settings.screenPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.Channel
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.shownName
import app.opentv.data.parser.displayTitle
import app.opentv.data.parser.sourceTag
import app.opentv.ui.ChannelsViewModel
import app.opentv.ui.VodViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Channel search with an on-screen keyboard.
 *
 * Android TV's default text field punts to "type on your phone", which is useless if your phone
 * isn't to hand. This screen draws its own d-pad keyboard so search works with the remote alone,
 * and shows matching channels live as you type.
 */
@Composable
fun SearchScreen(
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onBack: () -> Unit,
    initialScope: String = "all",
    /** Phones open the film's page (play, download, favourite) instead of playing straight away. */
    onOpenMovie: (Movie) -> Unit = onPlayMovie,
    viewModel: ChannelsViewModel = viewModel(),
    vodViewModel: VodViewModel = viewModel(),
) {
    if (LocalLayoutClass.current == LayoutClass.PHONE) {
        PhoneSearchScreen(onPlayChannel, onOpenMovie, onOpenSeries, onBack, initialScope, viewModel, vodViewModel)
        return
    }
    var query by remember { mutableStateOf("") }
    val channelResults by viewModel.searchResults.collectAsState()
    val movieResults by vodViewModel.movieResults.collectAsState()
    val seriesResults by vodViewModel.seriesResults.collectAsState()
    val anyResults = channelResults.isNotEmpty() || movieResults.isNotEmpty() || seriesResults.isNotEmpty()

    LaunchedEffect(query) {
        viewModel.setSearchQuery(query)
        vodViewModel.setVodSearchQuery(query)
    }
    BackHandler { onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(20.dp))
            Text(
                query.ifEmpty { stringResource(R.string.search_type_name) },
                style = MaterialTheme.typography.titleLarge,
                color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxSize()) {
            OnScreenKeyboard(
                onKey = { if (query.length < 40) query += it },
                onSpace = { if (query.length < 40) query += " " },
                onBackspace = { query = query.dropLast(1) },
                onClear = { query = "" },
            )

            Spacer(Modifier.width(28.dp))

            Column(Modifier.weight(1f).fillMaxSize()) {
                when {
                    query.isBlank() -> Hint(stringResource(R.string.search_start_hint))
                    query.trim().length < 2 -> Hint(stringResource(R.string.common_keep_typing))
                    !anyResults -> Hint(stringResource(R.string.search_no_results, query))
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (channelResults.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.common_channels)) }
                            items(channelResults, key = { "c${it.key}" }) { row ->
                                SearchResultRow(row = row, onClick = { onPlayChannel(row.primary) })
                            }
                        }
                        if (movieResults.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.nav_movies)) }
                            items(movieResults, key = { "m${it.id}" }) { movie ->
                                VodResultRow(movie.displayTitle, movie.posterUrl, listOfNotNull(movie.sourceTag, movie.year?.toString()).joinToString("  ·  ")) {
                                    onPlayMovie(movie)
                                }
                            }
                        }
                        if (seriesResults.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.nav_shows)) }
                            items(seriesResults, key = { "s${it.id}" }) { show ->
                                VodResultRow(show.displayTitle, show.posterUrl, listOfNotNull(show.sourceTag, show.year?.toString()).joinToString("  ·  ")) {
                                    onOpenSeries(show)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search on a phone: a real text field with the system keyboard (the d-pad keyboard above is for
 * remotes) and full-width results, filterable to one kind. Opened from Movies or Series it starts
 * filtered to that kind, so "search films" only shows films.
 */
@Composable
private fun PhoneSearchScreen(
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onBack: () -> Unit,
    initialScope: String,
    viewModel: ChannelsViewModel,
    vodViewModel: VodViewModel,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(initialScope) }
    val channelResults by viewModel.searchResults.collectAsState()
    val movieResults by vodViewModel.movieResults.collectAsState()
    val seriesResults by vodViewModel.seriesResults.collectAsState()
    val showChannels = scope == "all" || scope == "channels"
    val showMovies = scope == "all" || scope == "movies"
    val showSeries = scope == "all" || scope == "series"

    // Origin tags on the film/series results (EN, ES, DE, 4K, NF…), most common first, as quick
    // filters — the way to pick your language out of "a thousand Matrixes".
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    val movieTags = remember(movieResults) { movieResults.associate { it.id to it.sourceTag.orEmpty().split(' ') } }
    val seriesTags = remember(seriesResults) { seriesResults.associate { it.id to it.sourceTag.orEmpty().split(' ') } }
    val tagOptions = remember(movieTags, seriesTags, showMovies, showSeries) {
        buildList {
            if (showMovies) movieTags.values.forEach { addAll(it) }
            if (showSeries) seriesTags.values.forEach { addAll(it) }
        }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
    }
    LaunchedEffect(tagOptions) { if (tag != null && tag !in tagOptions) tag = null }
    val movies = if (tag == null) movieResults else movieResults.filter { tag in movieTags[it.id].orEmpty() }
    val shows = if (tag == null) seriesResults else seriesResults.filter { tag in seriesTags[it.id].orEmpty() }

    val anyResults = (showChannels && channelResults.isNotEmpty()) ||
        (showMovies && movies.isNotEmpty()) || (showSeries && shows.isNotEmpty())
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(query) {
        viewModel.setSearchQuery(query)
        vodViewModel.setVodSearchQuery(query)
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().screenPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_done))
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                placeholder = {
                    Text(
                        when (scope) {
                            "movies" -> stringResource(R.string.phone_search_movies)
                            "series" -> stringResource(R.string.phone_search_series)
                            "channels" -> stringResource(R.string.phone_search_all)
                            else -> stringResource(R.string.search_type_name)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.phone_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "all" to stringResource(R.string.phone_scope_all),
                "channels" to stringResource(R.string.common_channels),
                "movies" to stringResource(R.string.nav_movies),
                "series" to stringResource(R.string.nav_shows),
            ).forEach { (key, label) ->
                FilterChip(selected = scope == key, onClick = { scope = key }, label = { Text(label) })
            }
        }
        if (tagOptions.size > 1 && query.trim().length >= 2) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tagOptions.forEach { t ->
                    FilterChip(
                        selected = tag == t,
                        onClick = { tag = if (tag == t) null else t },
                        label = { Text(t) },
                    )
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                query.isBlank() -> Hint(stringResource(R.string.search_start_hint))
                query.trim().length < 2 -> Hint(stringResource(R.string.common_keep_typing))
                !anyResults -> Hint(stringResource(R.string.search_no_results, query))
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    // Scrolling the results is the cue to put the keyboard away.
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope { while (true) { awaitPointerEvent(); keyboard?.hide() } }
                    },
                ) {
                    if (showChannels && channelResults.isNotEmpty()) {
                        if (scope == "all") item { SectionHeader(stringResource(R.string.common_channels)) }
                        items(channelResults, key = { "c${it.key}" }) { row ->
                            SearchResultRow(row = row, onClick = { onPlayChannel(row.primary) })
                        }
                    }
                    if (showMovies && movies.isNotEmpty()) {
                        if (scope == "all") item { SectionHeader(stringResource(R.string.nav_movies)) }
                        items(movies, key = { "m${it.id}" }) { movie ->
                            VodResultRow(movie.displayTitle, movie.posterUrl, listOfNotNull(movie.sourceTag, movie.year?.toString()).joinToString("  ·  ")) { onPlayMovie(movie) }
                        }
                    }
                    if (showSeries && shows.isNotEmpty()) {
                        if (scope == "all") item { SectionHeader(stringResource(R.string.nav_shows)) }
                        items(shows, key = { "s${it.id}" }) { show ->
                            VodResultRow(show.displayTitle, show.posterUrl, listOfNotNull(show.sourceTag, show.year?.toString()).joinToString("  ·  ")) { onOpenSeries(show) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun VodResultRow(name: String, posterUrl: String?, subtitle: String?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 34.dp, height = 48.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchResultRow(row: ChannelsViewModel.Row, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = row.primary.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.primary.shownName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.now?.let { "${searchTime(it.startUtcMillis)}  ${it.title}" } ?: stringResource(R.string.guide_no_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val searchTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun searchTime(utcMillis: Long): String = searchTimeFormat.format(Date(utcMillis))
