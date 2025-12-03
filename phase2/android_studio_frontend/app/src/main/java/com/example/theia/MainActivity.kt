package com.example.theia

import android.os.Bundle
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
    HOME, EMERGENCY, GUIDANCE, MANAGER_LOGIN, MAP_DASHBOARD
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
            AppScreen.GUIDANCE -> PlaceholderScreen("Guidance view (placeholder)", onBack = { screen = AppScreen.HOME }, modifier = Modifier.padding(padding))
            AppScreen.Manager_LOGIN -> ManagerLoginScreen(
                onSuccess = { screen = AppScreen.MANAGER_DASHBOARD },
                onBack = { screen = AppScreen.HOME },
                modifier = Modifier.padding(padding)
            )

            AppScreen.M_DASHBOARD ->
                ManagerDashboardScreen(
                    onRegionStatus = { /* TODO next step */ },
                    onAccessibleAreas = { /* TODO next step */ },
                    onBack = { screen = AppScreen.HOME },
                    modifier = Modifier.padding(padding)
                )
        }
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
            Text("Guidance (placeholder)")
        }
        Button(
            onClick = { onNavigate(AppScreen.MAP) },
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
private fun ManagerLoginScreen(text: String, onSuccess: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Manager Login", = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        androidx.compose.material3.OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth
        )
        androidx.compose.material3.OutlinedTextField(
            value = password,
            onValueChange = { username = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth
        )

        Button(
            onClick = {if (isLoading) return @Button
                      isLoading=true
                      status = "Logging in..."},

                ApiClient.api.managerLogin(
                    LoginRequest(username, password)
                ).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        isLoading = false
                        if (response.isSuccessful && response.body()?.success == true) {
                            status = "Login successful!"
                            onSuccess()
                    } else {
                        status = "Invalid credentials"
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    isLoading = false
                    status = "Network error: ${t.localizedMessage}"
                }
            })
        },
        modifier = Modifier.fillMaxWidth()
    ) {
            Test(if (isLoading) " Please wait" else "Login")
    }
    test(status)

    Button( onClick = onBack, modifier = Modifier.fillMaxWidth()
    ){
        Text("Back")
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
