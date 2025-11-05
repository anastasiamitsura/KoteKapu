// HelpScreen.kt
package com.example.kotekapu_2.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kotekapu_2.ContactItem
import com.example.kotekapu_2.FAQItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    val faqItems = listOf(
        FAQItem(
            question = "Как зарегистрироваться в приложении?",
            answer = "Нажмите кнопку 'Регистрация' на стартовом экране, заполните обязательные поля (email, пароль, имя, фамилия) и следуйте инструкциям по онбордингу."
        ),
        FAQItem(
            question = "Как работает система рекомендаций?",
            answer = "Система анализирует ваши интересы из опроса предпочтений, историю лайков и регистраций на мероприятия, чтобы предлагать наиболее релевантные события."
        ),
        FAQItem(
            question = "Как искать мероприятия?",
            answer = "Используйте экран поиска (иконка лупы). Вы можете искать по названию, применять фильтры по тегам, форматам, датам и локациям."
        ),
        FAQItem(
            question = "Как создать организацию?",
            answer = "Перейдите в профиль → 'Мои организации' → 'Создать организацию'. Заполните информацию и дождитесь модерации."
        ),
        FAQItem(
            question = "Как отменить регистрацию на мероприятие?",
            answer = "В данный момент отмена регистрации недоступна через приложение. Свяжитесь с организатором мероприятия напрямую."
        ),
        FAQItem(
            question = "Что такое баллы и достижения?",
            answer = "Баллы начисляются за активность в приложении (регистрация, лайки, участие). Достижения открываются при выполнении определенных условий."
        )
    )

    val contactItems = listOf(
        ContactItem(
            type = "Email поддержки",
            value = "support@kotekapu.ru",
            icon = Icons.Default.Email
        ),
        ContactItem(
            type = "Телефон",
            value = "+7 (999) 123-45-67",
            icon = Icons.Default.Phone
        ),
        ContactItem(
            type = "Telegram канал",
            value = "@kotekapu_support",
            icon = Icons.Default.Message
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Помощь") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Заголовок
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = "Помощь",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Центр помощи",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Здесь вы найдете ответы на часто задаваемые вопросы",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Быстрый старт
            item {
                Text(
                    text = "Быстрый старт",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                QuickStartGuide()
            }

            // FAQ
            item {
                Text(
                    text = "Часто задаваемые вопросы",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(faqItems) { faqItem ->
                FAQCard(
                    faqItem = faqItem,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Контакты
            item {
                Text(
                    text = "Контакты поддержки",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(contactItems) { contactItem ->
                ContactCard(
                    contactItem = contactItem,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Дополнительная информация
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Нужна дополнительная помощь?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Если вы не нашли ответ на свой вопрос, свяжитесь с нашей службой поддержки. Мы отвечаем в течение 24 часов.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun QuickStartGuide() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            QuickStartStep(
                number = 1,
                title = "Регистрация и онбординг",
                description = "Заполните профиль и пройдите опрос предпочтений для персонализированных рекомендаций"
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            QuickStartStep(
                number = 2,
                title = "Исследуйте ленту",
                description = "Просматривайте рекомендованные мероприятия, ставьте лайки и регистрируйтесь"
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            QuickStartStep(
                number = 3,
                title = "Используйте поиск",
                description = "Находите мероприятия по интересам с помощью фильтров и поиска"
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            QuickStartStep(
                number = 4,
                title = "Создавайте организации",
                description = "Станьте организатором и создавайте собственные мероприятия"
            )
        }
    }
}

@Composable
fun QuickStartStep(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Номер шага
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(32.dp)
                    .wrapContentHeight(Alignment.CenterVertically),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Описание шага
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FAQCard(faqItem: FAQItem, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faqItem.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = faqItem.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ContactCard(contactItem: ContactItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = contactItem.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contactItem.type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contactItem.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

