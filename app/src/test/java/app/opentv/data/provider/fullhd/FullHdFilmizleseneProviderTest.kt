package app.opentv.data.provider.fullhd

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.junit.Test

class FullHdFilmizleseneProviderTest {
    @Test
    fun cardParser_readsCurrentSiteShape() {
        val html = """
            <ul class="list">
              <li class="film">
                <a class="tt" href="https://www.fullhdfilmizlesene.now/film/the-matrix-1/">
                  The Matrix 1 izle
                </a>
                <h2 class="film-tt"><span class="film-title">The Matrix 1</span></h2>
                <span class="imdb">8.7</span>
                <span class="film-yil">1999</span>
                <picture>
                  <img class="lazy afis"
                       data-src="https://img.fullhdfilmizlesene.now/poster/film/fullhd-the-matrix-1-720p.jpg"/>
                </picture>
              </li>
            </ul>
        """.trimIndent()

        val provider = FullHdFilmizleseneProvider(OkHttpClient())
        val items = provider.parseCards(
            Jsoup.parse(html, FullHdFilmizleseneProvider.MAIN_URL),
        )

        assertThat(items).hasSize(1)
        assertThat(items.single().title).isEqualTo("The Matrix 1")
        assertThat(items.single().year).isEqualTo(1999)
        assertThat(items.single().rating).isEqualTo(8.7)
        assertThat(items.single().posterUrl).contains("fullhd-the-matrix-1-720p.jpg")
        assertThat(items.single().id).contains("/film/the-matrix-1/")
    }
}
