/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import app.opentv.data.parser.VodTitleCleaner
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodTitleCleanerTest {

    @Test
    fun `prefix tags are the ones clean strips, in order`() {
        assertThat(VodTitleCleaner.prefixTags("4K-EN - The Matrix  (1999)")).containsExactly("4K", "EN").inOrder()
        assertThat(VodTitleCleaner.prefixTags("DE - Animatrix (2003)")).containsExactly("DE")
        assertThat(VodTitleCleaner.prefixTags("[ES] Torrente")).containsExactly("ES")
        assertThat(VodTitleCleaner.clean("4K-EN - The Matrix  (1999)")).isEqualTo("The Matrix (1999)")
    }

    @Test
    fun `a title with no provider prefix has no tags`() {
        assertThat(VodTitleCleaner.prefixTags("The Matrix (1999)")).isEmpty()
    }
}
