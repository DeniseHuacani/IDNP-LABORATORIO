package com.example.batterymonitor_compose

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.batterymonitor_compose.ui.theme.BatteryMonitor_ComposeTheme

// Ejercicio 1: acción personalizada para el broadcast manual
const val ACTION_ACTUALIZAR_BATERIA = "com.tuapp.ACTUALIZAR_BATERIA"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryMonitor_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BatteryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BatteryScreen(modifier: Modifier = Modifier) {
    // Paso 2 (ejercicio resuelto): Variable de estado para almacenar el porcentaje
    var porcentaje by remember { mutableStateOf(0) }

    // Ejercicio 4: Variable de estado para saber si está cargando
    var cargando by remember { mutableStateOf(false) }

    // Paso 3 (ejercicio resuelto): Obtener el contexto actual
    val context = LocalContext.current

    // Pasos 4, 5 y 6 (ejercicio resuelto): Registrar y desregistrar el BroadcastReceiver automático
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val nivel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val escala = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

                if (nivel != -1 && escala != -1) {
                    porcentaje = (nivel * 100) / escala
                }

                cargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }
        }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        Log.d("BatteryScreen", "Receiver registrado")

        onDispose {
            context.unregisterReceiver(receiver)
            Log.d("BatteryScreen", "Receiver desregistrado")
        }
    }

    // Ejercicio 2: definición del segundo BroadcastReceiver
    val receiverManual = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val batteryStatus = context?.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )

                val nivel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val escala = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

                if (nivel != -1 && escala != -1) {
                    porcentaje = (nivel * 100) / escala
                }

                cargando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                Log.d("BatteryScreen", "Actualización manual recibida")
            }
        }
    }

    // Ejercicio 3: Registrar y desregistrar el receiver manual con su propio DisposableEffect
    DisposableEffect(Unit) {
        ContextCompat.registerReceiver(
            context,
            receiverManual,
            IntentFilter(ACTION_ACTUALIZAR_BATERIA),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d("BatteryScreen", "Receiver manual registrado")

        onDispose {
            context.unregisterReceiver(receiverManual)
            Log.d("BatteryScreen", "Receiver manual desregistrado")
        }
    }

    // ---- Animación del progreso del círculo, sigue al porcentaje ----
    val progresoAnimado by animateFloatAsState(
        targetValue = porcentaje / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "progresoBateria"
    )

    val verdeCarga = Color(0xFF2E7D32)

    // ---- UI: fondo blanco, contenido centrado, círculo de carga verde ----
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Anillo de fondo (pista completa, gris claro)
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(180.dp),
                color = Color(0xFFE0E0E0),
                strokeWidth = 12.dp,
            )
            // Anillo de progreso real (verde), representa la carga
            CircularProgressIndicator(
                progress = { progresoAnimado },
                modifier = Modifier.size(180.dp),
                color = verdeCarga,
                strokeWidth = 12.dp,
            )
            Text(
                text = "$porcentaje%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = verdeCarga
            )
        }

        Text(
            text = if (cargando) "Estado: Cargando ⚡" else "Estado: No cargando",
            fontSize = 16.sp,
            color = if (cargando) verdeCarga else Color.Gray,
            modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)
        )

        Button(
            onClick = {
                val intent = Intent(ACTION_ACTUALIZAR_BATERIA).apply {
                    setPackage(context.packageName)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                pendingIntent.send()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = verdeCarga,
                contentColor = Color.White
            )
        ) {
            Text("Actualizar manualmente")
        }
    }
}