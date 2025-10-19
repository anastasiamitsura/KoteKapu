import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onOrganizationClick: (String) -> Unit,
    onShareClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мероприятие") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
                    }
                }
            )
        },
        bottomBar = {
            if (event.registrationOpen) {
                EventBottomBar(
                    event = event,
                    onRegisterClick = onRegisterClick
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Баннер мероприятия
            EventBanner(event = event)

            // Основная информация
            EventMainInfo(event = event)

            // Организатор
            OrganizerSection(
                organization = event.organization,
                onClick = { onOrganizationClick(event.organization.id) }
            )

            // Описание
            DescriptionSection(
                description = event.description,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded }
            )

            // Детали мероприятия
            EventDetails(event = event)

            // Теги
            TagsSection(tags = event.tags)

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EventBanner(event: Event) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Заглушка для изображения
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        )

        // Градиент и информация поверх баннера
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Формат и дата в баннере
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Дата",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EventMainInfo(event: Event) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Статус регистрации
        Surface(
            color = if (event.registrationOpen) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
            shape = CircleShape,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = if (event.registrationOpen) "Регистрация открыта"
                else "Регистрация закрыта",
                style = MaterialTheme.typography.labelMedium,
                color = if (event.registrationOpen) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Участники
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Участники",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${event.participantsCount} участников",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Локация
        if (event.location.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Локация",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Ссылка (для онлайн мероприятий)
        if (event.format != EventFormat.OFFLINE && event.onlineLink.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Ссылка",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.onlineLink,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OrganizerSection(
    organization: Organization,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар организатора
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Организатор",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (organization.eventsCount > 0) {
                    Text(
                        text = "${organization.eventsCount} мероприятий",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DescriptionSection(
    description: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Описание",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )

        if (description.length > 200) {
            TextButton(
                onClick = onExpandClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = if (isExpanded) "Свернуть" else "Развернуть",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun EventDetails(event: Event) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Детали мероприятия",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Расписание
        event.schedule.forEach { scheduleItem ->
            ScheduleItem(
                time = scheduleItem.time,
                title = scheduleItem.title,
                description = scheduleItem.description
            )
        }

        // Дополнительная информация
        if (event.additionalInfo.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = event.additionalInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScheduleItem(time: String, title: String, description: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(tags: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Теги",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EventBottomBar(
    event: Event,
    onRegisterClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (event.price > 0) {
                    Text(
                        text = "${event.price} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Бесплатно",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${event.participantsCount} участников",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onRegisterClick,
                modifier = Modifier.width(200.dp)
            ) {
                Text("Зарегистрироваться")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EventDetailScreenPreview() {
    MaterialTheme {
        EventDetailScreen(
            event = sampleDetailedEvent,
            onBackClick = {},
            onRegisterClick = {},
            onOrganizationClick = {},
            onShareClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EventBannerPreview() {
    MaterialTheme {
        EventBanner(event = sampleDetailedEvent)
    }
}