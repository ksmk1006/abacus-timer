package com.example.abacustimer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

// --- DATA CLASS FOR STORAGE ---
data class SessionData(
    val date: String,
    val digits: Int,
    val rows: Int,
    val timeTaken: String,
    val status: String
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
                    AbacusApp()
                }
            }
        }
    }
}

// --- HELPER FUNCTIONS FOR LOCAL STORAGE ---
fun saveSession(context: Context, session: SessionData) {
    val prefs = context.getSharedPreferences("AbacusPrefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val existingJson = prefs.getString("history", "[]")
    val type = object : TypeToken>() {}.type
    val history: MutableList = gson.fromJson(existingJson, type) ?: mutableListOf()
    
    history.add(0, session) // Add newest to the top
    prefs.edit().putString("history", gson.toJson(history)).apply()
}

fun getHistory(context: Context): List {
    val prefs = context.getSharedPreferences("AbacusPrefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val existingJson = prefs.getString("history", "[]")
    val type = object : TypeToken>() {}.type
    return gson.fromJson(existingJson, type) ?: emptyList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbacusApp() {
    var currentScreen by remember { mutableStateOf("Timer") }
    val context = LocalContext.current

    if (currentScreen == "Timer") {
        TimerScreen(
            onNavigateToHistory = { currentScreen = "History" },
            onSessionComplete = { digits, rows, time, isCorrect ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                val session = SessionData(
                    date = dateFormat.format(Date()),
                    digits = digits,
                    rows = rows,
                    timeTaken = time,
                    status = if (isCorrect) "Correct" else "Wrong"
                )
                saveSession(context, session)
            }
        )
    } else {
        HistoryScreen(
            onNavigateBack = { currentScreen = "Timer" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onNavigateToHistory: () -> Unit, onSessionComplete: (Int, Int, String, Boolean) -> Unit) {
    var digits by remember { mutableIntStateOf(1) }
    var rows by remember { mutableIntStateOf(5) }
    var isRunning by remember { mutableStateOf(false) }
    var timeInSeconds by remember { mutableIntStateOf(0) }
    var resultMessage by remember { mutableStateOf("") }

    // Timer Logic
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            timeInSeconds++
        }
    }

    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Abacus Practice") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.List, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = timeString, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Digits: $digits", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { if (digits > 1) digits-- }) { Text("-") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { digits++ }) { Text("+") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rows: $rows", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { if (rows > 1) rows-- }) { Text("-") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { rows++ }) { Text("+") }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isRunning) {
                Button(
                    onClick = {
                        isRunning = true
                        timeInSeconds = 0
                        resultMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Start Practice", fontSize = 18.sp)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        onClick = {
                            isRunning = false
                            resultMessage = "Saved as Correct!"
                            onSessionComplete(digits, rows, timeString, true)
                        }
                    ) {
                        Text("Mark Correct")
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        onClick = {
                            isRunning = false
                            resultMessage = "Saved as Wrong."
                            onSessionComplete(digits, rows, timeString, false)
                        }
                    ) {
                        Text("Mark Wrong")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (resultMessage.isNotEmpty()) {
                Text(text = resultMessage, fontSize = 18.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    // Load history once when screen opens
    val historyList = remember { getHistory(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No sessions saved yet.", fontSize = 18.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = session.date, fontWeight = FontWeight.Bold)
                                Text(
                                    text = session.status,
                                    color = if (session.status == "Correct") Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Configuration: ${session.digits} Digits x ${session.rows} Rows")
                            Text(text = "Time Taken: ${session.timeTaken}")
                        }
                    }
                }
            }
        }
    }
}
