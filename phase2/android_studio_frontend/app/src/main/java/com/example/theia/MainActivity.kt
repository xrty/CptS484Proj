package com.example.theia

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.theia.network.ApiClient
import com.example.theia.network.FallAlert
import com.example.theia.network.GuidanceRequest
import com.example.theia.network.GuidanceResponse
import com.example.theia.network.GuidanceStep
import com.example.theia.network.Hallway
import com.example.theia.network.HallwayUpdateRequest
import com.example.theia.network.LoginRequest
import com.example.theia.network.LoginResponse
import com.example.theia.ui.theme.TheiaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheiaTheme {
                AppShell()
            }
        }
    }
}

private enum class AppScreen {
    HOME,
    EMERGENCY,
    GUIDANCE,
    MANAGER_LOGIN,
    MANAGER_DASHBOARD,
    REGION_STATUS
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppShell() {
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Theia Prototype") })
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                onNavigate = { screen = it },
                modifier = Modifier.padding(padding)
            )
            AppScreen.EMERGENCY -> EmergencyScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding)
            )
            AppScreen.GUIDANCE -> GuidanceScreen(
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding)
            )
            AppScreen.MANAGER_LOGIN -> ManagerLoginScreen(
                onSuccess = { screen = AppScreen.MANAGER_DASHBOARD },
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding)
            )
            AppScreen.MANAGER_DASHBOARD -> ManagerDashboardScreen(
                onRegionStatus = { screen = AppScreen.REGION_STATUS },
                onAccessibleAreas = { /* TODO next step */ },
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding)
            )
            AppScreen.REGION_STATUS -> HallwayStatusScreen(
                onBack = { screen = AppScreen.MANAGER_DASHBOARD },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun HomeScreen(onNavigate: (AppScreen) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Choose a module to open. Only Emergency currently calls the real API.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onNavigate(AppScreen.EMERGENCY) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Emergency notifications")
        }
        Button(
            onClick = { onNavigate(AppScreen.GUIDANCE) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guidance")
        }
        Button(
            onClick = { onNavigate(AppScreen.MANAGER_LOGIN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Building Manager Login")
        }
    }
}

@Composable
private fun EmergencyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Idle") }
    var isSending by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun resetCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        countdownSeconds = null
    }

    fun sendFallAlert() {
        if (isSending) return
        resetCountdown()
        isSending = true
        status = "Sending fall alert..."
        val alert = FallAlert(
            user_id = 1,
            latitude = 47.1234,
            longitude = -122.5678
        )
        ApiClient.api.sendFallAlert(alert).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(
                call: Call<Map<String, Any>>,
                response: Response<Map<String, Any>>
            ) {
                isSending = false
                status = if (response.isSuccessful) {
                    val msg = response.body()?.get("message") ?: "Fall alert sent."
                    "Success: $msg"
                } else {
                    "Error ${response.code()}: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                isSending = false
                val err = t.localizedMessage ?: "unknown"
                status = "Network error: $err"
            }
        })
    }

    fun startCountdown() {
        if (isSending || countdownSeconds != null) return
        status = "Trigger armed. Sending shortly..."
        countdownJob = scope.launch {
            for (seconds in 5 downTo 1) {
                countdownSeconds = seconds
                delay(1000)
            }
            countdownSeconds = null
            sendFallAlert()
        }
    }

    fun cancelCountdown() {
        if (countdownSeconds == null) return
        resetCountdown()
        status = "Cancelled"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Emergency notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Sends a dummy fall alert to the FastAPI backend (/alerts/fall).")
        Button(
            onClick = { startCountdown() },
            enabled = !isSending && countdownSeconds == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    isSending -> "Sending..."
                    countdownSeconds != null -> "Armed..."
                    else -> "Trigger fall event"
                }
            )
        }
        countdownSeconds?.let { seconds ->
            Text("Sending fall alert in $seconds s... Tap cancel to stop.")
            Button(
                onClick = { cancelCountdown() },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home")
        }
    }
}

@Composable
private fun GuidanceScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(
            context,
            object : TextToSpeech.OnInitListener {
                override fun onInit(status: Int) {
                    if (status == TextToSpeech.SUCCESS) {
                        tts?.language = Locale.US
                    }
                }
            }
        )
    }

    var currentLocation by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Enter locations and press Get Guidance") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Guidance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = currentLocation,
            onValueChange = { currentLocation = it },
            label = { Text("Current location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (loading) return@Button
                loading = true
                result = "Requesting guidance..."

                val request = GuidanceRequest(
                    current_location = currentLocation.ifBlank { "Current hallway" },
                    destination = destination.ifBlank { "Next classroom" }
                )

                ApiClient.guidanceApi.getRoute(request)
                    .enqueue(object : Callback<GuidanceResponse> {
                        override fun onResponse(
                            call: Call<GuidanceResponse>,
                            response: Response<GuidanceResponse>
                        ) {
                            loading = false
                            if (response.isSuccessful) {
                                val body = response.body()!!
                                val stepsText = body.steps.joinToString("\n") { step: GuidanceStep ->
                                    "${step.order}. ${step.instruction}"
                                }
                                result = "Summary:\n${body.summary}\n\nSteps:\n$stepsText"
                                val params = Bundle()
                                tts?.speak(
                                    body.summary,
                                    TextToSpeech.QUEUE_FLUSH,
                                    params,
                                    "guidance"
                                )
                            } else {
                                result = "Error ${response.code()}: ${response.message()}"
                            }
                        }

                        override fun onFailure(call: Call<GuidanceResponse>, t: Throwable) {
                            loading = false
                            result = "Network error: ${t.localizedMessage ?: "unknown"}"
                        }
                    })
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            Text(if (loading) "Loading..." else "Get Guidance")
        }

        Text(result, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Wire this up later.")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home")
        }
    }
}

@Composable
private fun ManagerLoginScreen(onSuccess: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun attemptLogin() {
        if (username.isBlank() || password.isBlank()) {
            status = "Please enter a username and password."
            return
        }
        if (isLoading) return

        isLoading = true
        status = "Logging in..."

        ApiClient.api.managerLogin(LoginRequest(username, password))
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful && response.body()?.success == true) {
                        status = "Login successful"
                        onSuccess()
                    } else {
                        status = "Invalid credentials"
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    isLoading = false
                    status = "Network error: ${t.localizedMessage ?: "unknown"}"
                }
            })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Manager Login", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Button(
            onClick = { attemptLogin() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Please wait..." else "Login")
        }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun HallwayStatusScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var hallways by remember { mutableStateOf<List<Hallway>>(emptyList()) }
    var status by remember { mutableStateOf("Loading hallways...") }
    var isLoading by remember { mutableStateOf(false) }
    var updatingHallwayId by remember { mutableStateOf<Int?>(null) }

    fun refreshHallways() {
        isLoading = true
        status = "Loading hallways..."
        ApiClient.api.getHallways().enqueue(object : Callback<List<Hallway>> {
            override fun onResponse(
                call: Call<List<Hallway>>,
                response: Response<List<Hallway>>
            ) {
                isLoading = false
                if (response.isSuccessful) {
                    hallways = response.body().orEmpty()
                    status = "Loaded ${hallways.size} hallways"
                } else {
                    status = "Error ${response.code()}: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<List<Hallway>>, t: Throwable) {
                isLoading = false
                status = "Network error: ${t.localizedMessage ?: "unknown"}"
            }
        })
    }

    fun toggleHallway(hallway: Hallway) {
        val nextStatus = if (hallway.status == "available") "under_construction" else "available"
        updatingHallwayId = hallway.id
        status = "Updating ${hallway.name}..."
        ApiClient.api.updateHallwayStatus(hallway.id, HallwayUpdateRequest(status = nextStatus))
            .enqueue(object : Callback<Hallway> {
                override fun onResponse(call: Call<Hallway>, response: Response<Hallway>) {
                    updatingHallwayId = null
                    if (response.isSuccessful) {
                        val updated = response.body()
                        if (updated != null) {
                            hallways = hallways.map { if (it.id == updated.id) updated else it }
                            val friendly = updated.status.replace("_", " ")
                            status = "Set ${updated.name} to $friendly"
                        }
                    } else {
                        status = "Update failed: ${response.code()} ${response.message()}"
                    }
                }

                override fun onFailure(call: Call<Hallway>, t: Throwable) {
                    updatingHallwayId = null
                    status = "Network error: ${t.localizedMessage ?: "unknown"}"
                }
            })
    }

    LaunchedEffect(Unit) { refreshHallways() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Change Region Status",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text("Toggle hallways between available/under construction and sync to the backend.")
        Text(status, style = MaterialTheme.typography.bodyMedium)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(hallways) { hallway ->
                HallwayCard(
                    hallway = hallway,
                    isUpdating = updatingHallwayId == hallway.id || isLoading,
                    onToggle = { toggleHallway(hallway) }
                )
            }
        }
        Button(
            onClick = { refreshHallways() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Refreshing..." else "Refresh hallways")
        }
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun HallwayCard(
    hallway: Hallway,
    isUpdating: Boolean,
    onToggle: () -> Unit
) {
    val friendlyStatus = hallway.status.replace("_", " ")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(hallway.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Status: $friendlyStatus", style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onToggle,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (hallway.status == "available") "Mark under construction" else "Mark available"
                )
            }
        }
    }
}

@Composable
private fun ManagerDashboardScreen(
    onRegionStatus: () -> Unit,
    onAccessibleAreas: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Manager Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Button(
            onClick = onRegionStatus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Region Status")
        }

        Button(
            onClick = onAccessibleAreas,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Accessible Areas")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    TheiaTheme { HomeScreen(onNavigate = {}, modifier = Modifier.fillMaxSize()) }
}
