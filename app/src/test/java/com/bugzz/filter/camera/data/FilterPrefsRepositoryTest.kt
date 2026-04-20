package com.bugzz.filter.camera.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Nyquist Wave 0 tests for [FilterPrefsRepository] (CAT-05 / T-04-01 / D-25).
 *
 * Tests pin the DataStore round-trip contract:
 *   - writeThenRead returns the written value
 *   - readBeforeWrite returns DEFAULT_FILTER_ID
 *   - DataStore corruption → emits default (T-04-01 mitigation)
 *   - consecutive writes → last write wins (no accumulation)
 *
 * **All tests are @Ignore'd** — Plan 04-05 Task 1 lands FilterPrefsRepository + DataStore wiring
 * and un-Ignores these tests (RED → GREEN transition).
 *
 * Types referenced in comments below land in Plan 04-05:
 *   - com.bugzz.filter.camera.data.FilterPrefsRepository
 *     - companion object { const val DEFAULT_FILTER_ID = "spider_nose_static" }
 *     - val lastUsedFilterId: Flow<String>
 *     - suspend fun setLastUsedFilter(id: String)
 *     - (test seam) companion/factory: forTest(file: File): FilterPrefsRepository
 *       OR @VisibleForTesting constructor(dataStore: DataStore<Preferences>)
 *
 * Implementation note: the test seam strategy (forTest factory vs injected DataStore constructor)
 * is Plan 04-05's discretion. Comments below use a hypothetical forTest() pattern — Plan 04-05
 * may choose the @VisibleForTesting constructor approach instead and document in 04-05-SUMMARY.
 *
 * Uses InMemoryDataStore via PreferenceDataStoreFactory + TemporaryFolder (pure JVM, no
 * Robolectric needed — DataStore Preferences core has a JVM implementation for tests).
 *
 * Flow testing via Turbine 1.2.0 (added in Plan 04-01).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    // -------------------------------------------------------------------------
    // CAT-05: write then read returns same id
    // -------------------------------------------------------------------------

    /**
     * CAT-05: setLastUsedFilter("bugA_fall") → lastUsedFilterId.first() == "bugA_fall".
     *
     * Uses a file-backed DataStore in a TemporaryFolder (cleaned up after test).
     * No DataStore singleton pollution across tests.
     */
    @Test
    @Ignore("TODO Plan 04-05 Task 1 — un-Ignore when FilterPrefsRepository + DataStore wiring lands")
    fun writeThenRead_returnsSameId() = runTest(testDispatcher) {
        // val tempFile = File(tempFolder.root, "test_prefs.preferences_pb")
        // val testDataStore = PreferenceDataStoreFactory.create(
        //     corruptionHandler = null,
        //     produceFile = { tempFile }
        // )
        // val repo = FilterPrefsRepository(testDataStore)  // or .forTest(tempFile)
        //
        // repo.setLastUsedFilter("bugA_fall")
        //
        // repo.lastUsedFilterId.test {
        //     assertEquals("write then read must return same id",
        //         "bugA_fall", awaitItem())
        //     cancelAndIgnoreRemainingEvents()
        // }
    }

    // -------------------------------------------------------------------------
    // CAT-05: read before any write returns DEFAULT_FILTER_ID
    // -------------------------------------------------------------------------

    /**
     * CAT-05: fresh DataStore (no writes) → lastUsedFilterId emits DEFAULT_FILTER_ID.
     * DEFAULT_FILTER_ID = "spider_nose_static" per D-02 roster (first entry in catalog).
     */
    @Test
    @Ignore("TODO Plan 04-05 Task 1 — un-Ignore when DEFAULT_FILTER_ID constant and fallback logic land")
    fun read_beforeWrite_returnsDefaultFilterId() = runTest(testDispatcher) {
        // val tempFile = File(tempFolder.root, "empty_prefs.preferences_pb")
        // val testDataStore = PreferenceDataStoreFactory.create(
        //     corruptionHandler = null,
        //     produceFile = { tempFile }
        // )
        // val repo = FilterPrefsRepository(testDataStore)
        //
        // repo.lastUsedFilterId.test {
        //     assertEquals("fresh DataStore must emit DEFAULT_FILTER_ID",
        //         FilterPrefsRepository.DEFAULT_FILTER_ID, awaitItem())
        //     cancelAndIgnoreRemainingEvents()
        // }
    }

    // -------------------------------------------------------------------------
    // T-04-01: DataStore corruption → emits default (mitigation for T-04-01)
    // -------------------------------------------------------------------------

    /**
     * T-04-01 mitigation: if the DataStore preferences file is corrupted (IO exception on read),
     * the repository must NOT crash — it must emit DEFAULT_FILTER_ID as a safe fallback.
     *
     * Implementation: FilterPrefsRepository.lastUsedFilterId uses `.catch { emit(emptyPreferences()) }`
     * (or equivalent ReplaceFileCorruptionHandler) to recover. This test pins that contract.
     *
     * Strategy options for Plan 04-05:
     *   (a) Write a corrupt byte sequence to the temp file before constructing DataStore
     *   (b) Inject a mock DataStore<Preferences> whose flow throws IOException
     *   Either is acceptable; (b) is simpler for unit testing; document in 04-05-SUMMARY.
     */
    @Test
    @Ignore("TODO Plan 04-05 Task 1 — T-04-01 mitigation: un-Ignore when corruption fallback .catch lands")
    fun corruptedDataStore_emitsDefaultFilterId() = runTest(testDispatcher) {
        // Option A (file corruption): write garbage bytes to tempFile before create
        // val tempFile = File(tempFolder.root, "corrupt.preferences_pb")
        // tempFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x01, 0x42))
        // val testDataStore = PreferenceDataStoreFactory.create(
        //     corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        //     produceFile = { tempFile }
        // )
        // val repo = FilterPrefsRepository(testDataStore)
        //
        // repo.lastUsedFilterId.test {
        //     assertEquals("corrupt DataStore must fall back to DEFAULT_FILTER_ID",
        //         FilterPrefsRepository.DEFAULT_FILTER_ID, awaitItem())
        //     cancelAndIgnoreRemainingEvents()
        // }
    }

    // -------------------------------------------------------------------------
    // CAT-05: second write overwrites first (no accumulation)
    // -------------------------------------------------------------------------

    /**
     * CAT-05: writing twice must result in the second value being stored,
     * not both values concatenated or the first value retained.
     */
    @Test
    @Ignore("TODO Plan 04-05 Task 1 — un-Ignore when FilterPrefsRepository write-overwrite semantics confirmed")
    fun writeAgain_overwritesOldValue() = runTest(testDispatcher) {
        // val tempFile = File(tempFolder.root, "overwrite_prefs.preferences_pb")
        // val testDataStore = PreferenceDataStoreFactory.create(
        //     corruptionHandler = null,
        //     produceFile = { tempFile }
        // )
        // val repo = FilterPrefsRepository(testDataStore)
        //
        // repo.setLastUsedFilter("bugB_crawl")
        // repo.setLastUsedFilter("bugC_fall")
        //
        // repo.lastUsedFilterId.test {
        //     assertEquals("second write must overwrite first — last write wins",
        //         "bugC_fall", awaitItem())
        //     cancelAndIgnoreRemainingEvents()
        // }
    }

    // -------------------------------------------------------------------------
    // CAT-05: unknown id from DataStore (not in current catalog) → falls back to default
    // -------------------------------------------------------------------------

    /**
     * CAT-05: if the stored filter ID is no longer in FilterCatalog.all (e.g., catalog changed
     * between app versions), CameraViewModel.bind() must fall back to DEFAULT_FILTER_ID.
     *
     * Note: this invariant is tested at the CameraViewModel level (Plan 04-05 Task 3), not
     * at the repository level — the repository stores any String without validation.
     * This test is a REMINDER stub; actual enforcement is in CameraViewModelTest.
     *
     * If Plan 04-05 decides to add validation at repository level, un-Ignore and implement here.
     */
    @Test
    @Ignore("TODO Plan 04-05 Task 3 — unknown-id fallback tested in CameraViewModelTest; this stub may be deleted")
    fun unknownIdFromDataStore_viewModelFallsBackToDefault() = runTest(testDispatcher) {
        // FilterPrefsRepository stores raw String — no catalog validation at this layer.
        // CameraViewModel.bind() reads lastUsedFilterId and validates against FilterCatalog.all.
        // See CameraViewModelTest.initialBind_unknownIdFromDataStore_fallsBackToDefault (Plan 04-05 optional)
    }
}
