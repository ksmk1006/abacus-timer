package com.example.abacustimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

data class SessionLog(
    val timeFormatted: String,
    val total: Int,
    val correct: Int,
    val accuracy: String,
    val speed: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AbacusTimerApp()
                }
            }
        }
    }
}

@Composable
fun AbacusTimerApp() {
    var timeMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    var totalQuestionsText by remember { mutableStateOf("20") }
    var correctAnswersText by remember { mutableStateOf("18") }

    val historyList = remember { mutableStateListOf<SessionLog>() }

    // Stopwatch loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10) // Updates every 10ms
            timeMillis += 10
        }
    }

    // Dynamic calculations
    val totalSecs = timeMillis / 1000.0
    val totalQ = totalQuestionsText.toIntOrNull() ?: 0
    val correctQ = correctAnswersText.toIntOrNull() ?: 0

    val accuracy = if (totalQ > 0) (correctQ.toDouble() / totalQ * 100).coerceAtMost(100.0) else 0.0
    val avgSpeed = if (totalQ > 0 && totalSecs > 0) totalSecs / totalQ else 0.0
    val qpm = if (totalSecs > 0) (totalQ / totalSecs) * 60 else 0.0

    // Time formatting
    val minutes = (timeMillis / 1000) / 60
    val seconds = (timeMillis / 1000) % 60
    val hundredths = (timeMillis % 1000) / 10
    val timerDisplay = String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Abacus Performance Tracker",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stopwatch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerDisplay,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isRunning) "Pause" else "Start")
                    }

                    OutlinedButton(onClick = {
                        isRunning = false
                        timeMillis = 0L
                    }) {
                        Text("Reset")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = totalQuestionsText,
                onValueChange = { totalQuestionsText = it },
                label = { Text("Total Questions") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = correctAnswersText,
                onValueChange = { correctAnswersText = it },
                label = { Text("Correct Answers") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Performance Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(label = "Accuracy", value = String.format(Locale.US, "%.1f%%", accuracy))
                MetricItem(label = "Avg Speed", value = String.format(Locale.US, "%.2f s/q", avgSpeed))
                MetricItem(label = "QPM", value = String.format(Locale.US, "%.1f", qpm))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (totalQ > 0) {
                    historyList.add(
                        0,
                        SessionLog(
                            timeFormatted = timerDisplay,
                            total = totalQ,
                            correct = correctQ,
                            accuracy = String.format(Locale.US, "%.1f%%", accuracy),
                            speed = String.format(Locale.US, "%.2fs/q", avgSpeed)
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Session to Log")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session History List
        Text(
            text = "Session History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(historyList) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⏱ ${item.timeFormatted}", fontWeight = FontWeight.Bold)
                        Text("Score: ${item.correct}/${item.total}")
                        Text("Acc: ${item.accuracy}")
                        Text("Speed: ${item.speed}")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
