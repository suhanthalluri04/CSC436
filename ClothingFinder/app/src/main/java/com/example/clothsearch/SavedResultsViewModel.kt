package com.example.clothsearch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedResultsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: SavedSearchRepository =
        SavedSearchRepository(AppDatabase.get(app).savedSearchDao())

    val saved: StateFlow<List<SavedSearch>> = repo.all.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun save(search: SavedSearch) {
        viewModelScope.launch { repo.save(search) }
    }

    fun delete(search: SavedSearch) {
        viewModelScope.launch { repo.delete(search) }
    }
}
