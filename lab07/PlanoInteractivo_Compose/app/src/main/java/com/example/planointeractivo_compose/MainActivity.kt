package com.example.planointeractivo_compose

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.planointeractivo_compose.ui.theme.PlanoInteractivo_ComposeTheme
import org.json.JSONObject

const val ANCHO_PLANO = 360f
const val ALTO_PLANO = 400f

// ---------- Modelos ----------
data class Marcador(
    val offset: Offset,
    val radio: Float,
    val titulo: String,
    val descripcion: String
)

data class Ambiente(
    val topLeft: Offset,
    val size: Size,
    val color: Color,
    val etiqueta: String
)

data class PlanoData(
    val ambientes: List<Ambiente>,
    val marcadores: List<Marcador>
)

// ---------- Carga desde assets/ambientes.json ----------
fun cargarPlano(context: Context): PlanoData {
    return try {
        val texto = context.assets.open("ambientes.json")
            .bufferedReader()
            .use { it.readText() }
        val raiz = JSONObject(texto)

        val arrayAmbientes = raiz.getJSONArray("ambientes")
        val ambientes = List(arrayAmbientes.length()) { i ->
            val o = arrayAmbientes.getJSONObject(i)
            Ambiente(
                topLeft = Offset(o.getDouble("x").toFloat(), o.getDouble("y").toFloat()),
                size = Size(o.getDouble("ancho").toFloat(), o.getDouble("alto").toFloat()),
                color = Color(android.graphics.Color.parseColor(o.getString("color"))),
                etiqueta = o.getString("etiqueta")
            )
        }

        val arrayMarcadores = raiz.getJSONArray("marcadores")
        val marcadores = List(arrayMarcadores.length()) { i ->
            val o = arrayMarcadores.getJSONObject(i)
            Marcador(
                offset = Offset(o.getDouble("x").toFloat(), o.getDouble("y").toFloat()),
                radio = o.getDouble("radio").toFloat(),
                titulo = o.getString("titulo"),
                descripcion = o.getString("descripcion")
            )
        }

        PlanoData(ambientes, marcadores)
    } catch (e: Exception) {
        PlanoData(emptyList(), emptyList())
    }
}

// ---------- ViewModel ----------
class PlanoViewModel(application: Application) : AndroidViewModel(application) {

    private val plano = cargarPlano(application)

    val ambientes: List<Ambiente> = plano.ambientes
    val marcadores: List<Marcador> = plano.marcadores

    var marcadorSeleccionado by mutableStateOf<Marcador?>(null)
        private set

    fun onToque(toque: Offset, escala: Float) {
        marcadores.forEach { marcador ->
            if ((toque - marcador.offset * escala).getDistance() <= marcador.radio * escala) {
                marcadorSeleccionado = marcador
            }
        }
    }

    fun cerrarDialogo() {
        marcadorSeleccionado = null
    }
}

// ---------- Plano (Canvas) ----------
@Composable
fun PlanoAmbiente(viewModel: PlanoViewModel = viewModel()) {
    val medidorTexto = rememberTextMeasurer()
    val colorMarcador = Color(0xFFE53935)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Canvas(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .aspectRatio(ANCHO_PLANO / ALTO_PLANO)
                .pointerInput(Unit) {
                    detectTapGestures { toque ->
                        viewModel.onToque(toque, size.width / ANCHO_PLANO)
                    }
                }
        ) {
            val escala = size.width / ANCHO_PLANO

            // Ambientes con su etiqueta
            viewModel.ambientes.forEach { ambiente ->
                drawRect(
                    color = ambiente.color,
                    topLeft = ambiente.topLeft * escala,
                    size = ambiente.size * escala
                )
                drawText(
                    textMeasurer = medidorTexto,
                    text = ambiente.etiqueta,
                    topLeft = ambiente.topLeft * escala + Offset(10f, 8f),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238)
                    )
                )
            }

            // Contorno del edificio
            drawRect(
                color = Color(0xFF455A64),
                topLeft = Offset(10f, 10f) * escala,
                size = Size(340f, 380f) * escala,
                style = Stroke(width = 8f)
            )

            // Paredes internas
            val pared = Color(0xFF455A64)
            drawLine(pared, Offset(180f, 10f) * escala, Offset(180f, 280f) * escala, strokeWidth = 5f)
            drawLine(pared, Offset(10f, 160f) * escala, Offset(350f, 160f) * escala, strokeWidth = 5f)
            drawLine(pared, Offset(10f, 280f) * escala, Offset(350f, 280f) * escala, strokeWidth = 5f)

            // Marcadores numerados con sombra y borde blanco
            viewModel.marcadores.forEachIndexed { indice, marcador ->
                val centro = marcador.offset * escala
                val radio = marcador.radio * escala

                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = radio + 4f,
                    center = centro + Offset(0f, 5f)
                )
                drawCircle(color = Color.White, radius = radio + 5f, center = centro)
                drawCircle(color = colorMarcador, radius = radio, center = centro)

                val numero = medidorTexto.measure(
                    text = (indice + 1).toString(),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                drawText(
                    textLayoutResult = numero,
                    topLeft = centro - Offset(
                        numero.size.width / 2f,
                        numero.size.height / 2f
                    )
                )
            }
        }
    }

    viewModel.marcadorSeleccionado?.let { marcador ->
        AlertDialog(
            onDismissRequest = { viewModel.cerrarDialogo() },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = colorMarcador
                )
            },
            title = { Text(marcador.titulo, fontWeight = FontWeight.Bold) },
            text = { Text(marcador.descripcion) },
            confirmButton = {
                TextButton(onClick = { viewModel.cerrarDialogo() }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// ---------- Pantallas ----------
sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Mapa : Screen("mapa", "Mapa")
    object Plano : Screen("plano", "Plano")
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Bienvenido a Campus Navigator",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ve a la pestaña Mapa para explorar el plano interior",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MapaScreen(onVerPlano: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Mapa de ubicaciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onVerPlano,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Ver plano interior del pabellón")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanoScreen(onVolver: () -> Unit) {
    val viewModel: PlanoViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Plano interior del pabellón",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Toca un círculo rojo para ver la información del ambiente",
                style = MaterialTheme.typography.bodyMedium
            )

            PlanoAmbiente(viewModel)

            // Leyenda numerada (coincide con los números del plano)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Leyenda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    viewModel.marcadores.forEachIndexed { indice, marcador ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (indice + 1).toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = marcador.titulo,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = marcador.descripcion,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Navegacion ----------
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Mapa)
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    val icono = if (screen == Screen.Home) Icons.Default.Home else Icons.Default.Place
                    NavigationBarItem(
                        selected = rutaActual == screen.route ||
                                (screen == Screen.Mapa && rutaActual == Screen.Plano.route),
                        onClick = { navController.navigate(screen.route) },
                        icon = { Icon(icono, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Mapa.route) {
                MapaScreen(onVerPlano = { navController.navigate(Screen.Plano.route) })
            }
            composable(Screen.Plano.route) {
                PlanoScreen(onVolver = { navController.popBackStack() })
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanoInteractivo_ComposeTheme {
                MainScreen()
            }
        }
    }
}