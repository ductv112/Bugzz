package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.render.BugBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Nyquist Wave 0 tests for the Phase 4 expanded [FilterCatalog] (CAT-01 / CAT-02 / D-02 amended).
 *
 * Tests pin the 15-entry catalog contract — all fields populated, unique IDs, valid enums,
 * positive frame counts, scale factors in (0,1], asset dirs from the 4-sprite-group set.
 *
 * **All tests are @Ignore'd** — Plan 04-04 Task 2 expands FilterCatalog from 2 entries (Phase 3)
 * to 15 entries per D-02 roster and un-Ignores these tests (RED → GREEN transition).
 *
 * NOTE: The existing [FilterCatalogTest] (Phase 3, 2 entries) is NOT deleted by this plan.
 * Plan 04-04 Task 2 decides whether to:
 *   (a) Delete FilterCatalogTest.kt once 15-entry catalog supersedes 2-entry catalog, or
 *   (b) Mark its @Test methods @Ignore'd with a note about D-02 supersession.
 * Either choice is documented in 04-04-SUMMARY.md.
 *
 * Pure JVM test — no Robolectric needed (FilterCatalog.all is a simple List, no Android types).
 */
class FilterCatalogExpandedTest {

    // -------------------------------------------------------------------------
    // CAT-01: catalog has exactly 15 entries
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when FilterCatalog expands to 15 entries per D-02")
    fun catalog_has_15_entries() {
        assertEquals(
            "FilterCatalog must contain exactly 15 entries per D-02 amended roster",
            15,
            FilterCatalog.all.size,
        )
    }

    // -------------------------------------------------------------------------
    // CAT-02: all entries have unique IDs
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands")
    fun allEntries_haveUniqueIds() {
        val ids = FilterCatalog.all.map { it.id }
        assertEquals(
            "All filter IDs must be unique — duplicates indicate catalog data error",
            ids.size,
            ids.toSet().size,
        )
    }

    // -------------------------------------------------------------------------
    // CAT-02: all entries have non-empty display names
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2")
    fun allEntries_haveNonEmptyDisplayName() {
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' must have non-empty displayName",
                filter.displayName.isNotBlank(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // CAT-02: all entries have frameCount > 0
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands")
    fun allEntries_haveFrameCountGreaterThan0() {
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' must have frameCount > 0 (got ${filter.frameCount})",
                filter.frameCount > 0,
            )
        }
    }

    // -------------------------------------------------------------------------
    // CAT-02: scale factor in (0, 1]
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands")
    fun allEntries_haveScaleFactorInExpectedRange() {
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' scaleFactor must be in (0, 1] (got ${filter.scaleFactor})",
                filter.scaleFactor > 0f && filter.scaleFactor <= 1f,
            )
        }
    }

    // -------------------------------------------------------------------------
    // CAT-02: assetDir must be one of the 4 extractable sprite groups (D-02 / T-04-02)
    // -------------------------------------------------------------------------

    /**
     * T-04-02 mitigation: pins the 4-sprite-group constraint — prevents accidental catalog
     * entries pointing to non-existent asset directories (T-04-02 Tampering disposition).
     *
     * Allowed set per D-02 amended roster:
     *   sprites/sprite_spider — 23 frames (spider_prankfilter.json)
     *   sprites/sprite_bugA   — 7 frames  (home_lottie.json group A)
     *   sprites/sprite_bugB   — 12 frames (home_lottie.json group B)
     *   sprites/sprite_bugC   — 16 frames (home_lottie.json group C)
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands; T-04-02 mitigation")
    fun allEntries_assetDirInAllowedSet() {
        val allowed = setOf(
            "sprites/sprite_spider",
            "sprites/sprite_bugA",
            "sprites/sprite_bugB",
            "sprites/sprite_bugC",
        )
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' assetDir '${filter.assetDir}' must be in allowed set $allowed",
                filter.assetDir in allowed,
            )
        }
    }

    // -------------------------------------------------------------------------
    // CAT-01: all 4 behaviors represented with ≥3 entries each (D-02 coverage)
    // -------------------------------------------------------------------------

    /**
     * D-02 behavior coverage: 6 STATIC + 3 CRAWL + 3 SWARM + 3 FALL = 15 total.
     * Each behavior must have ≥3 entries so the picker shows meaningful variety per mode.
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands")
    fun allFourBehaviors_representedWithAtLeast3Entries() {
        // Group entries by behavior variant simple name via reflection (avoids when-exhaustive
        // churn when BugBehavior variants change naming in Phase 4 refactor).
        val byBehavior = FilterCatalog.all.groupingBy {
            it.behavior::class.java.simpleName
        }.eachCount()

        assertTrue(
            "STATIC behavior must have ≥3 entries per D-02 (found ${byBehavior["Static"] ?: 0})",
            (byBehavior["Static"] ?: 0) >= 3,
        )
        assertTrue(
            "CRAWL behavior must have ≥3 entries per D-02 (found ${byBehavior["Crawl"] ?: 0})",
            (byBehavior["Crawl"] ?: 0) >= 3,
        )
        assertTrue(
            "SWARM behavior must have ≥3 entries per D-02 (found ${byBehavior["Swarm"] ?: 0})",
            (byBehavior["Swarm"] ?: 0) >= 3,
        )
        assertTrue(
            "FALL behavior must have ≥3 entries per D-02 (found ${byBehavior["Fall"] ?: 0})",
            (byBehavior["Fall"] ?: 0) >= 3,
        )
    }

    // -------------------------------------------------------------------------
    // CAT-02: frameDurationMs > 0 for all entries
    // -------------------------------------------------------------------------

    @Test
    @Ignore("TODO Plan 04-04 Task 2 — un-Ignore when 15-entry catalog lands")
    fun allEntries_haveFrameDurationMsGreaterThan0() {
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' must have frameDurationMs > 0 (got ${filter.frameDurationMs})",
                filter.frameDurationMs > 0L,
            )
        }
    }
}
