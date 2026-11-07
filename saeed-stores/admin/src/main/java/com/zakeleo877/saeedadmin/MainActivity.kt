package com.zakeleo877.saeedadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Dashboard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.theme.NeonVoidAppTheme
import com.zakeleo877.saeedcore.theme.NeonVoidBackground
import com.zakeleo877.saeedcore.theme.NeonVoidPrimary
import com.zakeleo877.saeedcore.theme.NeonVoidSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeonVoidAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeonVoidBackground
                ) {
                    AdminScreen()
                }
            }
        }
    }
}

@Composable
fun AdminScreen() {
    val loginEmail by remember { mutableStateOf("") }
    val loginPass by remember { mutableStateOf("") }
    val showStatus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = NeonVoidSecondary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "لوحة",
                    tint = NeonVoidPrimary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        },
        containerColor = NeonVoidBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = NeonVoidPrimary.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.scaffold_message),
                            color = NeonVoidSecondary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("البريد الإلكتروني") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = loginPass,
                            onValueChange = { loginPass = it },
                            label = { Text("كلمة المرور") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        Button(
                            onClick = {
                                // سيتم تنفيذ تسجيل الدخول (Auth) وادعاء المدير (claim_first_admin)
                                showStatus = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = Button.colors(containerColor = NeonVoidSecondary)
                        ) {
                            Text(
                                text = "تسجيل الدخول",
                                color = Color.Black
                            )
                        }

                        if (showStatus) {
                            Text(
                                text = "جاري التحقق من الهوية (is_admin)...",
                                color = NeonVoidPrimary,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}