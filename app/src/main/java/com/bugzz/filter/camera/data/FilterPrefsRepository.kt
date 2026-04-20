package com.bugzz.filter.camera.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Top-level delegate — MUST NOT be inside the class body (04-RESEARCH Pitfall 5).
private val Context.bugzzPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "bugzz_prefs")

/**
 * DataStore wrapper for user preferences — specifically the last-used filter id (CAT-05 / D-25).
 *
 * Constructor split pattern (Phase 2 STATE #14):
 *   - Primary `internal constructor(dataStore)` = test seam; accepts any DataStore<Preferences>.
 *   - Secondary `@Inject constructor(@ApplicationContext context)` = production Hilt binding;
 *     delegates to primary with the application-scoped `bugzzPrefsDataStore` extension.
 *
 * T-04-01 mitigation: [lastUsedFilterId] flow uses `.catch` to emit [emptyPreferences] on
 * [IOException] (DataStore file corruption), preventing app crash and falling back to
 * [DEFAULT_FILTER_ID] via the map fallback.
 */
@Singleton
class FilterPrefsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // Production @Inject ctor delegates to primary with application-context DataStore.
    @Inject constructor(@ApplicationContext context: Context) : this(context.bugzzPrefsDataStore)

    /**
     * Emits the stored filter id. On [IOException] (file corruption — T-04-01), falls back to
     * [DEFAULT_FILTER_ID] via [emptyPreferences] emission.
     */
    val lastUsedFilterId: Flow<String> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Timber.tag("FilterPrefs").w(e, "DataStore read error — using default")
                emit(emptyPreferences())
            } else throw e
        }
        .map { prefs -> prefs[KEY_LAST_FILTER] ?: DEFAULT_FILTER_ID }

    /**
     * Persists the selected filter id to DataStore (CAT-05).
     * Called from [CameraViewModel.onSelectFilter] via viewModelScope.launch (non-blocking).
     */
    suspend fun setLastUsedFilter(id: String) {
        dataStore.edit { prefs -> prefs[KEY_LAST_FILTER] = id }
    }

    companion object {
        private val KEY_LAST_FILTER = stringPreferencesKey("last_used_filter_id")

        /** First entry in FilterCatalog.all after Plan 04-04 roster (D-02). */
        const val DEFAULT_FILTER_ID = "spider_nose_static"
    }
}
