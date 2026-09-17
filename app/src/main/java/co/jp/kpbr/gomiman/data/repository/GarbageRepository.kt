package co.jp.kpbr.gomiman.data.repository

import co.jp.kpbr.gomiman.data.local.GarbageDatabaseHelper
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GarbageRepository(
    private val preferencesManager: PreferencesManager,
    private val dbHelper: GarbageDatabaseHelper? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _garbageModels = MutableStateFlow<List<GarbageCollectionModel>>(emptyList())
    val garbageModels: StateFlow<List<GarbageCollectionModel>> = _garbageModels.asStateFlow()

    private val _isSynced = MutableStateFlow(preferencesManager.isGarbageSettingSynced())
    val isSynced: StateFlow<Boolean> = _isSynced.asStateFlow()

    private val _scheduleVersion = MutableStateFlow(preferencesManager.getGarbageScheduleVersion())
    val scheduleVersion: StateFlow<Long> = _scheduleVersion.asStateFlow()

    suspend fun loadGarbageCollections() = withContext(ioDispatcher) {
        var list = preferencesManager.getGarbageCollections()

        // One-time smooth migration from legacy SQLite database if local preferences is empty
        if (list.isEmpty() && dbHelper != null) {
            val legacyList = try {
                dbHelper.getAllGarbageCollections()
            } catch (e: Exception) {
                emptyList()
            }
            if (legacyList.isNotEmpty()) {
                list = legacyList
                preferencesManager.saveGarbageCollections(list)
            }
        }

        _garbageModels.value = list
        _isSynced.value = preferencesManager.isGarbageSettingSynced()
        _scheduleVersion.value = preferencesManager.getGarbageScheduleVersion()
    }

    suspend fun insertGarbageCollection(model: GarbageCollectionModel): Long = withContext(ioDispatcher) {
        val currentList = _garbageModels.value.toMutableList()

        // Deduplication safety check: if an identical item already exists, do not re-insert
        val existingItem = currentList.firstOrNull {
            it.weekStatus == model.weekStatus &&
            it.weeks == model.weeks &&
            it.days == model.days &&
            it.garbageTypes == model.garbageTypes
        }
        if (existingItem != null) {
            return@withContext existingItem.id ?: 0L
        }

        val newId = (currentList.maxOfOrNull { it.id ?: 0L } ?: 0L) + 1L
        val newVersion = preferencesManager.updateGarbageScheduleVersion()

        model.id = newId
        model.version = newVersion
        currentList.add(model)

        preferencesManager.saveGarbageCollections(currentList)
        preferencesManager.setGarbageSettingSynced(false)

        _garbageModels.value = currentList
        _isSynced.value = false
        _scheduleVersion.value = newVersion
        newId
    }

    suspend fun deleteGarbageCollection(id: Long) = withContext(ioDispatcher) {
        val currentList = _garbageModels.value.filter { it.id != id }
        val newVersion = preferencesManager.updateGarbageScheduleVersion()

        preferencesManager.saveGarbageCollections(currentList)
        preferencesManager.setGarbageSettingSynced(false)

        _garbageModels.value = currentList
        _isSynced.value = false
        _scheduleVersion.value = newVersion
    }

    suspend fun deleteAllGarbageCollections() = withContext(ioDispatcher) {
        val newVersion = preferencesManager.updateGarbageScheduleVersion()

        preferencesManager.saveGarbageCollections(emptyList())
        preferencesManager.setGarbageSettingSynced(false)

        _garbageModels.value = emptyList()
        _isSynced.value = false
        _scheduleVersion.value = newVersion
    }

    suspend fun markSynced(synced: Boolean) = withContext(ioDispatcher) {
        preferencesManager.setGarbageSettingSynced(synced)
        _isSynced.value = synced
    }

    suspend fun syncWithServer(syncRepository: SyncRepository): Result<Unit> = withContext(ioDispatcher) {
        val currentCollections = _garbageModels.value
        val currentVersion = _scheduleVersion.value
        val result = syncRepository.syncGarbageSetting(currentCollections, currentVersion)
        if (result.isSuccess) {
            markSynced(true)
        }
        result
    }
}
