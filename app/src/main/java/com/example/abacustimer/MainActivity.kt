package com.example.abacustimer

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var timeMillis by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    var totalQuestionsText by remember { mutableStateOf("20") }
    var correctAnswersText by remember { mutableStateOf("18") }
    var sheetUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val historyList = remember { mutableStateListOf<SessionLog>() }

    // Stopwatch loop
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            timeMillis += 10
        }
    }

    // Calculations
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

        Spacer(modifier = Modifier.height(12.dp))

        // Stopwatch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerDisplay,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // Google Sheet URL Input
        OutlinedTextField(
            value = sheetUrl,
            onValueChange = { sheetUrl = it },
            label = { Text("Google Sheet Web App URL") },
            placeholder = { Text("Paste Web App URL here") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricItem(label = "Accuracy", value = String.format(Locale.US, "%.1f%%", accuracy))
                MetricItem(label = "Avg Speed", value = String.format(Locale.US, "%.2f s/q", avgSpeed))
                MetricItem(label = "QPM", value = String.format(Locale.US, "%.1f", qpm))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save Button
        Button(
            onClick = {
                if (totalQ > 0) {
                    val accStr = String.format(Locale.US, "%.1f%%", accuracy)
                    val speedStr = String.format(Locale.US, "%.2fs/q", avgSpeed)
                    val qpmStr = String.format(Locale.US, "%.1f", qpm)

                    // Save locally
                    historyList.add(
                        0,
                        SessionLog(
                            timeFormatted = timerDisplay,
                            total = totalQ,
                            correct = correctQ,
                            accuracy = accStr,
                            speed = speedStr
                        )
                    )

                    // Sync to Google Sheet if URL provided
                    if (sheetUrl.isNotBlank()) {
                        isSaving = true
                        coroutineScope.launch {
                            val success = syncToGoogleSheet(
                                webAppUrl = sheetUrl.trim(),
                                time = timerDisplay,
                                total = totalQ,
                                correct = correctQ,
                                accuracy = accStr,
                                speed = speedStr,
                                qpm = qpmStr
                            )
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Saved to Google Sheet!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save to Google Sheet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Saved locally (Paste Sheet URL to sync online)", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Saving..." else "Save Session to Google Sheet")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Log
        Text(
            text = "Session History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(4.dp))

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

// Background Network Request to Google Apps Script Web App
suspend fun syncToGoogleSheet(
    webAppUrl: String,
    time: String,
    total: Int,
    correct: Int,
    accuracy: String,
    speed: String,
    qpm: String
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val queryParams = "time=${URLEncoder.encode(time, "UTF-8")}" +
                    "&total=$total" +
                    "&correct=$correct" +
                    "&accuracy=${URLEncoder.encode(accuracy, "UTF-8")}" +
                    "&speed=${URLEncoder.encode(speed, "UTF-8")}" +
                    "&qpm=${URLEncoder.encode(qpm, "UTF-8")}"

            val fullUrl = if (webAppUrl.contains("?")) "$webAppUrl&$queryParams" else "$webAppUrl?$queryParams"
            val url = URL(fullUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            responseCode in 200..399
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
