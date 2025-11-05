package com.example.kotekapu_2

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ApiService {

    companion object {
        private const val BASE_URL = "http://192.168.0.111:5000"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val gson = Gson()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // ИЗМЕНИТЬ на BODY для полной отладки
        })
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .build()

    // === АВТОРИЗАЦИЯ И РЕГИСТРАЦИЯ ===

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Starting registration for: $email")

            val requestBody = RegisterRequest(email, password, firstName, lastName)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/register")
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<AuthResponse>(response)

            println("DEBUG: Registration result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Registration exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Starting login for: $email")

            val requestBody = LoginRequest(email, password)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/login")
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<AuthResponse>(response)

            println("DEBUG: Login result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Login exception: ${e.message}")
            Result.failure(e)
        }
    }

    // === ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ ===

    suspend fun getUserProfile(token: String, userId: Int): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Getting user profile for userId: $userId")

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/profile")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<UserProfileResponse>(response)

            println("DEBUG: User profile result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Get user profile exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun completeProfile(
        token: String,
        userId: Int,
        phone: String? = null,
        age_user: Int? = null,
        placement: String? = null,
        studyPlace: String? = null,
        gradeCourse: String? = null
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Completing profile for userId: $userId")

            val requestBody = CompleteProfileRequest(phone, age_user, placement, studyPlace, gradeCourse)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/complete-profile")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<AuthResponse>(response)

            println("DEBUG: Complete profile result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Complete profile exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        token: String,
        userId: Int,
        phone: String? = null,
        age_user: Int? = null,
        placement: String? = null,
        studyPlace: String? = null,
        gradeCourse: String? = null,
        avatar: String? = null
    ): Result<UserProfileResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Updating profile for userId: $userId")

            val requestBody = UpdateProfileRequest(phone, age_user, placement, studyPlace, gradeCourse, avatar)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/profile")
                .put(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<UserProfileResponse>(response)

            println("DEBUG: Update profile result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Update profile exception: ${e.message}")
            Result.failure(e)
        }
    }

    // === ПРЕДПОЧТЕНИЯ И ИНТЕРЕСЫ ===

    suspend fun getPreferenceCategories(): Result<PreferencesResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Getting preference categories")

            val request = Request.Builder()
                .url("$BASE_URL/api/preferences/categories")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<PreferencesResponse>(response)

            println("DEBUG: Preference categories result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Get preference categories exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun completePreferences(
        token: String,
        userId: Int,
        interests: List<String>,
        formats: List<String>,
        eventTypes: List<String>
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Completing preferences for userId: $userId")

            val requestBody = CompletePreferencesRequest(interests, formats, eventTypes)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/complete-preferences")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<AuthResponse>(response)

            println("DEBUG: Complete preferences result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Complete preferences exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserInterests(token: String, userId: Int): Result<UserInterests> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/interests")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<UserInterests>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === ЛЕНТА И МЕРОПРИЯТИЯ ===

    suspend fun getFeed(token: String): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/feed")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecommendedFeed(
        token: String,
        limit: Int = 10,
        offset: Int = 0
    ): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/feed/recommended?limit=$limit&offset=$offset"

            println("DEBUG: Sending GET request to: $url")
            println("DEBUG: Token present: ${token.isNotEmpty()}")

            val requestBuilder = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")

            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            println("DEBUG: Response code: ${response.code}")
            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Exception in getRecommendedFeed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getEventDetails(token: String, eventId: Int): Result<EventDetailResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Getting event details for eventId: $eventId")

            val request = Request.Builder()
                .url("$BASE_URL/api/events/$eventId")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<EventDetailResponse>(response)

            println("DEBUG: Event details result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Get event details exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun registerForEvent(
        token: String,
        eventId: Int
    ): Result<EventRegistrationResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Registering for event: $eventId")

            val request = Request.Builder()
                .url("$BASE_URL/api/events/$eventId/register")
                .post(RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<EventRegistrationResponse>(response)

            println("DEBUG: Event registration result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Event registration exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserEvents(token: String, userId: Int): Result<UserEventsResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Getting user events for userId: $userId")

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/events")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<UserEventsResponse>(response)

            println("DEBUG: User events result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Get user events exception: ${e.message}")
            Result.failure(e)
        }
    }

    // === ЛАЙКИ ===

    suspend fun likePost(token: String, postId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/posts/like/$postId")
                .post(RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to like post: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePostWithInterests(
        token: String,
        postId: Int,
        userId: Int,
        interestTags: List<String>,
        formatTags: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestBody = LikeRequest(userId, postId, interestTags, formatTags)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/posts/like")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to like post: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === ОРГАНИЗАЦИИ ===

    suspend fun createOrganisation(
        token: String,
        title: String,
        description: String,
        city: String? = null,
        avatar: String? = null,
        tags: List<String> = emptyList(),
        socialLinks: List<String> = emptyList()
    ): Result<Organisation> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Creating organisation: $title")

            val requestBody = CreateOrganisationRequest(title, description, city, avatar, tags, socialLinks)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/organisations")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<Organisation>(response)

            println("DEBUG: Create organisation result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Create organisation exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun subscribeToOrganisation(
        token: String,
        organisationId: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/organisations/$organisationId/subscribe")
                .post(RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to subscribe: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrganizationEvents(
        token: String? = null,
        organisationId: Int,
        limit: Int = 20,
        offset: Int = 0
    ): Result<OrganizationEventsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/organisations/$organisationId/events?limit=$limit&offset=$offset"
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = requestBuilder.build().let { client.newCall(it).execute() }
            handleResponse<OrganizationEventsResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === СОЗДАНИЕ МЕРОПРИЯТИЙ ===

    suspend fun createEvent(
        token: String,
        organisationId: Int,
        title: String,
        description: String,
        dateTime: String,
        location: String? = null,
        eventType: String? = null,
        interestTags: List<String> = emptyList(),
        formatTags: List<String> = emptyList(),
        picture: String? = null
    ): Result<CreateEventResponse> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Creating event: $title")

            val requestBody = CreateEventRequest(
                title = title,
                description = description,
                date_time = dateTime,
                location = location,
                event_type = eventType,
                interest_tags = interestTags,
                format_tags = formatTags,
                pic = picture
            )

            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/organisations/$organisationId/events")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val result = handleResponse<CreateEventResponse>(response)

            println("DEBUG: Create event result: $result")
            result
        } catch (e: Exception) {
            println("DEBUG: Create event exception: ${e.message}")
            Result.failure(e)
        }
    }

    // === ПОИСК ===

    suspend fun search(
        token: String? = null,
        request: SearchRequest
    ): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(request).toRequestBody(JSON_MEDIA_TYPE)

            val requestBuilder = Request.Builder()
                .url("$BASE_URL/api/search")
                .post(jsonBody)
                .addHeader("Content-Type", "application/json")

            token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = requestBuilder.build().let { client.newCall(it).execute() }
            handleResponse<SearchResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSearchSuggestions(token: String? = null): Result<SearchSuggestions> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("$BASE_URL/api/search/suggestions")
                .get()

            token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = requestBuilder.build().let { client.newCall(it).execute() }
            handleResponse<SearchSuggestions>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === ТЕСТИРОВАНИЕ И ДЕБАГ ===

    suspend fun checkServerConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful
            println("DEBUG: Server connection check: $isSuccess")
            Result.success(isSuccess)
        } catch (e: Exception) {
            println("DEBUG: Server connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun ping(): Result<PingResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/ping")
                .get()
                .build()

            println("DEBUG: Pinging server: ${request.url}")
            val response = client.newCall(request).execute()
            println("DEBUG: Ping response: ${response.code}")

            val body = response.body?.string()
            println("DEBUG: Ping response body: $body")

            handleResponse<PingResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Ping failed: ${e.message}")
            Result.failure(e)
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private inline fun <reified T> handleResponse(response: Response): Result<T> {
        return try {
            val body = response.body?.string()
            println("DEBUG: Response code: ${response.code}, body length: ${body?.length}")

            if (response.isSuccessful && body != null) {
                try {
                    val result = gson.fromJson(body, T::class.java)
                    println("DEBUG: ✅ Successfully parsed response")
                    Result.success(result)
                } catch (e: Exception) {
                    println("DEBUG: ❌ JSON parsing error: ${e.message}")
                    println("DEBUG: Problematic JSON: ${body.take(500)}...")
                    Result.failure(Exception("Ошибка обработки данных: ${e.message}"))
                }
            } else {
                val error = when (response.code) {
                    500 -> "Внутренняя ошибка сервера"
                    401 -> "Неавторизован"
                    404 -> "Ресурс не найден"
                    else -> "Ошибка сервера: ${response.code}"
                }
                println("DEBUG: Server error: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            println("DEBUG: 💥 Handle response exception: ${e.message}")
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }
}