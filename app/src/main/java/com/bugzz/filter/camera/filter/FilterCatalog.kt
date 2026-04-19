package com.bugzz.filter.camera.filter

/**
 * Registry of all available bug filters.
 *
 * STUB — Plan 03-03 replaces this with the 2-entry production catalog (ant_on_nose_v1 +
 * spider_on_forehead_v1) per D-01/D-02. The stub returns empty list so
 * [FilterCatalogTest.catalog_hasExactlyTwoFilters] intentionally FAILS (RED gate).
 */
object FilterCatalog {

    /** All available filters. STUB: empty list — Plan 03-03 populates with 2 entries. */
    val all: List<FilterDefinition> = emptyList()

    /**
     * Look up a filter by its unique [id].
     * STUB: always returns null — Plan 03-03 implements real lookup.
     */
    fun byId(id: String): FilterDefinition? = null
}
