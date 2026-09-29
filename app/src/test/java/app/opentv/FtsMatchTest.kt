/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.data.repo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FtsMatchTest {

    @Test
    fun `every word becomes a prefix term`() {
        assertThat(ftsMatch("The Matr")).isEqualTo("the* matr*")
    }

    @Test
    fun `fts syntax characters cannot break the query`() {
        assertThat(ftsMatch("\"spider-man\" OR*")).isEqualTo("spider* man* or*")
    }

    @Test
    fun `accented letters and digits survive`() {
        assertThat(ftsMatch("Acción 2")).isEqualTo("acción* 2*")
    }

    @Test
    fun `nothing searchable gives null`() {
        assertThat(ftsMatch(" -- ")).isNull()
    }
}
