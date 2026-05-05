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
import org.junit.Ignore
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

    // -------------------------------------------------------------------------
    // Phase 6 D-23 EXTENSION — onboarding_completed key (UX-01 + UX-02)
    //
    // The plan extends this same FilterPrefsRepository class (rather than creating
    // OnboardingPrefsRepository) so we have ONE repo + ONE test class for DataStore
    // round-trip + corruption-default semantics. Tests are @Ignored at Wave 0 because
    // the property `onboardingCompleted: Flow<Boolean>` and the writer
    // `setOnboardingCompleted(value: Boolean)` are landing in Plan 06-02.
    //
    // When Plan 06-02 lands, the implementer will:
    //   - Add `KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")`
    //     and DEFAULT_ONBOARDING_COMPLETED = false in FilterPrefsRepository.companion
    //   - Add `onboardingCompleted: Flow<Boolean>` mirroring the lastUsedFilterId .catch pattern
    //   - Add `suspend fun setOnboardingCompleted(value: Boolean)`
    //   - Remove @Ignore from the three tests below
    //
    // T-06-04 mirror of T-04-01: corrupt DataStore → emit `false` (safe default — better to
    // re-show onboarding than to skip it on a corrupted prefs file).
    // -------------------------------------------------------------------------

    /**
     * UX-02 round-trip: setOnboardingCompleted() → onboardingCompleted.first() == true.
     * Mirrors the writeThenRead_returnsSameId pattern. D-23 specifies the setter has no
     * argument (single-shot "mark complete"); idempotent on repeated calls.
     */
    @Test
    fun writeOnboardingCompleted_thenRead_returnsTrue() = runTest(testDispatcher) {
        val repo = newRepo("onboarding-write.preferences_pb")
        repo.setOnboardingCompleted()
        advanceUntilIdle()
        assertEquals(true, repo.onboardingCompleted.first())
    }

    /**
     * UX-01 first-launch default: fresh DataStore (no onboarding write yet) → onboardingCompleted
     * emits false. SplashViewModel uses this to route first-launch users to OnboardingScreen.
     */
    @Test
    fun readOnboardingCompleted_beforeWrite_returnsFalseDefault() = runTest(testDispatcher) {
        val repo = newRepo("onboarding-empty.preferences_pb")
        assertEquals(false, repo.onboardingCompleted.first())
    }

    /**
     * T-06-04 mitigation: if the DataStore preferences file is corrupted (IOException on read),
     * onboardingCompleted must emit false (safe default — re-show onboarding, never crash).
     * Mirrors the corruptedDataStore_emitsDefault pattern but for the boolean key.
     */
    @Test
    fun corruptedDataStore_onboardingCompleted_emitsFalseDefault() = runTest(testDispatcher) {
        val throwingStore: DataStore<Preferences> = mock()
        whenever(throwingStore.data).doReturn(
            flow<Preferences> { throw IOException("simulated corruption") }
        )
        val repo = FilterPrefsRepository(throwingStore)
        assertEquals(false, repo.onboardingCompleted.first())
    }
}
