package com.studio.vitalroute.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio.vitalroute.data.firebase.FirestoreRepository
import com.studio.vitalroute.data.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val activity: Activity? = null,
    val showExportMenu: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleted: Boolean = false
)

class ActivityDetailViewModel(private val activityId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private val repository = FirestoreRepository()

    init {
        loadActivity()
    }

    fun showExportMenu()  { _uiState.update { it.copy(showExportMenu = true) } }
    fun hideExportMenu()  { _uiState.update { it.copy(showExportMenu = false) } }
    fun showDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = true) } }
    fun hideDeleteDialog() { _uiState.update { it.copy(showDeleteDialog = false) } }

    fun deleteActivity() {
        viewModelScope.launch {
            try {
                repository.deleteActivity(activityId)
                _uiState.update { it.copy(deleted = true, showDeleteDialog = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(showDeleteDialog = false) }
            }
        }
    }

    private fun loadActivity() {
        viewModelScope.launch {
            try {
                val activity = repository.getActivityById(activityId)
                _uiState.update { it.copy(isLoading = false, activity = activity) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        fun factory(activityId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ActivityDetailViewModel(activityId) as T
            }
    }
}
