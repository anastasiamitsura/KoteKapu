package com.example.kotekapu_2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    apiService: ApiService,
    authManager: AuthManager,
    onLogout: () -> Unit,
    onEventClick: (Post) -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var hasMorePosts by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    // Функция для обновления ленты
    fun refreshFeed() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                println("DEBUG: Refreshing feed...")
                val token = authManager.getCurrentToken() ?: ""
                val result = apiService.getRecommendedFeed(token = token, limit = 10, offset = 0)

                if (result.isSuccess) {
                    val feedResponse = result.getOrNull()
                    val newPosts = feedResponse?.posts ?: emptyList()
                    posts = newPosts
                    hasMorePosts = feedResponse?.has_more ?: (newPosts.size >= 10)
                    println("DEBUG: ✅ Feed refreshed, loaded ${newPosts.size} posts, hasMore: $hasMorePosts")
                    errorMessage = null
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Ошибка загрузки"
                    println("DEBUG: ❌ Feed refresh failed: $errorMessage")
                    // Fallback к локальным данным
                    posts = getSamplePosts()
                    hasMorePosts = false
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка сети: ${e.message}"
                println("DEBUG: 💥 Feed refresh error: ${e.message}")
                // Fallback к локальным данным
                posts = getSamplePosts()
                hasMorePosts = false
            } finally {
                isLoading = false
            }
        }
    }

    // Функция для загрузки дополнительных постов
    fun loadMorePosts() {
        if (isLoadingMore || !hasMorePosts) return

        isLoadingMore = true
        coroutineScope.launch {
            try {
                val token = authManager.getCurrentToken()
                val result = apiService.getRecommendedFeed(
                    token = token ?: "",
                    limit = 5,
                    offset = posts.size
                )

                if (result.isSuccess) {
                    val feedResponse = result.getOrNull()
                    val newPosts = feedResponse?.posts ?: emptyList()
                    if (newPosts.isNotEmpty()) {
                        posts = posts + newPosts
                        hasMorePosts = feedResponse?.has_more ?: (newPosts.size >= 5)
                        println("DEBUG: ✅ Loaded ${newPosts.size} more posts, total: ${posts.size}, hasMore: $hasMorePosts")
                    } else {
                        println("DEBUG: ℹ️ No more posts available")
                        hasMorePosts = false
                    }
                } else {
                    println("DEBUG: ❌ Failed to load more posts: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("DEBUG: 💥 Error loading more posts: ${e.message}")
            } finally {
                isLoadingMore = false
            }
        }
    }

    // Загрузка ленты при открытии
    LaunchedEffect(Unit) {
        println("DEBUG: MainScreen launched - loading initial feed")
        refreshFeed()
    }

    // Обработка смены табов
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && posts.isEmpty()) {
            refreshFeed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мероприятия") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Выход")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // TODO: Переход к созданию организации
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Создать") },
                text = { Text("Организация") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Табы
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

            // Отображение состояния
            when {
                isLoading -> {
                    LoadingState()
                }
                errorMessage != null -> {
                    ErrorState(
                        errorMessage = errorMessage!!,
                        onRetry = {
                            refreshFeed()
                        }
                    )
                }
                posts.isEmpty() -> {
                    EmptyState(
                        onRetry = {
                            refreshFeed()
                        }
                    )
                }
                else -> {
                    // Отображение постов
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts) { post ->
                            PostCard(
                                post = post,
                                onLikeClick = {
                                    coroutineScope.launch {
                                        likePostWithInterests(apiService, authManager, post)
                                    }
                                },
                                onClick = {
                                    onEventClick(post)
                                }
                            )
                        }

                        // Индикатор загрузки или кнопка "Загрузить еще"
                        item {
                            if (isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (hasMorePosts) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(
                                        onClick = { loadMorePosts() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text("Показать больше мероприятий")
                                    }
                                }
                            } else if (posts.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Все мероприятия загружены",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог выхода
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Выход") },
                text = { Text("Вы уверены, что хотите выйти?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            coroutineScope.launch {
                                authManager.logout()
                                onLogout()
                            }
                        }
                    ) {
                        Text("Выйти")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

// Состояние загрузки
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Загрузка рекомендаций...")
        }
    }
}

// Состояние ошибки
@Composable
fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Ошибка",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Ошибка загрузки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(onClick = onRetry) {
                Text("Повторить попытку")
            }
            Text(
                text = "Используются демо-данные",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// Состояние пустой ленты
@Composable
fun EmptyState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Create,
                contentDescription = "Нет мероприятий",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "Пока нет мероприятий",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Попробуйте обновить ленту",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Button(onClick = onRetry) {
                Text("Обновить ленту")
            }
        }
    }
}

// Карточка поста
@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onClick: () -> Unit
) {
    var isLiked by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Фоновый градиент
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Градиент для текста
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f,
                            endY = 200f
                        )
                    )
            )

            // Контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Верхняя часть - релевантность и лайк
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    post.relevance_score?.let { score ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${(score * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } ?: Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            onLikeClick()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Лайк",
                            tint = if (isLiked) Color.Red else Color.White
                        )
                    }
                }

                // Нижняя часть - информация
                Column {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = post.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Теги
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        post.interest_tags.take(3).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        post.format_tags.take(2).forEach { tag ->
                            Surface(
                                color = when (tag.lowercase()) {
                                    "онлайн" -> MaterialTheme.colorScheme.tertiary
                                    "офлайн" -> MaterialTheme.colorScheme.secondary
                                    "гибрид" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }.copy(alpha = 0.8f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Дата и тип
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Дата",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDateTime(post.date_time ?: post.created_at ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = when (post.type) {
                                "event" -> MaterialTheme.colorScheme.secondary
                                "simple" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = CircleShape
                        ) {
                            Text(
                                text = when (post.type) {
                                    "event" -> "Мероприятие"
                                    "simple" -> "Пост"
                                    else -> post.type
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Функция для лайка с интересами
private suspend fun likePostWithInterests(
    apiService: ApiService,
    authManager: AuthManager,
    post: Post
) {
    try {
        val token = authManager.getCurrentToken()
        val userId = authManager.getCurrentUserId()

        if (token != null && userId != null) {
            println("DEBUG: Liking post ${post.id}")
            val result = apiService.likePostWithInterests(
                token = token,
                postId = post.id,
                userId = userId,
                interestTags = post.interest_tags,
                formatTags = post.format_tags
            )

            if (result.isSuccess) {
                println("DEBUG: Post liked successfully, interests updated")
            } else {
                println("DEBUG: Failed to like post: ${result.exceptionOrNull()?.message}")
            }
        } else {
            println("DEBUG: Cannot like post - no token or user ID")
        }
    } catch (e: Exception) {
        println("DEBUG: Error liking post: ${e.message}")
    }
}

// Форматирование даты
private fun formatDateTime(dateTimeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateTimeString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        "Дата не указана"
    }
}

private fun getSamplePosts(): List<Post> {
    return listOf(
        Post(
            id = 1,
            title = "Хакатон по мобильной разработке",
            description = "Соревнование по созданию мобильных приложений с призовым фондом",
            date_time = "2024-01-15T14:00:00",
            created_at = "2024-01-01T10:00:00",
            pic = null,
            interest_tags = listOf("IT", "Программирование", "Мобильная разработка"),
            format_tags = listOf("офлайн"),
            organization_id = 1,
            author_id = null,
            type = "event",
            relevance_score = 0.85,
            likes = 5
        ),
        Post(
            id = 2,
            title = "Онлайн курс по Kotlin",
            description = "Изучите современный язык программирования для Android разработки",
            date_time = "2024-01-20T16:00:00",
            created_at = "2024-01-02T11:00:00",
            pic = null,
            interest_tags = listOf("IT", "Программирование", "Kotlin"),
            format_tags = listOf("онлайн"),
            organization_id = 2,
            author_id = null,
            type = "event",
            relevance_score = 0.75,
            likes = 3
        ),
        Post(
            id = 3,
            title = "Воркшоп по UI/UX дизайну",
            description = "Практическое занятие по созданию пользовательских интерфейсов",
            date_time = "2024-01-25T12:00:00",
            created_at = "2024-01-03T09:00:00",
            pic = null,
            interest_tags = listOf("Дизайн", "Творчество", "UI/UX"),
            format_tags = listOf("офлайн"),
            organization_id = 3,
            author_id = null,
            type = "event",
            relevance_score = 0.65,
            likes = 8
        )
    )
}