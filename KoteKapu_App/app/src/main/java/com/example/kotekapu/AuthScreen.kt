import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.kotekapu.ui.theme.KoteKapuTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNextClicked: (String, String) -> Unit,
    onLoginClicked: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Text(
            text = "Вход",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))


        // Поле почты
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле пароля
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Call
                        else Icons.Default.Face,
                        contentDescription = if (passwordVisible) "Скрыть пароль"
                        else "Показать пароль"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))


        // Кнопка Далее
        Button(
            onClick = {
                if (validateForm(email, password)) {
                    onNextClicked(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Далее", style = MaterialTheme.typography.labelLarge)
        }
        
        
    }
}

// Функция валидации
private fun validateForm(
    email: String,
    password: String,
): Boolean {
    return when {
        email.isEmpty() || !isValidEmail(email) -> {
            // Показать ошибку email
            false
        }
        password.length < 6 -> {
            // Показать ошибку пароля
            false
        }
        else -> true
    }
}

private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}


class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoteKapuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen(
                        onNextClicked = {email, password ->
                            // Обработка регистрации
                            Toast.makeText(this, "Авторизация успешна!", Toast.LENGTH_SHORT).show()
                        },
                        onLoginClicked = {
                            // Переход на экран входа
                            // startActivity(Intent(this, LoginActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    KoteKapuTheme {
        AuthScreen(
            onNextClicked = {email, password ->
                // Preview action
            },
            onLoginClicked = {
                // Preview action
            }
        )
    }
}