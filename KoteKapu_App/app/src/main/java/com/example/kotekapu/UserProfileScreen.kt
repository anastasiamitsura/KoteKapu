import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    user: User,
    isOwnProfile: Boolean = false,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOrganizationClick: (String) -> Unit,
    onEventClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Мероприятия", "Организации", "Отчивки")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isOwnProfile) {
                FloatingActionButton(
                    onClick = onEditClick,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Хедер профиля
            ProfileHeader(user = user, isOwnProfile = isOwnProfile)

            // Статистика
            ProfileStats(user = user)

            // Информация
            ProfileInfo(user = user)

            // Табы
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Контент табов
            when (selectedTab) {
                0 -> EventsTab(
                    events = user.upcomingEvents,
                    onEventClick = onEventClick
                )
                1 -> OrganizationsTab(
                    organizations = user.organizations,
                    onOrganizationClick = onOrganizationClick
                )
                2 -> ReviewsTab(reviews = user.reviews)
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User, isOwnProfile: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Аватар
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.take(2).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Имя и статус
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        if (user.status.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {
                Text(
                    text = user.status,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Описание
        if (user.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileStats(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            count = user.eventsCount.toString(),
            label = "Мероприятий"
        )
        StatItem(
            count = user.organizationsCount.toString(),
            label = "Организаций"
        )
        StatItem(
            count = user.followersCount.toString(),
            label = "Подписчиков"
        )
    }
}

@Composable
fun StatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileInfo(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Город
        if (user.city.isNotEmpty()) {
            InfoRow(
                icon = Icons.Default.LocationOn,
                text = user.city
            )
        }

        // Учебное заведение
        if (user.education.isNotEmpty()) {
            InfoRow(
                icon = Icons.Default.ShoppingCart,
                text = user.education
            )
        }

        // Телефон
        if (user.phone.isNotEmpty()) {
            InfoRow(
                icon = Icons.Default.Phone,
                text = user.phone
            )
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EventsTab(events: List<Event>, onEventClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (events.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MailOutline,
                text = "Нет предстоящих мероприятий"
            )
        } else {
            events.forEach { event ->
                EventListItem(
                    event = event,
                    onClick = { onEventClick(event.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun OrganizationsTab(
    organizations: List<Organization>,
    onOrganizationClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (organizations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Face,
                text = "Нет организаций"
            )
        } else {
            organizations.forEach { organization ->
                OrganizationListItem(
                    organization = organization,
                    onClick = { onOrganizationClick(organization.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ReviewsTab(reviews: List<Review>) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (reviews.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Star,
                text = "Нет отзывов"
            )
        } else {
            reviews.forEach { review ->
                ReviewItem(review = review)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun EventListItem(event: Event, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrganizationListItem(organization: Organization, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${organization.eventsCount} мероприятий",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок отзыва
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.eventTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                RatingBar(rating = review.rating)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Текст отзыва
            Text(
                text = review.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Дата
            Text(
                text = review.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun RatingBar(rating: Int, maxRating: Int = 5) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Рейтинг",
                tint = if (i <= rating) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationProfileScreen(
    organization: Organization,
    isOwner: Boolean = false,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onEventClick: (String) -> Unit,
    onCreateEventClick: () -> Unit,
    onSubscribeClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Предстоящие", "Прошедшие", "Отзывы")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Организация") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = onCreateEventClick) {
                            Icon(Icons.Default.Add, contentDescription = "Создать мероприятие")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Хедер организации
            OrganizationHeader(
                organization = organization,
                onSubscribeClick = onSubscribeClick,
                isOwner = isOwner
            )

            // Табы
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Контент табов
            when (selectedTab) {
                0 -> EventsTab(
                    events = organization.upcomingEvents,
                    onEventClick = onEventClick
                )
                1 -> EventsTab(
                    events = organization.pastEvents,
                    onEventClick = onEventClick
                )
                2 -> ReviewsTab(reviews = organization.reviews)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganizationHeader(
    organization: Organization,
    onSubscribeClick: () -> Unit,
    isOwner: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = organization.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                // Статус организации
                Surface(
                    color = when (organization.status) {
                        OrganizationStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                        OrganizationStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        OrganizationStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = CircleShape
                ) {
                    Text(
                        text = organization.status.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка подписки/управления
                if (isOwner) {
                    OutlinedButton(onClick = onSubscribeClick) {
                        Text("Управление")
                    }
                } else {
                    Button(
                        onClick = onSubscribeClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (organization.isSubscribed)
                                MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (organization.isSubscribed) "Отписаться" else "Подписаться"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Описание
        if (organization.description.isNotEmpty()) {
            Text(
                text = organization.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Статистика
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                count = organization.eventsCount.toString(),
                label = "Мероприятий"
            )
            StatItem(
                count = organization.subscribersCount.toString(),
                label = "Подписчиков"
            )
            StatItem(
                count = organization.rating.toString(),
                label = "Рейтинг"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Теги
        if (organization.tags.isNotEmpty()) {
            Text(
                text = "Направления:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                organization.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileScreenPreview() {
    MaterialTheme {
        UserProfileScreen(
            user = sampleUser,
            isOwnProfile = true,
            onBackClick = {},
            onEditClick = {},
            onSettingsClick = {},
            onOrganizationClick = {},
            onEventClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrganizationProfileScreenPreview() {
    MaterialTheme {
        OrganizationProfileScreen(
            organization = sampleOrganization,
            onBackClick = {},
            onEditClick = {},
            onEventClick = {},
            onCreateEventClick = {},
            onSubscribeClick = {}
        )
    }
}