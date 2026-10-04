package com.example.campusnavigator_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campusnavigator_compose.ui.theme.CampusNavigator_ComposeTheme

// ---------- Paleta del campus ----------
private val AzulCampus = Color(0xFF0D47A1)
private val AzulClaro = Color(0xFF1976D2)

private val CampusColors = lightColorScheme(
    primary = AzulCampus,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00796B),
    secondaryContainer = Color(0xFFCCEFE8),
    background = Color(0xFFF3F6FB),
    surface = Color.White
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            CampusNavigator_ComposeTheme {
                MaterialTheme(colorScheme = CampusColors) {
                    MainScreen()
                }
            }
        }
    }
}

// ---------- ViewModel compartido ----------
class SeleccionViewModel : ViewModel() {
    var edificioSeleccionado by mutableStateOf<String>("Ninguno")
        private set

    fun seleccionarEdificio(nombre: String) {
        edificioSeleccionado = nombre
    }
}

// ---------- Navegación ----------
sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Edificios : Screen("edificios", "Edificios")
    object Mapa : Screen("mapa", "Mapa")
    object Perfil : Screen("perfil", "Perfil")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val seleccionViewModel: SeleccionViewModel = viewModel()

    val items = listOf(Screen.Home, Screen.Edificios, Screen.Mapa, Screen.Perfil)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Campus Navigator", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val icon = when (screen) {
                        Screen.Home -> Icons.Default.Home
                        Screen.Edificios -> Icons.Default.List
                        Screen.Mapa -> Icons.Default.Place
                        Screen.Perfil -> Icons.Default.Person
                    }

                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { navController.navigate(screen.route) },
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
            composable(Screen.Home.route) { HomeScreen(viewModel = seleccionViewModel) }
            composable(Screen.Edificios.route) { EdificiosScreen(viewModel = seleccionViewModel) }
            composable(Screen.Mapa.route) { MapaScreen() }
            composable(Screen.Perfil.route) { PerfilScreen() }
        }
    }
}

// ---------- HOME ----------
@Composable
fun HomeScreen(viewModel: SeleccionViewModel) {
    val edificio = viewModel.edificioSeleccionado

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera con degradado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AzulCampus, AzulClaro)))
                .padding(24.dp)
        ) {
            Column {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Bienvenido a la app de Edificios",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Encuentra tu camino por el campus",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Tarjeta del último edificio consultado
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = AzulCampus)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Último edificio consultado",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = edificio,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AzulCampus
                    )
                }
            }
        }

        // Tarjetas informativas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard("4", "Edificios", Modifier.weight(1f))
            InfoCard("1", "Mapa", Modifier.weight(1f))
        }
    }
}

@Composable
fun InfoCard(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(text = etiqueta, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------- EDIFICIOS ----------
data class Edificio(val nombre: String, val descripcion: String)

@Composable
fun EdificiosScreen(viewModel: SeleccionViewModel) {
    val edificios = listOf(
        Edificio("Biblioteca Central", "Libros, salas de estudio y cubículos"),
        Edificio("Pabellón A", "Aulas de teoría y laboratorios"),
        Edificio("Pabellón B", "Oficinas administrativas y aulas"),
        Edificio("Auditorio", "Conferencias, eventos y ceremonias")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Edificios del campus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AzulCampus
            )
        }
        items(edificios) { edificio ->
            val seleccionado = viewModel.edificioSeleccionado == edificio.nombre

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (seleccionado) MaterialTheme.colorScheme.primaryContainer
                    else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AzulCampus),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = edificio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = edificio.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Button(onClick = { viewModel.seleccionarEdificio(edificio.nombre) }) {
                        Text("Ver")
                    }
                }
            }
        }
    }
}

// ---------- MAPA ----------
@Composable
fun MapaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mapa del campus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AzulCampus
        )
        Text(
            text = "Mapa de ubicaciones (pendiente de integrar)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDCEBD3))
            ) {
                // Caminos del campus
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawLine(Color.White, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 28f)
                    drawLine(Color.White, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 28f)
                    drawLine(Color(0xFFF5F0DC), Offset(0f, h), Offset(w, 0f), strokeWidth = 14f)
                }
                PinMapa("Biblioteca Central", Modifier.align(BiasAlignment(-0.55f, -0.6f)))
                PinMapa("Pabellón A", Modifier.align(BiasAlignment(0.55f, -0.5f)))
                PinMapa("Pabellón B", Modifier.align(BiasAlignment(-0.55f, 0.55f)))
                PinMapa("Auditorio", Modifier.align(BiasAlignment(0.6f, 0.6f)))
            }
        }
    }
}

@Composable
fun PinMapa(nombre: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Place,
            contentDescription = nombre,
            tint = Color(0xFFD32F2F),
            modifier = Modifier.size(38.dp)
        )
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ---------- PERFIL ----------
@Composable
fun PerfilScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(AzulCampus, AzulClaro))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Estudiante",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Perfil de usuario (pendiente de implementar)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FilaPerfil(Icons.Default.Email, "correo@ejemplo.com")
                FilaPerfil(Icons.Default.Phone, "+51 000 000 000")
                FilaPerfil(Icons.Default.Settings, "Configuración")
            }
        }
    }
}

@Composable
fun FilaPerfil(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = AzulCampus)
        Spacer(Modifier.width(16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}