package com.example.kotekapu_2

// Модели для API
data class AuthResponse(
    val message: String,
    val user: User,
    val access_token: String
)

data class User(
    val id: Int,
    val email: String,
    val first_name: String,
    val last_name: String,
    val age_user: Int?,
    val placement: String?,
    val study_place: String?,
    val exp: Int,
    val avatar: String?,
    val interests_metrics: Map<String, Double>,
    val format_metrics: Map<String, Double>,
    val feed_metrics: Map<String, Any>,
    val created_at: String?
)

data class Post(
    val id: Int,
    val title: String,
    val description: String,
    val date_time: String? = null,
    val created_at: String? = null,
    val pic: String? = null,
    val interest_tags: List<String> = emptyList(),
    val format_tags: List<String> = emptyList(),
    val organization_id: Int? = null,
    val author_id: Int? = null,
    val type: String = "event",
    val relevance_score: Double? = null,
    val likes: Int = 0,
    val organization_name: String? = null,
    val organization_avatar: String? = null,
    val location: String? = null,
    val event_type: String? = null
)


data class RegisterRequest(
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ErrorResponse(
    val error: String
)

// Добавьте в существующие модели или создайте новые

data class UserInterests(
    val interests_metrics: Map<String, Double> = emptyMap(),
    val format_metrics: Map<String, Double> = emptyMap()
)

data class FeedRequest(
    val user_id: Int? = null,
    val limit: Int = 5,
    val offset: Int = 0
)

data class LikeRequest(
    val user_id: Int,
    val post_id: Int,
    val interest_tags: List<String>,
    val format_tags: List<String>
)

data class CompleteProfileRequest(
    val phone: String? = null,
    val age_user: Int? = null,
    val placement: String? = null,
    val study_place: String? = null,
    val grade_course: String? = null
)

data class CompletePreferencesRequest(
    val interests: List<String>,
    val formats: List<String>,
    val event_types: List<String>
)

data class PreferencesResponse(
    val interest_categories: List<String>,
    val format_types: List<String>,
    val event_types: List<String>
)

data class EventRegistrationResponse(
    val message: String,
    val event: Post
)

data class Organisation(
    val id: Int,
    val title: String,
    val description: String,
    val avatar: String?,
    val city: String?,
    val status: String,
    val tags: List<String>,
    val social_links: List<String>,
    val events_count: Int,
    val subscribers_count: Int,
    val owner_id: Int
)

data class CreateOrganisationRequest(
    val title: String,
    val description: String,
    val city: String? = null,
    val avatar: String? = null,
    val tags: List<String> = emptyList(),
    val social_links: List<String> = emptyList()
)

data class ConnectionTestResponse(
    val message: String,
    val status: String,
    val timestamp: String
)

data class FeedResponse(
    val posts: List<Post>,
    val count: Int = 0,
    val total: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    val has_more: Boolean? = null,  // Добавляем флаг
    val message: String? = null
)