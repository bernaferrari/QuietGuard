package com.bernaferrari.quietguard.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val initial = readInitialPreferences(dataStore)
    private val state = MutableStateFlow(initial ?: emptyPreferences())
    private val loadedState = MutableStateFlow(initial != null)
    private val listeners = mutableListOf<(String) -> Unit>()
    private val notifications =
        Channel<Pair<Set<String>, List<(String) -> Unit>>>(Channel.UNLIMITED)
    private val lock = SynchronizedObject()
    private val writes = Channel<Write>(Channel.UNLIMITED)
    private val pending = mutableListOf<(MutablePreferences) -> Unit>()

    private data class Write(
        val mutate: (MutablePreferences) -> Unit,
        val completion: CompletableDeferred<Unit>?,
    )

    private val changeEvents =
        MutableSharedFlow<Set<String>>(
            extraBufferCapacity = 64,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
        )
    // One writer preserves the order in which callers see changes in memory.
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    val data: StateFlow<Preferences> = state.asStateFlow()
    val isLoaded: StateFlow<Boolean> = loadedState.asStateFlow()
    val changes: SharedFlow<Set<String>> = changeEvents.asSharedFlow()

    val enabledFlow: Flow<Boolean> = data.map { it[KEY_ENABLED] ?: false }.distinctUntilChanged()

    init {
        scope.launch {
            for ((keys, listeners) in notifications) {
                for (key in keys) {
                    for (listener in listeners) {
                        runCatching { listener(key) }
                            .onFailure {
                                println("QuietGuard preference listener failed: ${it.message}")
                            }
                    }
                }
            }
        }
        scope.launch {
            if (initial == null) {
                val stored = dataStore.data.first()
                synchronized(lock) {
                    val merged = stored.toMutablePreferences()
                    pending.forEach { it(merged) }
                    pending.clear()
                    publish(merged.toPreferences())
                    loadedState.value = true
                }
            }
            for ((mutate, completion) in writes) {
                try {
                    dataStore.edit { mutate(it) }
                    completion?.complete(Unit)
                } catch (cancelled: CancellationException) {
                    completion?.cancel(cancelled)
                    throw cancelled
                } catch (failure: Exception) {
                    if (completion != null) completion.completeExceptionally(failure)
                    else println("Unable to save QuietGuard preferences: ${failure.message}")
                }
            }
        }
    }

    fun addListener(listener: (String) -> Unit): () -> Unit {
        synchronized(lock) { listeners.add(listener) }
        return { synchronized(lock) { listeners.remove(listener) } }
    }

    fun getBoolean(key: BooleanPreference): Boolean = getBoolean(key.name, key.default)

    fun getBoolean(name: String, default: Boolean = false): Boolean =
        state.value[booleanPreferencesKey(name)] ?: default

    fun getInt(name: String, default: Int = 0): Int =
        state.value[intPreferencesKey(name)] ?: default

    fun getLong(name: String, default: Long = 0L): Long =
        state.value[longPreferencesKey(name)] ?: default

    fun getFloat(name: String, default: Float = 0f): Float =
        state.value[floatPreferencesKey(name)] ?: default

    fun getString(name: String, default: String? = null): String? =
        state.value[stringPreferencesKey(name)] ?: default

    fun getStringSet(name: String, default: Set<String> = emptySet()): Set<String> =
        state.value[stringSetPreferencesKey(name)] ?: default

    fun putBoolean(key: BooleanPreference, value: Boolean) = putBoolean(key.name, value)

    fun putBoolean(name: String, value: Boolean) = update {
        it[booleanPreferencesKey(name)] = value
    }

    fun putInt(name: String, value: Int) = update { it[intPreferencesKey(name)] = value }

    fun putLong(name: String, value: Long) = update { it[longPreferencesKey(name)] = value }

    fun putFloat(name: String, value: Float) = update { it[floatPreferencesKey(name)] = value }

    fun putString(name: String, value: String?) = update {
        val key = stringPreferencesKey(name)
        if (value == null) {
            it -= key
        } else {
            it[key] = value
        }
    }

    fun putStringSet(name: String, value: Set<String>) = update {
        it[stringSetPreferencesKey(name)] = value
    }

    fun remove(name: String) = update { preferences ->
        preferences.asMap().keys.filter { it.name == name }.forEach { key -> preferences -= key }
    }

    fun removeBoolean(name: String) = update { it -= booleanPreferencesKey(name) }

    fun removeString(name: String) = update { it -= stringPreferencesKey(name) }

    fun removeStringSet(name: String) = update { it -= stringSetPreferencesKey(name) }

    fun keysWithPrefix(prefix: String): Set<String> {
        if (prefix.isBlank()) return state.value.asMap().keys.map { it.name }.toSet()
        return state.value
            .asMap()
            .keys
            .map { it.name }
            .filter { it.startsWith("${prefix}_") }
            .toSet()
    }

    fun putBooleans(values: Map<String, Boolean?>) = update { prefs ->
        for ((name, value) in values) {
            val key = booleanPreferencesKey(name)
            if (value == null) prefs -= key else prefs[key] = value
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        val completion = CompletableDeferred<Unit>()
        update(completion) { it[KEY_ENABLED] = enabled }
        completion.await()
    }

    private fun update(
        completion: CompletableDeferred<Unit>? = null,
        mutator: (MutablePreferences) -> Unit,
    ) {
        synchronized(lock) {
            val updated = state.value.toMutablePreferences()
            mutator(updated)
            if (loadedState.value && updated == state.value && completion == null) return
            publish(updated.toPreferences())
            if (!loadedState.value) pending.add(mutator)
            check(writes.trySend(Write(mutator, completion)).isSuccess)
        }
    }

    private fun publish(next: Preferences) {
        val keys = changedKeys(state.value, next)
        state.value = next
        if (keys.isNotEmpty()) {
            changeEvents.tryEmit(keys)
            val snapshot = synchronized(lock) { listeners.toList() }
            if (snapshot.isNotEmpty()) notifications.trySend(keys to snapshot)
        }
    }

    fun changedKeys(old: Preferences, new: Preferences): Set<String> {
        val before = old.asMap()
        val after = new.asMap()
        return (before.keys + after.keys)
            .filterTo(mutableSetOf()) { before[it] != after[it] }
            .mapTo(mutableSetOf()) { it.name }
    }

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey(PreferenceKeys.ENABLED)

        fun namespaced(prefix: String, key: String): String =
            if (prefix.isBlank()) key else "${prefix}_${key}"

        fun uidKey(prefix: String, uid: Int): String = namespaced(prefix, uid.toString())
    }
}
