package com.example.kotekapu_2

import androidx.compose.ui.graphics.vector.ImageVector


data class AuthResponse(
    val message: String,
    val user: User,
    val access_token: String
)

data class EventDetailResponse(
    val event: Post? = null,
    val success: Boolean = false,
    val message: String? = null
)


data class User(
    val id: Int = 0,
    val email: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val phone: String? = null,
    val age_user: Int? = null,
    val placement: String? = null,
    val study_place: String? = null,
    val grade_course: String? = null,
    val exp: Int = 0,
    val avatar: String? = null,
    val profile_completed: Boolean = false,
    val preferences_completed: Boolean = false,
    val interests_metrics: Map<String, Double> = emptyMap(),
    val format_metrics: Map<String, Double> = emptyMap(),
    val event_type_metrics: Map<String, Double> = emptyMap(),
    val feed_metrics: Map<String, Any> = emptyMap(),
    val created_at: String = ""
)

fun User.safeIsProfileCompleted(): Boolean = this.profile_completed ?: false
fun User.safeIsPreferencesCompleted(): Boolean = this.preferences_completed ?: false

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

data class UserProfileResponse(
    val user: User,
    val stats: UserStats,
    val achievements: List<Achievement>
)

data class UserStats(
    val events_attended: Int = 0,
    val events_created: Int = 0,
    val organizations_count: Int = 0,
    val likes_given: Int = 0,
    val exp: Int = 0,
    val level: Int = 1
)

data class UserEventsResponse(
    val upcoming_events: List<Post> = emptyList(),
    val past_events: List<Post> = emptyList(),
    val created_events: List<Post> = emptyList()
) {

    constructor() : this(emptyList(), emptyList(), emptyList())
}

data class UpdateProfileRequest(
    val phone: String? = null,
    val age_user: Int? = null,
    val placement: String? = null,
    val study_place: String? = null,
    val grade_course: String? = null,
    val avatar: String? = null
)

data class Achievement(
    val id: Int,
    val name: String,
    val description: String,
    val points: Int,
    val earned_at: String? = null
)

data class SearchRequest(
    val query: String? = null,
    val filters: SearchFilters? = null,
    val limit: Int = 20,
    val offset: Int = 0
)

data class SearchFilters(
    val interests: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val event_types: List<String> = emptyList(),
    val date_from: String? = null,
    val date_to: String? = null,
    val location: String? = null,
    val organization_id: Int? = null
)

data class SearchResponse(
    val events: List<Post>,
    val organizations: List<Organisation>,
    val total_events: Int,
    val total_organizations: Int
)

data class SearchSuggestions(
    val popular_searches: List<String>,
    val recent_searches: List<String>,
    val popular_tags: List<String>
)

data class PingResponse(
    val message: String,
    val status: String
)


data class CreateEventRequest(
    val title: String,
    val description: String,
    val date_time: String,
    val location: String? = null,
    val event_type: String? = null,
    val interest_tags: List<String> = emptyList(),
    val format_tags: List<String> = emptyList(),
    val pic: String? = null
)

data class CreateEventResponse(
    val message: String,
    val event: Post
)

data class OrganizationEventsResponse(
    val events: List<Post>,
    val total: Int,
    val organization: Organisation
)

data class FAQItem(
    val question: String,
    val answer: String
)

data class ContactItem(
    val type: String,
    val value: String,
    val icon: ImageVector
)

