import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onEventClick: (Event) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val events by remember { mutableStateOf(sampleEvents) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мероприятия") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Уведомления")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Профиль")
                    }
                }
            )
        },
        content = { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Табы для фильтрации ленты
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Для вас") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Подписки") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Популярные") }
                    )
                }

                // Лента мероприятий
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events) { event ->
                        EventCard(
                            event = event,
                            onEventClick = { onEventClick(event) },
                            onLikeClick = { /* Обработка лайка */ }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventCard(
    event: Event,
    onEventClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    var isLiked by remember { mutableStateOf(event.isLiked) }

    Card(
        onClick = onEventClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Фоновое изображение
            // В реальном приложении используйте Coil или Glide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )

            // Градиент поверх изображения для лучшей читаемости текста
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f,
                            endY = 200f
                        )
                    )
            )

            // Контент поверх изображения
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Верхняя часть - организация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OrganizationInfo(organization = event.organization)

                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            onLikeClick()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite
                            else Icons.Outlined.Favorite,
                            contentDescription = "Лайк",
                            tint = if (isLiked) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Нижняя часть - информация о мероприятии
                Column {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Теги мероприятия
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        event.tags.take(3).forEach { tag ->
                            TagChip(tag = tag)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Дата и формат
                    EventMetaInfo(event = event)
                }
            }
        }
    }
}

@Composable
fun OrganizationInfo(organization: Organization) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Аватар организации (заглушка)
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = organization.name,
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White,
            maxLines = 1
        )
    }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        shape = CircleShape
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EventMetaInfo(event: Event) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Дата",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = event.date,
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White
            )
        }

        Surface(
            color = when (event.format) {
                EventFormat.ONLINE -> MaterialTheme.colorScheme.tertiary
                EventFormat.OFFLINE -> MaterialTheme.colorScheme.secondary
                EventFormat.HYBRID -> MaterialTheme.colorScheme.primary
            },
            shape = CircleShape
        ) {
            Text(
                text = event.format.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(
            onEventClick = {},
            onSearchClick = {},
            onProfileClick = {},
            onNotificationsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventCardPreview() {
    MaterialTheme {
        EventCard(
            event = sampleEvents[0],
            onEventClick = {},
            onLikeClick = {}
        )
    }
}