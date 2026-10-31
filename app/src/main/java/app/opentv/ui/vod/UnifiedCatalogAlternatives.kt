package app.opentv.ui.vod

import app.opentv.data.provider.ProviderItem
import app.opentv.data.provider.ProviderMediaType

/** One visible item, with its real provider-specific item IDs retained for playback. */
internal data class UnifiedCatalogGroup(
    val primary: ProviderItem,
    val sources: List<ProviderItem>,
)

/** In-process catalogue index: navigation routes only carry provider and item IDs. */
internal object UnifiedCatalogAlternatives {
    private const val LIMIT = 2_048
    private val sources = LinkedHashMap<String, List<ProviderItem>>()

    private fun id(item: ProviderItem) = "${item.providerId}:${item.id}"

    @Synchronized
    fun remember(groups: List<UnifiedCatalogGroup>) {
        groups.forEach { group ->
            val previous = group.sources.flatMap { sources[id(it)].orEmpty() }
            val combined = (group.sources + previous).distinctBy(::id)
            combined.forEach { item ->
                sources.remove(id(item))
                sources[id(item)] = combined
            }
        }
        while (sources.size > LIMIT) {
            val oldest = sources.keys.firstOrNull() ?: break
            sources.remove(oldest)
        }
    }

    @Synchronized
    fun alternatives(providerId: String, itemId: String): List<ProviderItem> =
        sources["$providerId:$itemId"].orEmpty()
            .filterNot { it.providerId == providerId && it.id == itemId }
}

/** Playback recovery target: series episodes are matched by season and episode number, never by opaque IDs. */
data class UnifiedFallbackContext(
    val mediaType: ProviderMediaType,
    val alternateItems: List<ProviderItem>,
    val season: Int? = null,
    val episodeNumber: Int? = null,
) {
    fun without(providerId: String): UnifiedFallbackContext =
        copy(alternateItems = alternateItems.filterNot { it.providerId == providerId })
}
