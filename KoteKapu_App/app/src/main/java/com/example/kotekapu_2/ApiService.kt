package com.example.kotekapu_2


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .build()

    // Регистрация
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

    // Авторизация
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

    // Получение ленты
    suspend fun getFeed(token: String): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/feed")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Лайк поста
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
            // Если произошла ошибка чтения, но ответ был успешным, попробуем создать пустой результат
            if (response.isSuccessful) {
                println("DEBUG: 🛠️ Creating empty result due to connection issue")
                try {
                    val emptyResult = FeedResponse(emptyList(), 0)
                    @Suppress("UNCHECKED_CAST")
                    return Result.success(emptyResult as T)
                } catch (e: Exception) {
                    // ignore
                }
            }
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }


    // Получение текущих интересов пользователя
    suspend fun getUserInterests(token: String, userId: Int): Result<UserInterests> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/interests")
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<UserInterests>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Получение рекомендованных постов с токеном
    suspend fun getRecommendedFeed(
        token: String,
        limit: Int = 10,
        offset: Int = 0
    ): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            // Используем GET запрос
            val url = "$BASE_URL/api/feed/recommended?limit=$limit&offset=$offset"

            println("DEBUG: Sending GET request to: $url")
            println("DEBUG: Token present: ${token.isNotEmpty()}")

            val requestBuilder = Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")

            // Добавляем токен только если он не пустой
            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            println("DEBUG: Response code: $responseCode")

            // Обрабатываем ответ
            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Exception in getRecommendedFeed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun getDebugFeed(token: String): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/debug/feed")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            println("DEBUG: Debug feed response: ${response.code}")
            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Debug feed also failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getDebugFeed(): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/debug/feed")
                .get()
                .build()

            println("DEBUG: Getting debug feed from: ${request.url}")
            val response = client.newCall(request).execute()
            println("DEBUG: Debug feed response: ${response.code}")

            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Debug feed failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Альтернативный метод через POST (если предпочтительнее)
    suspend fun getRecommendedFeedPost(
        token: String,
        userId: Int? = null,
        limit: Int = 5,
        offset: Int = 0
    ): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = FeedRequest(userId, limit, offset)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/feed/recommended")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            println("DEBUG: Sending POST request to: ${request.url}")
            println("DEBUG: Token: $token")

            val response = client.newCall(request).execute()
            println("DEBUG: Response code: ${response.code}")

            val body = response.body?.string()
            println("DEBUG: Response body: $body")

            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Exception in getRecommendedFeedPost: ${e.message}")
            Result.failure(e)
        }
    }

    // Лайк с обновлением интересов
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

    // Добавьте в ApiService.kt

    // Завершение профиля (Сценарий 2)
    suspend fun completeProfile(
        token: String,
        userId: Int,
        phone: String? = null,
        age: Int? = null,
        placement: String? = null,
        studyPlace: String? = null,
        gradeCourse: String? = null
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = CompleteProfileRequest(phone, age, placement, studyPlace, gradeCourse)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/complete-profile")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<AuthResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Получение категорий для опроса (Сценарий 2.1)
    suspend fun getPreferenceCategories(): Result<PreferencesResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/preferences/categories")
                .get()
                .build()

            val response = client.newCall(request).execute()
            handleResponse<PreferencesResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Завершение опроса предпочтений (Сценарий 2.1)
    suspend fun completePreferences(
        token: String,
        userId: Int,
        interests: List<String>,
        formats: List<String>,
        eventTypes: List<String>
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val requestBody = CompletePreferencesRequest(interests, formats, eventTypes)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/users/$userId/complete-preferences")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<AuthResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Регистрация на мероприятие (Сценарий 5)
    suspend fun registerForEvent(
        token: String,
        eventId: Int
    ): Result<EventRegistrationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/events/$eventId/register")
                .post(RequestBody.create(null, ""))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<EventRegistrationResponse>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Создание организации (Сценарий 8)
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
            val requestBody = CreateOrganisationRequest(title, description, city, avatar, tags, socialLinks)
            val jsonBody = gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url("$BASE_URL/api/organisations")
                .post(jsonBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            handleResponse<Organisation>(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Подписка на организацию
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

    suspend fun getTestFeed(): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/test/feed")
                .get()
                .build()

            println("DEBUG: Sending test request to: ${request.url}")
            val response = client.newCall(request).execute()
            println("DEBUG: Test response code: ${response.code}")

            val body = response.body?.string()
            println("DEBUG: Test response body: $body")

            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Test feed exception: ${e.message}")
            Result.failure(e)
        }
    }

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

    // Проверка связи с сервером
    suspend fun testConnection(): Result<ConnectionTestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/test/connection")
                .get()
                .build()

            println("DEBUG: Testing connection to: ${request.url}")
            val response = client.newCall(request).execute()
            println("DEBUG: Connection test response: ${response.code}")

            val body = response.body?.string()
            println("DEBUG: Connection test body: $body")

            handleResponse<ConnectionTestResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Connection test failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Получение упрощенной ленты
    suspend fun getSimpleFeed(): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/test/simple-feed")
                .get()
                .build()

            println("DEBUG: Getting simple feed from: ${request.url}")
            val response = client.newCall(request).execute()
            println("DEBUG: Simple feed response: ${response.code}")

            handleResponse<FeedResponse>(response)
        } catch (e: Exception) {
            println("DEBUG: Simple feed failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Проверка связи с сервером
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

    data class PingResponse(
        val message: String,
        val status: String
    )

}

