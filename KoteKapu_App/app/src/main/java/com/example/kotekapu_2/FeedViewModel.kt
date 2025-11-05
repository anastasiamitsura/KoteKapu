package com.example.kotekapu_2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val apiService: ApiService,
    private val authManager: AuthManager
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 5

    init {
        loadInitialFeed()
    }

    fun loadInitialFeed() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val token = authManager.getCurrentToken()
                if (token != null) {
                    val result = apiService.getRecommendedFeed(
                        token = token,
                        limit = pageSize,
                        offset = 0
                    )

                    if (result.isSuccess) {
                        _posts.value = result.getOrNull()?.posts ?: emptyList()
                        currentOffset = pageSize
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Ошибка загрузки ленты"
                    }
                } else {
                    _error.value = "Токен не найден"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка сети: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMorePosts() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val token = authManager.getCurrentToken()
                if (token != null) {
                    val result = apiService.getRecommendedFeed(
                        token = token,
                        limit = pageSize,
                        offset = currentOffset
                    )

                    if (result.isSuccess) {
                        val newPosts = result.getOrNull()?.posts ?: emptyList()
                        _posts.value = _posts.value + newPosts
                        currentOffset += pageSize
                    } else {
                        _error.value = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                    }
                } else {
                    _error.value = "Токен не найден"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка сети: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun likePost(post: Post) {
        viewModelScope.launch {
            try {
                val token = authManager.getCurrentToken()
                val userId = authManager.getCurrentUserId()

                if (token != null && userId != null) {
                    val result = apiService.likePostWithInterests(
                        token = token,
                        postId = post.id,
                        userId = userId,
                        interestTags = post.interest_tags,
                        formatTags = post.format_tags
                    )

                    if (result.isSuccess) {
                        val updatedPosts = _posts.value.map { currentPost ->
                            if (currentPost.id == post.id) {
                                currentPost.copy(likes = currentPost.likes + 1)
                            } else {
                                currentPost
                            }
                        }
                        _posts.value = updatedPosts

                        // Можно также обновить интересы пользователя
                        updateUserInterests(token, userId)
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Error liking post: ${e.message}")
            }
        }
    }

    private suspend fun updateUserInterests(token: String, userId: Int) {
        val interestsResult = apiService.getUserInterests(token, userId)
        if (interestsResult.isSuccess) {
            println("DEBUG: User interests updated")
        }
    }

    fun clearError() {
        _error.value = null
    }
}