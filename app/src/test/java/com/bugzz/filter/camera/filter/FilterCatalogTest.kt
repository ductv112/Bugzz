package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.detector.FaceLandmarkMapper.Anchor
import com.bugzz.filter.camera.render.BugBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Nyquist Wave 0 tests for [FilterCatalog] (REN-03 / D-01/D-02).
 *
 * Pins the catalog shape: exactly 2 filters, correct anchor types and behaviors.
 *
 * Wave 0 state: [FilterCatalog.all] is an empty stub and [FilterCatalog.byId] always returns null.
 * All tests are @Ignore'd to keep exit 0. Plan 03-03 populates the catalog and un-Ignores them.
 *
 * Pure JVM — no Android framework types needed.
 */
class FilterCatalogTest {

    /**
     * TODO Plan 03-03: un-Ignore when FilterCatalog.all is populated with 2 entries.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when FilterCatalog has 2 entries")
    @Test
    fun catalog_hasExactlyTwoFilters() {
        assertEquals(
            "FilterCatalog must contain exactly 2 filters (ant_on_nose_v1 + spider_on_forehead_v1)",
            2, FilterCatalog.all.size,
        )
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterCatalog contains ant_on_nose_v1.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when ant_on_nose_v1 is in catalog")
    @Test
    fun catalog_byId_antOnNose_resolves() {
        val filter = FilterCatalog.byId("ant_on_nose_v1")
        assertNotNull("ant_on_nose_v1 must exist in catalog", filter)
        assertEquals("ant_on_nose_v1 anchor must be NOSE_TIP (D-01)", Anchor.NOSE_TIP, filter!!.anchorType)
        // BugBehavior.Static is an object — use === identity check
        assertEquals("ant_on_nose_v1 behavior must be Static (D-01)", BugBehavior.Static, filter.behavior)
        assertEquals("ant_on_nose_v1 scaleFactor must be 0.20f (D-01)", 0.20f, filter.scaleFactor, 1e-4f)
        assertEquals("ant_on_nose_v1 assetDir must match D-06 layout", "sprites/ant_on_nose_v1", filter.assetDir)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterCatalog contains spider_on_forehead_v1.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when spider_on_forehead_v1 is in catalog")
    @Test
    fun catalog_byId_spiderOnForehead_resolves() {
        val filter = FilterCatalog.byId("spider_on_forehead_v1")
        assertNotNull("spider_on_forehead_v1 must exist in catalog", filter)
        assertEquals("spider_on_forehead_v1 anchor must be FOREHEAD (D-02)", Anchor.FOREHEAD, filter!!.anchorType)
        assertEquals("spider_on_forehead_v1 behavior must be Static (D-02)", BugBehavior.Static, filter.behavior)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterCatalog.byId() has real lookup logic.
     * Note: this test already passes with the stub (returns null) but relies on behavior
     * that is accidentally correct. Un-Ignore explicitly for completeness.
     */
    @org.junit.Ignore("Plan 03-03 — un-Ignore when FilterCatalog has real byId() implementation")
    @Test
    fun catalog_byId_unknown_returnsNull() {
        assertNull(
            "byId with unknown id must return null",
            FilterCatalog.byId("nonexistent_filter_v99"),
        )
    }
}
