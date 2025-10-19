import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventAnalyticsScreen(
    event: Event,
    analytics: EventAnalytics,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit,
    onParticipantClick: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Обзор", "Участники", "Эффективность")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика мероприятия") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Default.Send, contentDescription = "Экспорт")
                    }
                    IconButton(onClick = { /* TODO: Поделиться аналитикой */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться")
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
            // Заголовок мероприятия
            EventHeader(event = event, modifier = Modifier.padding(16.dp))

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
                0 -> OverviewTab(analytics = analytics, event = event)
                1 -> ParticipantsTab(
                    participants = analytics.participants,
                    onParticipantClick = onParticipantClick
                )
                2 -> EffectivenessTab(analytics = analytics, event = event)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EventHeader(event: Event, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Дата",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Участники",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${event.participantsCount} участников",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OverviewTab(analytics: EventAnalytics, event: Event) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ключевые метрики
        KeyMetricsSection(analytics = analytics, event = event)

        // Статистика регистраций
        RegistrationStatsSection(analytics = analytics)

        // Демография
        DemographicsSection(analytics = analytics)

        // Эффективность продвижения
        PromotionEffectivenessSection(analytics = analytics)
    }
}

@Composable
fun KeyMetricsSection(analytics: EventAnalytics, event: Event) {
    Column {
        Text(
            text = "Ключевые метрики",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Регистрации
            MetricCard(
                title = "Регистрации",
                value = analytics.registrationsCount.toString(),
                subtitle = "из ${event.participantsCount} макс.",
                trend = analytics.registrationTrend,
                modifier = Modifier.weight(1f)
            )

            // Посещаемость
            MetricCard(
                title = "Посещаемость",
                value = "${analytics.attendanceRate}%",
                subtitle = "${analytics.attendedCount} участников",
                trend = analytics.attendanceTrend,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Лайки
            MetricCard(
                title = "Лайки",
                value = analytics.likesCount.toString(),
                subtitle = "лайков мероприятия",
                trend = analytics.likesTrend,
                modifier = Modifier.weight(1f)
            )

            // Просмотры
            MetricCard(
                title = "Просмотры",
                value = analytics.viewsCount.toString(),
                subtitle = "просмотров страницы",
                trend = analytics.viewsTrend,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    trend: Trend,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TrendIndicator(trend = trend)
            }
        }
    }
}

@Composable
fun TrendIndicator(trend: Trend) {
    val trendData = when (trend) {
        Trend.UP -> TrendData(
            icon = Icons.Default.KeyboardArrowUp,
            color = Color(0xFF4CAF50),
            text = "+12%"
        )
        Trend.DOWN -> TrendData(
            icon = Icons.Default.KeyboardArrowDown,
            color = Color(0xFFF44336),
            text = "-5%"
        )
        Trend.STABLE -> TrendData(
            icon = Icons.Default.Close,
            color = Color(0xFF9E9E9E),
            text = "0%"
        )
        Trend.NEW -> TrendData(
            icon = Icons.Default.Add,
            color = MaterialTheme.colorScheme.primary,
            text = "NEW"
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = trendData.icon,
            contentDescription = null,
            tint = trendData.color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = trendData.text,
            style = MaterialTheme.typography.labelSmall,
            color = trendData.color,
            fontSize = 10.sp
        )
    }
}

data class TrendData(
    val icon: ImageVector,
    val color: Color,
    val text: String
)

@Composable
fun RegistrationStatsSection(analytics: EventAnalytics) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Статистика регистраций",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // График регистраций по дням (упрощенная версия)
            RegistrationTimeline(registrationData = analytics.registrationTimeline)

            Spacer(modifier = Modifier.height(16.dp))

            // Детализация по источникам
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SourceStatItem(
                    source = "Лента",
                    count = analytics.sources["feed"] ?: 0,
                    total = analytics.registrationsCount,
                    modifier = Modifier.weight(1f)
                )
                SourceStatItem(
                    source = "Поиск",
                    count = analytics.sources["search"] ?: 0,
                    total = analytics.registrationsCount,
                    modifier = Modifier.weight(1f)
                )
                SourceStatItem(
                    source = "Ссылка",
                    count = analytics.sources["direct"] ?: 0,
                    total = analytics.registrationsCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RegistrationTimeline(registrationData: List<RegistrationDay>) {
    val maxRegistrations = registrationData.maxOfOrNull { it.count } ?: 0

    Column {
        Text(
            text = "Регистрации по дням",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            registrationData.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Столбец графика
                    val heightPercent = if (maxRegistrations > 0) day.count.toFloat() / maxRegistrations else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(60.dp * heightPercent)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = day.count.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = day.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SourceStatItem(
    source: String,
    count: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count.toFloat() / total * 100).toInt() else 0

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count чел.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DemographicsSection(analytics: EventAnalytics) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Демография участников",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DemographicItem(
                    title = "Города",
                    data = analytics.cities,
                    modifier = Modifier.weight(1f)
                )
                DemographicItem(
                    title = "Возраст",
                    data = analytics.ageGroups,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DemographicItem(
    title: String,
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        data.entries.take(3).forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$value чел.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (data.size > 3) {
            Text(
                text = "и еще ${data.size - 3}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PromotionEffectivenessSection(analytics: EventAnalytics) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Эффективность продвижения",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            analytics.promotionChannels.forEach { channel ->
                PromotionChannelItem(
                    channel = channel,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PromotionChannelItem(channel: PromotionChannel, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = channel.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${channel.registrations} регистраций • CTR: ${channel.ctr}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${channel.conversionRate}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ParticipantsTab(
    participants: List<EventParticipant>,
    onParticipantClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Участники (${participants.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (participants.isEmpty()) {
            EmptyAnalyticsState(
                icon = Icons.Default.Person,
                text = "Нет данных об участниках"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants) { participant ->
                    ParticipantItem(
                        participant = participant,
                        onClick = { onParticipantClick(participant.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ParticipantItem(participant: EventParticipant, onClick: () -> Unit) {
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
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.name.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = participant.city,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = participant.registrationDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (participant.attended) {
                    Text(
                        text = "Присутствовал",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EffectivenessTab(analytics: EventAnalytics, event: Event) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ROI (если мероприятие платное)
        if (event.price > 0) {
            ROISection(analytics = analytics, event = event)
        }

        // Удовлетворенность
        SatisfactionSection(analytics = analytics)

        // Рекомендации
        RecommendationsSection(analytics = analytics)
    }
}

@Composable
fun ROISection(analytics: EventAnalytics, event: Event) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ROI мероприятия",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Доход",
                    value = "${analytics.revenue} ₽",
                    subtitle = "от ${analytics.paidParticipants} оплат",
                    trend = Trend.UP,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "ROI",
                    value = "${analytics.roi}%",
                    subtitle = "рентабельность",
                    trend = if (analytics.roi > 0) Trend.UP else Trend.DOWN,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SatisfactionSection(analytics: EventAnalytics) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Удовлетворенность",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SatisfactionMetric(
                    value = analytics.averageRating,
                    maxValue = 5.0,
                    label = "Рейтинг",
                    modifier = Modifier.weight(1f)
                )
                SatisfactionMetric(
                    value = analytics.recommendationScore.toDouble(),
                    maxValue = 100.0,
                    label = "NPS",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Отзывы
            if (analytics.recentReviews.isNotEmpty()) {
                Text(
                    text = "Последние отзывы",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                analytics.recentReviews.take(2).forEach { review ->
                    ReviewPreview(review = review)
                }
            }
        }
    }
}

@Composable
fun SatisfactionMetric(
    value: Double,
    maxValue: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
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
fun ReviewPreview(review: EventReview) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Рейтинг звездами
                StarRating(rating = review.rating)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = review.authorName,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StarRating(rating: Int, maxRating: Int = 5) {
    Row {
        for (i in 1..maxRating) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Default.Star,
                contentDescription = "Рейтинг",
                tint = if (i <= rating) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun RecommendationsSection(analytics: EventAnalytics) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Рекомендации",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            analytics.recommendations.forEach { recommendation ->
                RecommendationItem(recommendation = recommendation)
            }
        }
    }
}

@Composable
fun RecommendationItem(recommendation: Recommendation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = recommendation.icon,
            contentDescription = null,
            tint = when (recommendation.priority) {
                Priority.HIGH -> MaterialTheme.colorScheme.error
                Priority.MEDIUM -> MaterialTheme.colorScheme.secondary
                Priority.LOW -> MaterialTheme.colorScheme.primary
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyAnalyticsState(icon: ImageVector, text: String) {
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

@Preview(showBackground = true)
@Composable
fun EventAnalyticsScreenPreview() {
    MaterialTheme {
        EventAnalyticsScreen(
            event = sampleDetailedEvent,
            analytics = sampleEventAnalytics,
            onBackClick = {},
            onExportClick = {},
            onParticipantClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun KeyMetricsSectionPreview() {
    MaterialTheme {
        KeyMetricsSection(
            analytics = sampleEventAnalytics,
            event = sampleDetailedEvent
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RegistrationStatsSectionPreview() {
    MaterialTheme {
        RegistrationStatsSection(analytics = sampleEventAnalytics)
    }
}