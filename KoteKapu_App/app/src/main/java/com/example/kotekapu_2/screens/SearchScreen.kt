// SearchScreen.kt
package com.example.kotekapu_2.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kotekapu_2.ApiService
import com.example.kotekapu_2.AuthManager
import com.example.kotekapu_2.Organisation
import com.example.kotekapu_2.Post
import com.example.kotekapu_2.SearchFilters
import com.example.kotekapu_2.SearchRequest
import com.example.kotekapu_2.SearchResponse
import com.example.kotekapu_2.SearchSuggestions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    apiService: ApiService,
    authManager: AuthManager,
    onBack: () -> Unit,
    onEventClick: (Post) -> Unit,
    onOrganizationClick: (Organisation) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<SearchResponse?>(null) }
    var searchSuggestions by remember { mutableStateOf<SearchSuggestions?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 - мероприятия, 1 - организации
    var filters by remember { mutableStateOf(SearchFilters()) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val searchState = rememberSearchState(apiService, authManager)

    // Загрузка подсказок при открытии
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val token = authManager.getCurrentToken()
            val result = apiService.getSearchSuggestions(token)
            if (result.isSuccess) {
                searchSuggestions = result.getOrNull()
            }
        }
    }


    LaunchedEffect(searchQuery, filters) {
        if (searchQuery.length >= 2 || filters != SearchFilters()) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val results = searchState.performSearch(searchQuery, filters)
                    searchResults = results
                    println("DEBUG: Search results - ${results.events.size} events, ${results.organizations.size} orgs")
                } catch (e: Exception) {
                    println("DEBUG: Search error: ${e.message}")
                    // Fallback к демо-данным только при ошибке
                    searchResults = getDemoSearchResults(searchQuery)
                } finally {
                    isLoading = false
                }
            }
        } else {
            searchResults = null
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchTextField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            keyboardController?.hide()
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val results = searchState.performSearch(searchQuery, filters)
                                    searchResults = results
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        focusRequester = focusRequester,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (filters != SearchFilters()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Фильтры
            if (showFilters) {
                SearchFiltersSection(
                    filters = filters,
                    onFiltersChange = { newFilters ->
                        filters = newFilters
                    },
                    onClearFilters = {
                        filters = SearchFilters()
                    }
                )
            }

            when {
                isLoading -> {
                    LoadingSearchState()
                }
                searchResults != null -> {
                    SearchResultsSection(
                        searchResults = searchResults!!,
                        activeTab = activeTab,
                        onTabChange = { activeTab = it },
                        onEventClick = onEventClick,
                        onOrganizationClick = onOrganizationClick
                    )
                }
                else -> {
                    SearchSuggestionsSection(
                        suggestions = searchSuggestions,
                        onSuggestionClick = { suggestion ->
                            searchQuery = suggestion
                        },
                        onTagClick = { tag ->
                            filters = filters.copy(interests = filters.interests + tag)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var hasFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(focusRequester),
        placeholder = { Text("Поиск мероприятий и организаций...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Поиск")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
            keyboardType = KeyboardType.Text
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun SearchFiltersSection(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onClearFilters: () -> Unit
) {
    var expandedInterests by remember { mutableStateOf(false) }
    var expandedFormats by remember { mutableStateOf(false) }
    var expandedEventTypes by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок с кнопкой очистки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text("Очистить")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Фильтр по интересам
            FilterChipGroup(
                title = "Интересы",
                options = listOf("IT", "Искусство", "Наука", "Спорт", "Бизнес", "Языки"),
                selectedOptions = filters.interests,
                onSelectionChange = { selected ->
                    onFiltersChange(filters.copy(interests = selected))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Фильтр по форматам
            FilterChipGroup(
                title = "Форматы",
                options = listOf("онлайн", "офлайн", "гибрид"),
                selectedOptions = filters.formats,
                onSelectionChange = { selected ->
                    onFiltersChange(filters.copy(formats = selected))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Фильтр по типам событий
            FilterChipGroup(
                title = "Типы событий",
                options = listOf("хакатон", "лекция", "мастер-класс", "встреча", "семинар"),
                selectedOptions = filters.event_types,
                onSelectionChange = { selected ->
                    onFiltersChange(filters.copy(event_types = selected))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Фильтр по локации
            OutlinedTextField(
                value = filters.location ?: "",
                onValueChange = { location ->
                    onFiltersChange(filters.copy(location = location.ifEmpty { null }))
                },
                label = { Text("Город или адрес") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun FilterChipGroup(
    title: String,
    options: List<String>,
    selectedOptions: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = if (isSelected) {
                            selectedOptions - option
                        } else {
                            selectedOptions + option
                        }
                        onSelectionChange(newSelection)
                    },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
fun SearchResultsSection(
    searchResults: SearchResponse,
    activeTab: Int,
    onTabChange: (Int) -> Unit,
    onEventClick: (Post) -> Unit,
    onOrganizationClick: (Organisation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { onTabChange(0) },
                text = {
                    Text("Мероприятия ${searchResults.total_events}")
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { onTabChange(1) },
                text = {
                    Text("Организации ${searchResults.total_organizations}")
                }
            )
        }

        when (activeTab) {
            0 -> {
                if (searchResults.events.isEmpty()) {
                    EmptySearchState("Мероприятия не найдены")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults.events) { event ->
                            SearchEventItem(
                                event = event,
                                onClick = { onEventClick(event) }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (searchResults.organizations.isEmpty()) {
                    EmptySearchState("Организации не найдены")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults.organizations) { organization ->
                            SearchOrganizationItem(
                                organization = organization,
                                onClick = { onOrganizationClick(organization) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchEventItem(event: Post, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "Мероприятие",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Теги
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    event.interest_tags.take(3).forEach { tag ->
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

                Spacer(modifier = Modifier.height(4.dp))

                // Дата
                Text(
                    text = event.date_time ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SearchOrganizationItem(organization: Organisation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = "Организация",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organization.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp)) // ИСПРАВЛЕНО: было Mododer

                Text(
                    text = organization.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Статистика
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "${organization.events_count} мероприятий",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "${organization.subscribers_count} подписчиков",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    organization.tags.take(3).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
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
}

@Composable
fun SearchSuggestionsSection(
    suggestions: SearchSuggestions?,
    onSuggestionClick: (String) -> Unit,
    onTagClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        suggestions?.popular_searches?.let { popularSearches ->
            if (popularSearches.isNotEmpty()) {
                item {
                    Text(
                        text = "Популярные запросы",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(popularSearches) { suggestion ->
                    SuggestionItem(
                        text = suggestion,
                        icon = Icons.Default.TrendingUp,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }

        suggestions?.popular_tags?.let { popularTags ->
            if (popularTags.isNotEmpty()) {
                item {
                    Text(
                        text = "Популярные теги",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularTags.forEach { tag ->
                            SuggestionChip(
                                onClick = { onTagClick(tag) },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun LoadingSearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Поиск...")
        }
    }
}

@Composable
fun EmptySearchState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = "Ничего не найдено",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun rememberSearchState(
    apiService: ApiService,
    authManager: AuthManager
): SearchState {
    return remember {
        SearchState(apiService, authManager)
    }
}

class SearchState(
    private val apiService: ApiService,
    private val authManager: AuthManager
) {
    suspend fun performSearch(
        query: String,
        filters: SearchFilters
    ): SearchResponse {
        val token = authManager.getCurrentToken()
        val request = SearchRequest(
            query = if (query.isNotEmpty()) query else null,
            filters = if (filters != SearchFilters()) filters else null
        )

        val result = apiService.search(token, request)
        return result.getOrNull() ?: SearchResponse(emptyList(), emptyList(), 0, 0)
    }
}
private fun getDemoSearchResults(query: String): SearchResponse {
    return SearchResponse(
        events = listOf(
            Post(
                id = 1,
                title = "Хакатон по $query",
                description = "Соревнование по созданию мобильных приложений с призовым фондом",
                date_time = "2024-02-15T14:00:00",
                created_at = "2024-01-01T10:00:00",
                interest_tags = listOf("IT", "Программирование", query),
                format_tags = listOf("офлайн"),
                type = "event"
            ),
            Post(
                id = 2,
                title = "Онлайн курс по $query",
                description = "Изучение современного языка программирования",
                date_time = "2024-02-20T16:00:00",
                created_at = "2024-01-02T11:00:00",
                interest_tags = listOf("IT", query),
                format_tags = listOf("онлайн"),
                type = "event"
            )
        ),
        organizations = listOf(
            Organisation(
                id = 1,
                title = "IT $query Community",
                description = "Сообщество разработчиков и IT-специалистов",
                avatar = null,
                city = "Москва",
                status = "approved",
                tags = listOf("IT", query, "технологии"),
                social_links = emptyList(),
                events_count = 5,
                subscribers_count = 150,
                owner_id = 1
            )
        ),
        total_events = 2,
        total_organizations = 1
    )
}

