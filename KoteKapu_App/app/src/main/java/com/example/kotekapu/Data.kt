import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val format: EventFormat,
    val organization: Organization,
    val tags: List<String>,
    val isLiked: Boolean = false,
    val imageUrl: String? = null,
    val registrationOpen: Boolean = true,
    val participantsCount: Int = 0,
    val location: String = "",
    val onlineLink: String = "",
    val schedule: List<ScheduleItems> = emptyList(),
    val additionalInfo: String = "",
    val price: Int = 0
)

data class ScheduleItems(
    val time: String,
    val title: String,
    val description: String = ""
)

enum class EventFormat(val displayName: String) {
    ONLINE("Онлайн"),
    OFFLINE("Офлайн"),
    HYBRID("Гибрид")
}





// Пример данных для детальной страницы
val sampleDetailedEvent = Event(
    id = "1",
    title = "Хакатон по мобильной разработке",
    description = "Приглашаем всех разработчиков мобильных приложений на ежегодный хакатон! Вас ждут интересные задачи, менторская поддержка и ценные призы. Мероприятие подойдет как начинающим, так и опытным разработчикам. Мы предоставим все необходимые ресурсы и поддержку для реализации ваших идей.",
    date = "15 декабря, 10:00",
    format = EventFormat.HYBRID,
    organization = Organization(
        id = "1",
        name = "IT Community",
        eventsCount = 24
    ),
    tags = listOf("Технологии", "Хакатон", "Разработка", "Mobile", "Android", "iOS"),
    isLiked = true,
    registrationOpen = true,
    participantsCount = 147,
    location = "Москва, ул. Тверская, 15",
    onlineLink = "meet.google.com/abc-def-ghi",
    schedule = listOf(
        ScheduleItems("10:00", "Регистрация", "Приветственный кофе"),
        ScheduleItems("11:00", "Открытие", "Представление задач и правил"),
        ScheduleItems("12:00", "Начало кодинга", "Работа над проектами"),
        ScheduleItems("18:00", "Презентация проектов", "Демонстрация результатов"),
        ScheduleItems("20:00", "Награждение", "Вручение призов")
    ),
    additionalInfo = "При себе иметь ноутбук. Еда и напитки предоставляются.",
    price = 0
)




// Пример данных для preview
val sampleEvents = listOf(
    Event(
        id = "1",
        title = "Хакатон по мобильной разработке",
        description = "Соревнование для разработчиков мобильных приложений",
        date = "15 дек, 10:00",
        format = EventFormat.HYBRID,
        organization = Organization("1", "IT Community"),
        tags = listOf("Технологии", "Хакатон", "Разработка"),
        isLiked = true
    ),
    Event(
        id = "2",
        title = "Лекция об искусственном интеллекте",
        description = "Обсуждение современных трендов в AI",
        date = "20 дек, 19:00",
        format = EventFormat.ONLINE,
        organization = Organization("2", "AI Research Lab"),
        tags = listOf("Искусственный интеллект", "Наука", "Лекция")
    ),
    Event(
        id = "3",
        title = "Воркшоп по дизайну интерфейсов",
        description = "Практическое занятие по UI/UX дизайну",
        date = "18 дек, 15:00",
        format = EventFormat.OFFLINE,
        organization = Organization("3", "Design Studio"),
        tags = listOf("Дизайн", "UI/UX", "Воркшоп")
    )
)




data class Review(
    val id: String,
    val eventTitle: String,
    val text: String,
    val rating: Int,
    val date: String,
    val authorName: String = ""
)



// Пример данных
val sampleUser = User(
    id = "1",
    name = "Александр Иванов",
    email = "alex@example.com",
    status = "Активный участник",
    bio = "Увлекаюсь мобильной разработкой и AI. Люблю посещать технологические мероприятия.",
    city = "Москва",
    education = "МГТУ им. Баумана, 3 курс",
    phone = "+7 (999) 123-45-67",
    eventsCount = 15,
    organizationsCount = 3,
    followersCount = 42,
    upcomingEvents = sampleEvents.take(2),
    organizations = listOf(
        Organization("1", "IT Community", eventsCount = 24),
        Organization("2", "AI Research Lab", eventsCount = 12)
    ),
    reviews = listOf(
        Review(
            id = "1",
            eventTitle = "Хакатон по мобильной разработке",
            text = "Отличное мероприятие! Организация на высшем уровне, интересные задачи.",
            rating = 5,
            date = "15 дек 2023"
        )
    )
)

val sampleOrganization = Organization(
    id = "1",
    name = "IT Community",
    description = "Сообщество разработчиков и IT-энтузиастов. Организуем митапы, хакатоны и воркшопы.",
    status = OrganizationStatus.ACTIVE,
    isSubscribed = false,
    eventsCount = 24,
    subscribersCount = 1500,
    rating = 4.8,
    tags = listOf("Технологии", "Разработка", "Хакатоны", "Митапы"),
    upcomingEvents = sampleEvents,
    pastEvents = sampleEvents.drop(2)
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val status: String = "",
    val bio: String = "",
    val city: String = "",
    val education: String = "",
    val phone: String = "",
    val eventsCount: Int = 0,
    val organizationsCount: Int = 0,
    val followersCount: Int = 0,
    val reviewsCount: Int = 0,
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val organizations: List<Organization> = emptyList(),
    val recentActivities: List<UserActivity> = emptyList(),
    val isEmailPublic: Boolean = false,
    val isPhonePublic: Boolean = false,
    val isEventsPublic: Boolean = true,
    val reviews: List<Review> = emptyList()
)

data class UserActivity(
    val id: String,
    val type: ActivityType,
    val description: String,
    val time: String,
    val targetId: String = "",
    val icon: ImageVector
)

enum class ActivityType {
    EVENT,
    ORGANIZATION,
    REVIEW,
    ACHIEVEMENT
}

// Пример данных для личного профиля
val samplePersonalUser = User(
    id = "1",
    name = "Александр Иванов",
    email = "alex@example.com",
    status = "Активный участник",
    bio = "Увлекаюсь мобильной разработкой и AI. Люблю посещать технологические мероприятия и хакатоны.",
    city = "Москва",
    education = "МГТУ им. Баумана, 3 курс",
    phone = "+7 (999) 123-45-67",
    eventsCount = 15,
    organizationsCount = 3,
    followersCount = 42,
    reviewsCount = 8,
    upcomingEvents = sampleEvents.take(2),
    pastEvents = sampleEvents.drop(2),
    organizations = listOf(
        Organization("1", "IT Community", eventsCount = 24),
        Organization("2", "AI Research Lab", eventsCount = 12)
    ),
    recentActivities = listOf(
        UserActivity(
            id = "1",
            type = ActivityType.EVENT,
            description = "Вы зарегистрировались на хакатон",
            time = "2 часа назад",
            targetId = "1",
            icon = Icons.Default.DateRange
        ),
        UserActivity(
            id = "2",
            type = ActivityType.REVIEW,
            description = "Вы оставили отзыв о мероприятии",
            time = "1 день назад",
            icon = Icons.Default.Star
        ),
        UserActivity(
            id = "3",
            type = ActivityType.ORGANIZATION,
            description = "Вы создали организацию IT Community",
            time = "3 дня назад",
            targetId = "1",
            icon = Icons.Default.Person
        )
    )
)

data class OrganizationData(
    val name: String = "",
    val description: String = "",
    val city: String = "",
    val tags: List<String> = emptyList(),
    val socialLinks: SocialLinks = SocialLinks(),
    val avatarUrl: String? = null
) {
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                description.isNotBlank() &&
                description.length in 20..500 &&
                city.isNotBlank() &&
                tags.isNotEmpty()
    }
}

data class SocialLinks(
    val website: String = "",
    val vk: String = "",
    val telegram: String = "",
    val instagram: String = ""
)

data class EventData(
    val title: String = "",
    val description: String = "",
    val format: EventFormat = EventFormat.OFFLINE,
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val onlineLink: String = "",
    val maxParticipants: Int = 0,
    val price: Int = 0,
    val tags: List<String> = emptyList(),
    val schedule: List<ScheduleItems> = emptyList(),
    val additionalInfo: String = "",
    val registrationOpen: Boolean = true
) {
    fun isValid(): Boolean {
        return title.isNotBlank() &&
                description.isNotBlank() &&
                date.isNotBlank() &&
                time.isNotBlank() &&
                tags.isNotEmpty() &&
                (format != EventFormat.OFFLINE || location.isNotBlank()) &&
                (format != EventFormat.ONLINE || onlineLink.isNotBlank())
    }
}

data class Organization(
    val id: String,
    val name: String,
    val description: String = "",
    val avatarUrl: String? = null,
    val status: OrganizationStatus = OrganizationStatus.ACTIVE,
    val isSubscribed: Boolean = false,
    val eventsCount: Int = 0,
    val subscribersCount: Int = 0,
    val rating: Double = 0.0,
    val tags: List<String> = emptyList(),
    val upcomingEvents: List<Event> = emptyList(),
    val pastEvents: List<Event> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val city: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val website: String = "",
    val vkLink: String = "",
    val telegramLink: String = "",
    val instagramLink: String = "",
    val rejectionReason: String = ""
)

enum class OrganizationStatus(val displayName: String) {
    ACTIVE("Активна"),
    PENDING("На проверке"),
    REJECTED("Отклонена")
}

data class EventAnalytics(
    val eventId: String,
    val registrationsCount: Int,
    val attendedCount: Int,
    val attendanceRate: Int,
    val likesCount: Int,
    val viewsCount: Int,
    val revenue: Int = 0,
    val paidParticipants: Int = 0,
    val roi: Int = 0,
    val averageRating: Double = 0.0,
    val recommendationScore: Int = 0,
    val registrationTrend: Trend = Trend.STABLE,
    val attendanceTrend: Trend = Trend.STABLE,
    val likesTrend: Trend = Trend.STABLE,
    val viewsTrend: Trend = Trend.STABLE,
    val registrationTimeline: List<RegistrationDay> = emptyList(),
    val sources: Map<String, Int> = emptyMap(),
    val cities: Map<String, Int> = emptyMap(),
    val ageGroups: Map<String, Int> = emptyMap(),
    val participants: List<EventParticipant> = emptyList(),
    val promotionChannels: List<PromotionChannel> = emptyList(),
    val recentReviews: List<EventReview> = emptyList(),
    val recommendations: List<Recommendation> = emptyList()
)

data class RegistrationDay(
    val date: String,
    val count: Int
)

data class EventParticipant(
    val id: String,
    val name: String,
    val city: String,
    val registrationDate: String,
    val attended: Boolean = false,
    val paid: Boolean = false
)

data class PromotionChannel(
    val name: String,
    val icon: ImageVector,
    val registrations: Int,
    val ctr: Double,
    val conversionRate: Double
)

data class EventReview(
    val id: String,
    val authorName: String,
    val rating: Int,
    val text: String,
    val date: String
)

data class Recommendation(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val priority: Priority
)

enum class Trend {
    UP, DOWN, STABLE, NEW
}

enum class Priority {
    HIGH, MEDIUM, LOW
}

// Пример данных
val sampleEventAnalytics = EventAnalytics(
    eventId = "1",
    registrationsCount = 147,
    attendedCount = 132,
    attendanceRate = 90,
    likesCount = 89,
    viewsCount = 543,
    revenue = 29400,
    paidParticipants = 98,
    roi = 45,
    averageRating = 4.7,
    recommendationScore = 82,
    registrationTrend = Trend.UP,
    attendanceTrend = Trend.UP,
    likesTrend = Trend.STABLE,
    viewsTrend = Trend.UP,
    registrationTimeline = listOf(
        RegistrationDay("1 дек", 15),
        RegistrationDay("2 дек", 28),
        RegistrationDay("3 дек", 42),
        RegistrationDay("4 дек", 36),
        RegistrationDay("5 дек", 26)
    ),
    sources = mapOf(
        "feed" to 89,
        "search" to 34,
        "direct" to 24
    ),
    cities = mapOf(
        "Москва" to 67,
        "Санкт-Петербург" to 34,
        "Новосибирск" to 12,
        "Екатеринбург" to 10,
        "Казань" to 8
    ),
    ageGroups = mapOf(
        "18-24" to 45,
        "25-34" to 67,
        "35-44" to 28,
        "45+" to 7
    ),
    participants = List(147) { index ->
        EventParticipant(
            id = "user_$index",
            name = "Участник ${index + 1}",
            city = listOf("Москва", "СПб", "Новосибирск", "Екатеринбург").random(),
            registrationDate = "2023-12-${(index % 5) + 1}",
            attended = index < 132,
            paid = index < 98
        )
    },
    promotionChannels = listOf(
        PromotionChannel(
            name = "Лента рекомендаций",
            icon = Icons.Default.Home,
            registrations = 89,
            ctr = 3.2,
            conversionRate = 15.6
        ),
        PromotionChannel(
            name = "Соцсети",
            icon = Icons.Default.MoreVert,
            registrations = 34,
            ctr = 2.1,
            conversionRate = 12.3
        ),
        PromotionChannel(
            name = "Email рассылка",
            icon = Icons.Default.Email,
            registrations = 12,
            ctr = 8.7,
            conversionRate = 22.1
        )
    ),
    recentReviews = listOf(
        EventReview(
            id = "1",
            authorName = "Александр И.",
            rating = 5,
            text = "Отличная организация, все понравилось!",
            date = "2023-12-15"
        ),
        EventReview(
            id = "2",
            authorName = "Мария К.",
            rating = 4,
            text = "Хорошее мероприятие, но мало практики",
            date = "2023-12-14"
        )
    ),
    recommendations = listOf(
        Recommendation(
            title = "Увеличьте охват",
            description = "Рекомендуем продвигать в соцсетях за 2 недели до события",
            icon = Icons.Default.Done,
            priority = Priority.HIGH
        ),
        Recommendation(
            title = "Улучшите контент",
            description = "Добавьте больше фото и видео с прошлых мероприятий",
            icon = Icons.Default.AccountCircle,
            priority = Priority.MEDIUM
        )
    )
)
