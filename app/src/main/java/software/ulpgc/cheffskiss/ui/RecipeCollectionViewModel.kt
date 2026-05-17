package software.ulpgc.cheffskiss.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.ulpgc.cheffskiss.application.control.CreateRecipeCollectionCommand
import software.ulpgc.cheffskiss.application.control.CreateRecipeCollectionInput
import software.ulpgc.cheffskiss.application.control.DeleteRecipeCollectionCommand
import software.ulpgc.cheffskiss.application.control.DeleteRecipeCollectionCommandInput
import software.ulpgc.cheffskiss.application.port.CurrentUserPort
import software.ulpgc.cheffskiss.application.port.ImageStorage
import software.ulpgc.cheffskiss.application.port.RecipeCollectionRepository
import software.ulpgc.cheffskiss.application.services.GetRecipeCollectionQuery
import software.ulpgc.cheffskiss.application.services.ImagePersistence
import software.ulpgc.cheffskiss.domain.model.RecipeCollection
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseAuthenticationService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.FirebaseRecipeCollectionService
import software.ulpgc.cheffskiss.infrastructure.adapter.output.ImageStorageFactory
import java.util.UUID

data class RecipeCollectionUiState(
    val isLoading: Boolean = true,
    val collections: List<RecipeCollection> = emptyList(),
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createName: String = "",
    val createNameError: String? = null,
    val createImage: String = "",
    val isCreating: Boolean = false,
)

class RecipeCollectionViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val port: RecipeCollectionRepository = FirebaseRecipeCollectionService()
    private val currentUserPort: CurrentUserPort = FirebaseAuthenticationService()
    private val imageStorage: ImageStorage by lazy { ImageStorageFactory.create(getApplication()) }

    private val _uiState = MutableStateFlow(RecipeCollectionUiState())
    val uiState: StateFlow<RecipeCollectionUiState> = _uiState.asStateFlow()

    private val userUuid: UUID?
        get() = currentUserPort.getCurrentUser()
            ?.let { UUID.nameUUIDFromBytes(it.toByteArray()) }

    init { load() }

    fun load() {
        val uid = userUuid ?: run {
            _uiState.update { it.copy(isLoading = false, error = "No authenticated user") }
            return
        }
        viewModelScope.launch {
            GetRecipeCollectionQuery(port)(uid)
                .onStart { _uiState.update { it.copy(isLoading = true, error = null) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { cols -> _uiState.update { it.copy(isLoading = false, collections = cols) } }
        }
    }

    fun showCreateDialog() =
        _uiState.update { it.copy(showCreateDialog = true, createName = "", createNameError = null) }

    fun hideCreateDialog() =
        _uiState.update { it.copy(showCreateDialog = false) }

    fun onCreateNameChange(name: String) =
        _uiState.update { it.copy(createName = name, createNameError = null) }

    fun onCreateImageChange(image: String) =
        _uiState.update { it.copy(createImage = image) }

    fun createCollection() {
        val uid = userUuid ?: return
        val name = _uiState.value.createName.trim()
        val image = _uiState.value.createImage

        if (name.isBlank()) {
            _uiState.update { it.copy(createNameError = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            runCatching {
                val imageUrl = ImagePersistence.persistIfLocal(
                    imageStorage = imageStorage,
                    source = image,
                    folder = uid.toString(),
                    fileName = "collection_cover.jpg",
                )
                CreateRecipeCollectionCommand(
                    port = port,
                    input = object : CreateRecipeCollectionInput {
                        override fun userId() = uid
                        override fun name() = name
                        override fun image() = imageUrl
                    },
                ).execute()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isCreating = false, showCreateDialog = false) }
        }
    }

    fun deleteCollection(collectionID: UUID, userId: UUID) {
        viewModelScope.launch {
            runCatching {
                DeleteRecipeCollectionCommand(
                    port = port,
                    input = object : DeleteRecipeCollectionCommandInput {
                        override fun id() = collectionID
                        override fun userId() = userId
                    },
                ).execute()
            }.onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}
