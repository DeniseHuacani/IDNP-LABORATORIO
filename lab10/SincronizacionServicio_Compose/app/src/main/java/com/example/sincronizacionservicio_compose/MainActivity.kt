package com.example.sincronizacionservicio_compose

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.sincronizacionservicio_compose.ui.theme.SincronizacionServicio_ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

const val ACCION_SYNC_LISTA = "com.example.sincronizacionservicio_compose.ACCION_SYNC_LISTA"

// PASO 4: segunda acción, para avisar de un error
const val ACCION_SYNC_ERROR = "com.example.sincronizacionservicio_compose.ACCION_SYNC_ERROR"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SincronizacionServicio_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaSincronizacion()
                }
            }
        }
    }
}

// ---------- Service ----------
class SincronizacionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SYNC", "1. Servicio iniciado")
        scope.launch {
            delay(3000)

            // PASO 4: error simulado 1 de cada 3 veces
            // (para probarlo siempre, cambiar (1..3) por (1..1))
            if ((1..1).random() == 1){
                val error = Intent(ACCION_SYNC_ERROR)
                error.setPackage(packageName)
                error.putExtra("motivo", "No se pudo conectar con el servidor (simulado)")
                sendBroadcast(error)
                Log.d("SYNC", "2. Error simulado: aviso de error enviado")
                stopSelf()
                return@launch
            }

            val dao = AppDatabase.obtener(applicationContext).edificioDao()
            var actualizados = 0
            dao.obtenerTodos().forEach { edificio ->
                if (edificio.categoria == "Académico") {
                    dao.actualizar(edificio.copy(sincronizado = true))
                    actualizados++
                }
            }

            val resultado = Intent(ACCION_SYNC_LISTA)
            resultado.setPackage(packageName)
            resultado.putExtra("edificios_actualizados", actualizados)
            sendBroadcast(resultado)
            Log.d("SYNC", "2. Aviso enviado: $actualizados edificios")

            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        Log.d("SYNC", "4. Servicio destruido (onDestroy)")
        super.onDestroy()
    }
}

// ---------- Room ----------
@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String,
    val sincronizado: Boolean = false
)

@Dao
interface EdificioDao {
    @Insert
    suspend fun insertar(edificio: EdificioEntity)

    @Update
    suspend fun actualizar(edificio: EdificioEntity)

    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodos(): List<EdificioEntity>

    @Query("SELECT * FROM EdificioEntity")
    fun observarTodos(): Flow<List<EdificioEntity>>
}

@Database(entities = [EdificioEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // Una sola instancia, compartida por la pantalla y por el Service
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sincronizacion.db"
                ).build().also { instancia = it }
            }
    }
}

// ---------- Pantalla ----------
@Composable
fun PantallaSincronizacion() {
    val context = LocalContext.current
    var sincronizando by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<String?>(null) }
    var esError by remember { mutableStateOf(false) } // PASO 4

    // PASO 2: edificios guardados en Room, observados desde la pantalla
    val dao = remember { AppDatabase.obtener(context).edificioDao() }
    val edificiosFlow = remember { dao.observarTodos() }
    val edificios by edificiosFlow.collectAsState(initial = emptyList())

    // PASO 2: datos de muestra la primera vez que se abre la app
    LaunchedEffect(Unit) {
        if (dao.obtenerTodos().isEmpty()) {
            listOf(
                "Biblioteca Central" to "Servicios",
                "Comedor Universitario" to "Servicios",
                "Facultad de Ingeniería" to "Académico",
                "Facultad de Ciencias" to "Académico",
                "Facultad de Derecho" to "Académico",
                "Coliseo Universitario" to "Deportes",
                "Auditorio Central" to "Eventos"
            ).forEach { (nombre, categoria) ->
                dao.insertar(EdificioEntity(nombre = nombre, categoria = categoria))
            }
        }
    }

    DisposableEffect(Unit) {
        // Receiver 1: la sincronización terminó bien
        val receiverOk = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Aviso recibido en la pantalla")
                val cantidad = intent?.getIntExtra("edificios_actualizados", 0) ?: 0
                resultado = "Sincronización completa: $cantidad edificios actualizados"
                esError = false
                sincronizando = false
            }
        }

        // PASO 4: Receiver 2: la sincronización falló
        val receiverError = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                Log.d("SYNC", "3. Aviso de error recibido en la pantalla")
                val motivo = intent?.getStringExtra("motivo") ?: "Error desconocido"
                resultado = "Error de sincronización: $motivo"
                esError = true
                sincronizando = false
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiverOk,
            IntentFilter(ACCION_SYNC_LISTA),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            context,
            receiverError,
            IntentFilter(ACCION_SYNC_ERROR),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiverOk)
            context.unregisterReceiver(receiverError)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                resultado = null
                esError = false
                sincronizando = true
                context.startService(Intent(context, SincronizacionService::class.java))
            },
            enabled = !sincronizando
        ) {
            Text("Sincronizar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                Log.d("SYNC", "Detención manual solicitada")
                context.stopService(Intent(context, SincronizacionService::class.java))
                sincronizando = false
                esError = false
                resultado = "Sincronización detenida por el usuario"
            },
            enabled = sincronizando
        ) {
            Text("Detener sincronización")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sincronizando) {
            CircularProgressIndicator()
        } else {
            Text(
                text = resultado ?: "Pulsa «Sincronizar» para comenzar",
                color = if (esError) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PASO 2: lista de edificios con su estado
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(edificios, key = { it.id }) { edificio ->
                FilaEdificio(edificio)
            }
        }
    }
}

// PASO 2: fila de la lista de edificios guardados en Room
@Composable
fun FilaEdificio(edificio: EdificioEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = edificio.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = edificio.categoria,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = if (edificio.sincronizado) "Sincronizado" else "Pendiente",
                fontWeight = FontWeight.SemiBold,
                color = if (edificio.sincronizado) Color(0xFF2E7D32) else Color.Gray
            )
        }
    }
}
