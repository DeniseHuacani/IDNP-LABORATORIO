package com.example.audioguiaforeground_compose

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.audioguiaforeground_compose.ui.theme.AudioGuiaForeground_ComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioGuiaForeground_ComposeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaAudioGuia()
                }
            }
        }
    }
}

// PASO 1: estado compartido entre el Service y la pantalla
object EstadoAudioGuia {
    val edificios = listOf(
        "Biblioteca Central",
        "Comedor Universitario",
        "Facultad de Ingeniería",
        "Facultad de Ciencias",
        "Coliseo Universitario",
        "Auditorio Central"
    )
    var indice by mutableStateOf(0)
    var reproduciendo by mutableStateOf(false)

    val edificioActual: String get() = edificios[indice]

    fun siguiente() {
        indice = (indice + 1) % edificios.size
    }
}

// ---------- Room: último edificio reproducido ----------
@Entity
data class UltimoEdificioEntity(
    @PrimaryKey val id: Int = 1,   // siempre una sola fila
    val indice: Int
)

@Dao
interface UltimoEdificioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(ultimo: UltimoEdificioEntity)

    @Query("SELECT * FROM UltimoEdificioEntity WHERE id = 1")
    suspend fun obtener(): UltimoEdificioEntity?
}

@Database(entities = [UltimoEdificioEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ultimoDao(): UltimoEdificioDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // una sola instancia, compartida por la pantalla y por el Service
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audioguia.db"
                ).build().also { instancia = it }
            }
    }
}

class AudioGuiaService : Service() {

    // PASO 4: el audio real reemplaza al avance simulado (job + coroutine)
    private var reproductor: MediaPlayer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACCION_REPRODUCIR = "com.example.audioguiaforeground_compose.REPRODUCIR"
        const val ACCION_PAUSAR = "com.example.audioguiaforeground_compose.PAUSAR"
        const val ACCION_DETENER = "com.example.audioguiaforeground_compose.DETENER"
        // PASO 1: refresca la notificación cuando cambia el edificio
        const val ACCION_ACTUALIZAR = "com.example.audioguiaforeground_compose.ACTUALIZAR"
        private const val ID_CANAL = "canal_audioguia"
        private const val ID_NOTIFICACION = 1
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                ID_CANAL,
                "Audio-guía",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACCION_REPRODUCIR -> {
                EstadoAudioGuia.reproduciendo = true
                startForeground(ID_NOTIFICACION, construirNotificacion())
                iniciarAudio()
                guardarUltimo()
            }
            ACCION_PAUSAR -> {
                reproductor?.pause() // PASO 4
                EstadoAudioGuia.reproduciendo = false
                actualizarNotificacion()
            }
            ACCION_ACTUALIZAR -> {
                reproductor?.seekTo(0) // PASO 4: la narración del nuevo edificio empieza de cero
                actualizarNotificacion()
                guardarUltimo()
            }
            ACCION_DETENER -> {
                liberarAudio() // PASO 4
                EstadoAudioGuia.reproduciendo = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    // PASO 4: crea el MediaPlayer con el archivo de res/raw y lo inicia
    private fun iniciarAudio() {
        if (reproductor == null) {
            reproductor = MediaPlayer.create(this, R.raw.audioguia)
            reproductor?.setOnCompletionListener {
                // al terminar el audio queda como "en pausa"
                EstadoAudioGuia.reproduciendo = false
                actualizarNotificacion()
            }
        }
        reproductor?.start()
    }

    private fun liberarAudio() {
        reproductor?.release()
        reproductor = null
    }

    private fun guardarUltimo() {
        val indice = EstadoAudioGuia.indice
        scope.launch {
            AppDatabase.obtener(applicationContext).ultimoDao()
                .guardar(UltimoEdificioEntity(indice = indice))
        }
    }

    private fun actualizarNotificacion() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ID_NOTIFICACION, construirNotificacion())
    }

    // PASO 2: PendingIntent que envía una acción al Service al tocar un botón
    private fun accionNotificacion(accion: String, codigo: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            codigo,
            Intent(this, AudioGuiaService::class.java).setAction(accion),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun construirNotificacion(): Notification {
        val reproduciendo = EstadoAudioGuia.reproduciendo
        val builder = NotificationCompat.Builder(this, ID_CANAL)
            .setContentTitle(EstadoAudioGuia.edificioActual)
            .setContentText(if (reproduciendo) "Reproduciendo audio-guía" else "Audio-guía en pausa")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)

        // PASO 2: botones dentro de la notificación
        if (reproduciendo) {
            builder.addAction(
                android.R.drawable.ic_media_pause, "Pausar",
                accionNotificacion(ACCION_PAUSAR, 1)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play, "Reanudar",
                accionNotificacion(ACCION_REPRODUCIR, 2)
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel, "Detener",
            accionNotificacion(ACCION_DETENER, 3)
        )
        return builder.build()
    }

    override fun onDestroy() {
        liberarAudio()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun PantallaAudioGuia() {
    val context = LocalContext.current
    var enSegundoPlano by remember { mutableStateOf(false) }

    // PASO 3: último edificio guardado en Room
    val dao = remember { AppDatabase.obtener(context).ultimoDao() }
    var ultimo by remember { mutableStateOf<Int?>(null) }

    // se vuelve a leer al abrir la app y cada vez que cambia el estado de reproducción
    LaunchedEffect(EstadoAudioGuia.reproduciendo) {
        ultimo = dao.obtener()?.indice
    }

    fun enviar(accion: String) {
        context.startService(
            Intent(context, AudioGuiaService::class.java).setAction(accion)
        )
    }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> enSegundoPlano = true
                Lifecycle.Event.ON_START -> enSegundoPlano = false
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("Edificio: ${EstadoAudioGuia.edificioActual}")
        Text(if (EstadoAudioGuia.reproduciendo) "Reproduciendo audio-guía..." else "Audio-guía detenida")

        if (enSegundoPlano && EstadoAudioGuia.reproduciendo) {
            Text("La app está en segundo plano: revisa la notificación")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = { enviar(AudioGuiaService.ACCION_REPRODUCIR) }) {
                Text("Reproducir")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { enviar(AudioGuiaService.ACCION_PAUSAR) }) {
                Text("Pausar")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = { enviar(AudioGuiaService.ACCION_DETENER) }) {
                Text("Detener")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            EstadoAudioGuia.siguiente()
            if (EstadoAudioGuia.reproduciendo) {
                enviar(AudioGuiaService.ACCION_ACTUALIZAR)
            }
        }) {
            Text("Siguiente edificio")
        }

        // PASO 3: botón para retomar el último edificio reproducido
        val nombreGuardado = ultimo?.let { EstadoAudioGuia.edificios.getOrNull(it) }
        if (nombreGuardado != null && !EstadoAudioGuia.reproduciendo) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                EstadoAudioGuia.indice = ultimo ?: 0
                enviar(AudioGuiaService.ACCION_REPRODUCIR)
            }) {
                Text("Continuar: $nombreGuardado")
            }
        }
    }
}