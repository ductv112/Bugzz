package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.render.BugBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 0 tests for the Phase 4 expanded [FilterCatalog] (CAT-01 / CAT-02 / D-02 amended).
 *
 * Tests pin the 15-entry catalog contract — all fields populated, unique IDs, valid enums,
 * positive frame counts, scale factors in (0,1], asset dirs from the 4-sprite-group set.
 *
 * Plan 04-04 Task 2 expanded FilterCatalog from 2 entries (Phase 3) to 15 entries per D-02
 * roster and un-Ignored these tests.
 *
 * FilterCatalogTest.kt (Phase 3, 2-entry) was DELETED by Plan 04-04 — superseded by this file.
 *
 * Pure JVM test — no Robolectric needed (FilterCatalog.all is a simple List, no Android types).
 */
class FilterCatalogExpandedTest {

    // -------------------------------------------------------------------------
    // CAT-01: catalog has exactly 15 entries
    // -------------------------------------------------------------------------

    @Test
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

    @Test
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

    @Test
    fun allFourBehaviors_representedWithAtLeast3Entries() {
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
    fun allEntries_haveFrameDurationMsGreaterThan0() {
        FilterCatalog.all.forEach { filter ->
            assertTrue(
                "Filter '${filter.id}' must have frameDurationMs > 0 (got ${filter.frameDurationMs})",
                filter.frameDurationMs > 0L,
            )
        }
    }
}
