import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: User,
    onBackClick: () -> Unit,
    onSaveClick: (User) -> Unit,
    onAvatarChange: () -> Unit
) {
    var editedUser by remember { mutableStateOf(user) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование профиля") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val validationErrors = validateProfile(editedUser)
                            if (validationErrors.isEmpty()) {
                                onSaveClick(editedUser)
                            } else {
                                errors = validationErrors
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Аватар
            AvatarSection(
                user = editedUser,
                onAvatarChange = onAvatarChange,
                modifier = Modifier.padding(16.dp)
            )

            // Основная информация
            PersonalInfoSection(
                user = editedUser,
                errors = errors,
                onUserChange = { newUser ->
                    editedUser = newUser
                    if (errors.isNotEmpty()) errors = validateProfile(newUser)
                },
                modifier = Modifier.padding(16.dp)
            )

            // Контактная информация
            ContactInfoSection(
                user = editedUser,
                onUserChange = { newUser ->
                    editedUser = newUser
                },
                modifier = Modifier.padding(16.dp)
            )

            // О себе
            AboutSection(
                user = editedUser,
                onUserChange = { newUser ->
                    editedUser = newUser
                },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun AvatarSection(
    user: User,
    onAvatarChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl != null) {
                    // TODO: Загрузка изображения с Coil/Glide
                    Text(
                        text = "Фото",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = user.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAvatarChange,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Face, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Изменить фото")
            }

            Text(
                text = "Рекомендуемый размер: 500x500 px",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PersonalInfoSection(
    user: User,
    errors: Map<String, String>,
    onUserChange: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Основная информация",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ФИО
        OutlinedTextField(
            value = user.name,
            onValueChange = { newValue ->
                onUserChange(user.copy(name = newValue))
            },
            label = { Text("ФИО *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errors.containsKey("name"),
            supportingText = {
                if (errors.containsKey("name")) {
                    Text(text = errors["name"]!!)
                }
            },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Статус
        OutlinedTextField(
            value = user.status,
            onValueChange = { newValue ->
                onUserChange(user.copy(status = newValue))
            },
            label = { Text("Статус") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("Например: Студент, Разработчик, Активный участник")
            },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Город
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = user.city,
                onValueChange = { newValue ->
                    onUserChange(user.copy(city = newValue))
                },
                label = { Text("Город") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = errors.containsKey("city"),
                supportingText = {
                    if (errors.containsKey("city")) {
                        Text(text = errors["city"]!!)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )

            IconButton(
                onClick = { /* TODO: Открыть выбор города */ },
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Выбрать город")
            }
        }
    }
}

@Composable
fun ContactInfoSection(
    user: User,
    onUserChange: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Контактная информация",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Email
        OutlinedTextField(
            value = user.email,
            onValueChange = { newValue ->
                onUserChange(user.copy(email = newValue))
            },
            label = { Text("Email *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = false, // Email обычно нельзя менять
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            supportingText = {
                Text("Для изменения email обратитесь в поддержку")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Телефон
        OutlinedTextField(
            value = user.phone,
            onValueChange = { newValue ->
                onUserChange(user.copy(phone = newValue))
            },
            label = { Text("Телефон") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            },
            supportingText = {
                Text("Будет виден только организаторам мероприятий, на которые вы зарегистрированы")
            }
        )
    }
}

@Composable
fun AboutSection(
    user: User,
    onUserChange: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "О себе",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Образование
        EducationSection(
            education = user.education,
            onEducationChange = { newEducation ->
                onUserChange(user.copy(education = newEducation))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Биография
        OutlinedTextField(
            value = user.bio,
            onValueChange = { newValue ->
                onUserChange(user.copy(bio = newValue))
            },
            label = { Text("О себе") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            supportingText = {
                Text("${user.bio.length}/500")
            }
        )
    }
}

@Composable
fun EducationSection(
    education: String,
    onEducationChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = education,
            onValueChange = { },
            label = { Text("Учебное заведение") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { isExpanded = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Выбрать")
                }
            },
            supportingText = {
                Text("Школа, колледж, университет")
            }
        )

        // Быстрый выбор популярных учебных заведений
        if (education.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Школа", "Колледж", "Университет", "Работаю").forEach { item ->
                    SuggestionChip(
                        onClick = { onEducationChange(item) },
                        label = { Text(item) }
                    )
                }
            }
        }

        // Диалог выбора учебного заведения
        if (isExpanded) {
            AlertDialog(
                onDismissRequest = { isExpanded = false },
                title = { Text("Выберите учебное заведение") },
                text = {
                    EducationSearchDialog(
                        selectedEducation = education,
                        onEducationSelect = { selected ->
                            onEducationChange(selected)
                            isExpanded = false
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { isExpanded = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun EducationSearchDialog(
    selectedEducation: String,
    onEducationSelect: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val popularInstitutions = listOf(
        "МГУ им. М.В. Ломоносова",
        "МГТУ им. Н.Э. Баумана",
        "НИУ ВШЭ",
        "МФТИ",
        "СПбГУ",
        "НГУ",
        "КФУ",
        "УрФУ"
    )

    val filteredInstitutions = popularInstitutions.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    Column {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Список учебных заведений
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(filteredInstitutions) { institution ->
                Card(
                    onClick = { onEducationSelect(institution) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (institution == selectedEducation)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = institution,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Ручной ввод
        if (searchQuery.isNotBlank() && filteredInstitutions.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onEducationSelect(searchQuery) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Добавить \"$searchQuery\"")
            }
        }
    }
}

@Composable
fun PrivacySettingsSection(
    user: User,
    onUserChange: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Настройки приватности",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close
                        else Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Настройки приватности
                PrivacySettingItem(
                    title = "Показывать email в профиле",
                    subtitle = "Будет виден другим пользователям",
                    checked = user.isEmailPublic,
                    onCheckedChange = { newValue ->
                        onUserChange(user.copy(isEmailPublic = newValue))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                PrivacySettingItem(
                    title = "Показывать телефон в профиле",
                    subtitle = "Будет виден другим пользователям",
                    checked = user.isPhonePublic,
                    onCheckedChange = { newValue ->
                        onUserChange(user.copy(isPhonePublic = newValue))
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                PrivacySettingItem(
                    title = "Показывать участие в мероприятиях",
                    subtitle = "Будет видно, на какие мероприятия вы ходите",
                    checked = user.isEventsPublic,
                    onCheckedChange = { newValue ->
                        onUserChange(user.copy(isEventsPublic = newValue))
                    }
                )
            }
        }
    }
}

@Composable
fun PrivacySettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun validateProfile(user: User): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (user.name.isBlank()) {
        errors["name"] = "Введите ФИО"
    } else if (user.name.length < 2) {
        errors["name"] = "ФИО должно содержать минимум 2 символа"
    }

    if (user.email.isBlank()) {
        errors["email"] = "Введите email"
    } else if (!isValidEmail(user.email)) {
        errors["email"] = "Введите корректный email"
    }

    if (user.phone.isNotBlank() && !isValidPhone(user.phone)) {
        errors["phone"] = "Введите корректный номер телефона"
    }

    if (user.bio.length > 500) {
        errors["bio"] = "Описание не должно превышать 500 символов"
    }

    return errors
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    return email.matches(emailRegex.toRegex())
}

private fun isValidPhone(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}\$"
    return phone.matches(phoneRegex.toRegex())
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    MaterialTheme {
        EditProfileScreen(
            user = samplePersonalUser,
            onBackClick = {},
            onSaveClick = {},
            onAvatarChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PersonalInfoSectionPreview() {
    MaterialTheme {
        PersonalInfoSection(
            user = samplePersonalUser,
            errors = mapOf("name" to "Обязательное поле"),
            onUserChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EducationSectionPreview() {
    MaterialTheme {
        EducationSection(
            education = "",
            onEducationChange = {}
        )
    }
}