package com.zakeleo877.saeedstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.icons.filled.ShoppingBasket
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
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
                    CustomerScreen()
                }
            }
        }
    }
}

@Composable
fun CustomerScreen() {
    val customerName by remember { mutableStateOf("") }
    val phone by remember { mutableStateOf("") }
    val notes by remember { mutableStateOf("") }
    val location by remember { mutableStateOf("") }
    val showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = NeonVoidPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Icon(
                    imageVector = Icons.Default.ShoppingBasket,
                    contentDescription = "سلة",
                    tint = NeonVoidSecondary,
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
                        containerColor = NeonVoidSecondary.copy(alpha = 0.1f),
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
                            color = NeonVoidPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("الاسم") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("ملاحظات") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("الموقع (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                // سيتم تنفيذ طابور الطلبات (WorkManager) هنا
                                showSuccess = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = Button.colors(containerColor = NeonVoidPrimary)
                        ) {
                            Text(
                                text = "إرسال الطلب",
                                color = Color.Black
                            )
                        }

                        if (showSuccess) {
                            Text(
                                text = "تم إرسال الطلب! سيصل إشعار محلي عند النجاح.",
                                color = NeonVoidSecondary,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}