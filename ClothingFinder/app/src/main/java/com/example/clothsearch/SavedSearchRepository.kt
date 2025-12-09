package com.example.clothsearch

class SavedSearchRepository(private val dao: SavedSearchDao) {
    val all = dao.observeAll()
    suspend fun save(search: SavedSearch) = dao.insert(search)
    suspend fun delete(search: SavedSearch) = dao.delete(search)
}
