package com.example.theia
import androidx.compose.runtime.LaunchedEffect
import android.os.Bundle
import java.util.Locale
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.theia.network.ApiClient
import com.example.theia.network.FallAlert
import com.example.theia.ui.theme.TheiaTheme
import com.example.theia.network.GuidanceRequest
import com.example.theia.network.GuidanceResponse
import com.example.theia.network.GuidanceStep
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    HOME, EMERGENCY, GUIDANCE, MAP
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
            AppScreen.EMERGENCY -> EmergencyScreen(onBack = { screen = AppScreen.HOME }, modifier = Modifier.padding(padding))
            AppScreen.GUIDANCE -> GuidanceScreen(onBack = { screen = AppScreen.HOME }, modifier = Modifier.padding(padding))
            AppScreen.MAP -> PlaceholderScreen("Map view (placeholder)", onBack = { screen = AppScreen.HOME }, modifier = Modifier.padding(padding))
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
            onClick = { onNavigate(AppScreen.MAP) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Building map (placeholder)")
        }
    }
}

@Composable
private fun EmergencyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var status by remember { mutableStateOf("Idle") }
    var isSending by remember { mutableStateOf(false) }

    fun sendFallAlert() {
        if (isSending) return
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
            onClick = { sendFallAlert() },
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSending) "Sending..." else "Send fall alert")
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
    val context = LocalContext.current   // <-- 新增这一行

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

        androidx.compose.material3.OutlinedTextField(
            value = currentLocation,
            onValueChange = { currentLocation = it },
            label = { Text("Current location") },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.OutlinedTextField(
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
                                val stepsText = body.steps.joinToString("\n") { step ->
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

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    TheiaTheme { HomeScreen(onNavigate = {}, modifier = Modifier.fillMaxSize()) }
}
