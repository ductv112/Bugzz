package com.bugzz.filter.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

/**
 * Unit tests for [FilterPrefsRepository] (CAT-05 / T-04-01 / D-25).
 *
 * Tests pin the DataStore round-trip contract:
 *   - writeThenRead returns the written value
 *   - readBeforeWrite returns DEFAULT_FILTER_ID
 *   - DataStore corruption → emits default (T-04-01 mitigation)
 *   - consecutive writes → last write wins (no accumulation)
 *
 * Uses [PreferenceDataStoreFactory] with [TemporaryFolder] for file-backed instances.
 * The `internal constructor(DataStore<Preferences>)` test seam (Phase 2 STATE #14) avoids
 * the Context.preferencesDataStore singleton constraint in tests.
 *
 * Corruption test uses a mock DataStore whose data flow throws [IOException] — simpler and
 * more deterministic than writing a corrupt binary proto file (see 04-05-PLAN comment §Strategy b).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilterPrefsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper: creates a FilterPrefsRepository backed by a fresh DataStore file in [tempFolder].
     * Each call with a different [fileName] gets an isolated store (no test cross-contamination).
     */
    private fun newRepo(fileName: String = "test.preferences_pb"): FilterPrefsRepository {
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            produceFile = { File(tempFolder.root, fileName) },
        )
        return FilterPrefsRepository(store) // internal test-seam constructor
    }

    // -------------------------------------------------------------------------
    // CAT-05: write then read returns same id
    // -------------------------------------------------------------------------

    /**
     * CAT-05: setLastUsedFilter("bugA_fall") → lastUsedFilterId.first() == "bugA_fall".
     */
    @Test
    fun writeThenRead_returnsSameId() = runTest(testDispatcher) {
        val repo = newRepo()
        repo.setLastUsedFilter("bugA_fall")
        advanceUntilIdle()
        assertEquals("bugA_fall", repo.lastUsedFilterId.first())
    }

    // -------------------------------------------------------------------------
    // CAT-05: read before any write returns DEFAULT_FILTER_ID
    // -------------------------------------------------------------------------

    /**
     * CAT-05: fresh DataStore (no writes) → lastUsedFilterId emits DEFAULT_FILTER_ID.
     * DEFAULT_FILTER_ID = "spider_nose_static" per D-02 roster (first entry in catalog).
     */
    @Test
    fun read_beforeWrite_returnsDefault() = runTest(testDispatcher) {
        val repo = newRepo("empty.preferences_pb")
        assertEquals(FilterPrefsRepository.DEFAULT_FILTER_ID, repo.lastUsedFilterId.first())
    }

    // -------------------------------------------------------------------------
    // CAT-05: second write overwrites first (no accumulation)
    // -------------------------------------------------------------------------

    /**
     * CAT-05: writing twice must result in the second value being stored — last write wins.
     */
    @Test
    fun writeAgain_overwritesOldValue() = runTest(testDispatcher) {
        val repo = newRepo()
        repo.setLastUsedFilter("first")
        repo.setLastUsedFilter("second")
        advanceUntilIdle()
        assertEquals("second", repo.lastUsedFilterId.first())
    }

    // -------------------------------------------------------------------------
    // T-04-01: DataStore corruption → emits default (mitigation for T-04-01)
    // -------------------------------------------------------------------------

    /**
     * T-04-01 mitigation: if the DataStore preferences file is corrupted ([IOException] on read),
     * the repository must NOT crash — it must emit [DEFAULT_FILTER_ID] as a safe fallback.
     *
     * Uses a mock DataStore whose data flow throws [IOException] — approach (b) per plan notes.
     */
    @Test
    fun corruptedDataStore_emitsDefault() = runTest(testDispatcher) {
        val throwingStore: DataStore<Preferences> = mock()
        whenever(throwingStore.data).doReturn(
            flow<Preferences> { throw IOException("simulated corruption") }
        )
        val repo = FilterPrefsRepository(throwingStore)
        assertEquals(FilterPrefsRepository.DEFAULT_FILTER_ID, repo.lastUsedFilterId.first())
    }
}
