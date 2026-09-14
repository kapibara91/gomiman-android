package co.jp.kpbr.gomiman.data.repository

import co.jp.kpbr.gomiman.data.local.GarbageDatabaseHelper
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GarbageRepository(
    private val dbHelper: GarbageDatabaseHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _garbageModels = MutableStateFlow<List<GarbageCollectionModel>>(emptyList())
    val garbageModels: StateFlow<List<GarbageCollectionModel>> = _garbageModels.asStateFlow()

    suspend fun loadGarbageCollections() = withContext(ioDispatcher) {
        val list = dbHelper.getAllGarbageCollections()
        _garbageModels.value = list
    }

    suspend fun insertGarbageCollection(model: GarbageCollectionModel): Long = withContext(ioDispatcher) {
        val id = dbHelper.insertGarbageCollection(model)
        loadGarbageCollections()
        id
    }

    suspend fun deleteGarbageCollection(id: Long) = withContext(ioDispatcher) {
        dbHelper.deleteGarbageCollection(id)
        loadGarbageCollections()
    }

    suspend fun deleteAllGarbageCollections() = withContext(ioDispatcher) {
        dbHelper.deleteAllGarbageCollections()
        _garbageModels.value = emptyList()
    }
}
