/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.vod

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.opentv.core.ServiceLocator
import app.opentv.data.provider.ProviderCatalogRequest
import app.opentv.data.provider.ProviderCatalogSection
import app.opentv.data.provider.ProviderDetails
import app.opentv.data.provider.ProviderError
import app.opentv.data.provider.ProviderItem
import app.opentv.data.provider.ProviderMediaType
import app.opentv.data.provider.ProviderPlaybackTarget
import app.opentv.data.provider.ProviderResult
import app.opentv.data.provider.ProviderSearchRequest
import app.opentv.data.provider.ProviderStream
import app.opentv.data.provider.ProviderSubtitle
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CloudMovieShelf(
    val providerId: String,
    val providerName: String,
    val section: ProviderCatalogSection,
    val items: List<ProviderItem>,
)

data class CloudPlayback(
    val streams: List<ProviderStream>,
    val subtitles: List<ProviderSubtitle>,
)

class CloudVodViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val repository = graph.providerRepository

    private val _movieShelves = MutableStateFlow<List<CloudMovieShelf>>(emptyList())
    val movieShelves: StateFlow<List<CloudMovieShelf>> = _movieShelves.asStateFlow()

    private val _loadingMovies = MutableStateFlow(false)
    val loadingMovies: StateFlow<Boolean> = _loadingMovies.asStateFlow()

    private val _lastError = MutableStateFlow<ProviderError?>(null)
    val lastError: StateFlow<ProviderError?> = _lastError.asStateFlow()

    private val _movieSearchResults = MutableStateFlow<List<ProviderItem>>(emptyList())
    val movieSearchResults: StateFlow<List<ProviderItem>> = _movieSearchResults.asStateFlow()

    private var moviesLoaded = false
    private var searchJob: Job? = null

    fun loadMovieShelves(force: Boolean = false) {
        if (_loadingMovies.value || (moviesLoaded && !force)) return

        viewModelScope.launch {
            _loadingMovies.value = true
            _lastError.value = null
            try {
                val providers = repository.providers(ProviderMediaType.MOVIE)
                val shelves = coroutineScope {
                    providers.flatMap { provider ->
                        val sections = provider.catalogSections
                            .filter { it.mediaType == ProviderMediaType.MOVIE }
                            .ifEmpty {
                                listOf(
                                    ProviderCatalogSection(
                                        id = "default",
                                        title = provider.name,
                                        mediaType = ProviderMediaType.MOVIE,
                                    ),
                                )
                            }

                        sections.map { section ->
                            async {
                                when (
                                    val result = repository.catalog(
                                        provider.id,
                                        ProviderCatalogRequest(
                                            mediaType = ProviderMediaType.MOVIE,
                                            sectionId = section.id,
                                        ),
                                    )
                                ) {
                                    is ProviderResult.Success ->
                                        CloudMovieShelf(
                                            providerId = provider.id,
                                            providerName = provider.name,
                                            section = section,
                                            items = result.value.items,
                                        )
                                    is ProviderResult.Failure -> {
                                        _lastError.value = result.error
                                        null
                                    }
                                }
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                _movieShelves.value = shelves.filter { it.items.isNotEmpty() }
                moviesLoaded = true
            } finally {
                _loadingMovies.value = false
            }
        }
    }

    fun setMovieSearchQuery(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _movieSearchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            val result = repository.searchAll(
                ProviderSearchRequest(
                    query = trimmed,
                    mediaType = ProviderMediaType.MOVIE,
                ),
            )
            _movieSearchResults.value = result.values.values
                .flatten()
                .distinctBy { "${it.providerId}:${it.id}" }
            result.failures.values.firstOrNull()?.let { _lastError.value = it }
        }
    }

    fun providerName(providerId: String): String =
        repository.providers().firstOrNull { it.id == providerId }?.name ?: providerId

    suspend fun details(
        providerId: String,
        itemId: String,
    ): ProviderResult<ProviderDetails> =
        repository.load(providerId, itemId)

    suspend fun playback(
        providerId: String,
        itemId: String,
    ): ProviderResult<CloudPlayback> {
        val target = ProviderPlaybackTarget.Movie(providerId, itemId)
        val streams = repository.streams(target)
        if (streams is ProviderResult.Failure) return streams

        val subtitles = repository.subtitles(target)
        val subtitleValues = when (subtitles) {
            is ProviderResult.Success -> subtitles.value
            is ProviderResult.Failure -> emptyList()
        }

        return ProviderResult.Success(
            CloudPlayback(
                streams = (streams as ProviderResult.Success).value,
                subtitles = subtitleValues,
            ),
        )
    }
}
