import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalProfileScreen(
    user: User,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateOrganizationClick: () -> Unit,
    onMyOrganizationsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onEventClick: (String) -> Unit,
    onOrganizationClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Активность", "Мероприятия", "Организации")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мой профиль") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Хедер профиля
            PersonalProfileHeader(user = user, onEditClick = onEditProfileClick)

            // Быстрые действия
            QuickActionsSection(
                onCreateOrganizationClick = onCreateOrganizationClick,
                onMyOrganizationsClick = onMyOrganizationsClick,
                onCalendarClick = onCalendarClick,
                onPreferencesClick = onPreferencesClick
            )

            // Статистика
            PersonalStatsSection(user = user)

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
                0 -> ActivityTab(
                    activities = user.recentActivities,
                    onEventClick = onEventClick,
                    onOrganizationClick = onOrganizationClick
                )
                1 -> PersonalEventsTab(
                    upcomingEvents = user.upcomingEvents,
                    pastEvents = user.pastEvents,
                    onEventClick = onEventClick
                )
                2 -> PersonalOrganizationsTab(
                    organizations = user.organizations,
                    onOrganizationClick = onOrganizationClick
                )
            }

            // Дополнительные настройки
            AdditionalOptionsSection(
                onHelpClick = onHelpClick,
                onLogoutClick = onLogoutClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PersonalProfileHeader(user: User, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        text = user.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    if (user.status.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = user.status,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Кнопка редактирования
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать профиль")
                }
            }

            // Описание
            if (user.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onCreateOrganizationClick: () -> Unit,
    onMyOrganizationsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPreferencesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Быстрые действия",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                text = "Создать\nорганизацию",
                onClick = onCreateOrganizationClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Person,
                text = "Мои\nорганизации",
                onClick = onMyOrganizationsClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.DateRange,
                text = "Календарь\nмероприятий",
                onClick = onCalendarClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Place,
                text = "Мои\nпредпочтения",
                onClick = onPreferencesClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PersonalStatsSection(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Моя активность",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PersonalStatItem(
                    icon = Icons.Default.DateRange,
                    count = user.eventsCount.toString(),
                    label = "Посетил\nмероприятий"
                )
                PersonalStatItem(
                    icon = Icons.Default.Person,
                    count = user.organizationsCount.toString(),
                    label = "Мои\nорганизации"
                )
                PersonalStatItem(
                    icon = Icons.Default.Person,
                    count = user.followersCount.toString(),
                    label = "Подписчиков"
                )
                PersonalStatItem(
                    icon = Icons.Default.Star,
                    count = user.reviewsCount.toString(),
                    label = "Оставлено\nотзывов"
                )
            }
        }
    }
}

@Composable
fun PersonalStatItem(icon: ImageVector, count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ActivityTab(
    activities: List<UserActivity>,
    onEventClick: (String) -> Unit,
    onOrganizationClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (activities.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Notifications,
                text = "Здесь будет отображаться ваша активность"
            )
        } else {
            activities.forEach { activity ->
                ActivityItem(
                    activity = activity,
                    onEventClick = onEventClick,
                    onOrganizationClick = onOrganizationClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PersonalEventsTab(
    upcomingEvents: List<Event>,
    pastEvents: List<Event>,
    onEventClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Предстоящие мероприятия
        if (upcomingEvents.isNotEmpty()) {
            Text(
                text = "Предстоящие",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            upcomingEvents.forEach { event ->
                EventListItem(
                    event = event,
                    onClick = { onEventClick(event.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Прошедшие мероприятия
        if (pastEvents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Прошедшие",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            pastEvents.forEach { event ->
                EventListItem(
                    event = event,
                    onClick = { onEventClick(event.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (upcomingEvents.isEmpty() && pastEvents.isEmpty()) {
            EmptyState(
                icon = Icons.Default.DateRange,
                text = "Вы еще не посещали мероприятия"
            )
        }
    }
}

@Composable
fun PersonalOrganizationsTab(
    organizations: List<Organization>,
    onOrganizationClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (organizations.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Person,
                text = "Вы не состоите в организациях"
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
fun ActivityItem(
    activity: UserActivity,
    onEventClick: (String) -> Unit,
    onOrganizationClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = activity.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (activity.type) {
                ActivityType.EVENT -> {
                    TextButton(onClick = { onEventClick(activity.targetId) }) {
                        Text("Перейти")
                    }
                }
                ActivityType.ORGANIZATION -> {
                    TextButton(onClick = { onOrganizationClick(activity.targetId) }) {
                        Text("Перейти")
                    }
                }
                else -> {
                    // Для других типов активности не показываем кнопку
                }
            }
        }
    }
}

@Composable
fun AdditionalOptionsSection(
    onHelpClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Дополнительно",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card {
            Column {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    text = "Помощь и поддержка",
                    onClick = onHelpClick
                )
                Divider()
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    text = "Выйти",
                    onClick = onLogoutClick,
                    textColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    ListItem(
        headlineContent = {
            Text(
                text = text,
                color = textColor
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun PersonalProfileScreenPreview() {
    MaterialTheme {
        PersonalProfileScreen(
            user = samplePersonalUser,
            onEditProfileClick = {},
            onSettingsClick = {},
            onCreateOrganizationClick = {},
            onMyOrganizationsClick = {},
            onCalendarClick = {},
            onPreferencesClick = {},
            onHelpClick = {},
            onLogoutClick = {},
            onEventClick = {},
            onOrganizationClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuickActionsPreview() {
    MaterialTheme {
        QuickActionsSection(
            onCreateOrganizationClick = {},
            onMyOrganizationsClick = {},
            onCalendarClick = {},
            onPreferencesClick = {}
        )
    }
}