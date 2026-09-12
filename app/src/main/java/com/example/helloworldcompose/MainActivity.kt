package com.example.helloworldcompose

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.helloworldcompose.ui.theme.HelloWorldComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloWorldComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegistroLibroScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroLibroScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Estados de los inputs
    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var paginas by remember { mutableStateOf("") }

    // Creacion de estado para guardar la lectura y mostrarla en pantalla
    var contenidoGuardado by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta superior
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "Libro",
                    modifier = Modifier.size(48.dp)
                )
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del Libro") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = autor,
                    onValueChange = { autor = it },
                    label = { Text("Autor") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Campo para Páginas leídas
        OutlinedTextField(
            value = paginas,
            onValueChange = { paginas = it },
            label = { Text("Páginas leídas") },
            modifier = Modifier.fillMaxWidth()
        )

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // GUARDAR
            Button(onClick = {
                if (titulo.isNotBlank() && autor.isNotBlank() && paginas.isNotBlank()) {
                    val datosAGuardar = "Título: $titulo\nAutor: $autor\nPáginas: $paginas"
                    try {
                        context.openFileOutput("registro_libro.txt", Context.MODE_PRIVATE).use { output ->
                            output.write(datosAGuardar.toByteArray())
                        }
                        Toast.makeText(context, "¡Datos guardados con éxito!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("GUARDAR")
            }

            // VER REGISTRO
            OutlinedButton(onClick = {
                try {
                    // Leer el archivo desde el almacenamiento interno
                    val contenido = context.openFileInput("registro_libro.txt").bufferedReader().use { it.readText() }

                    // PUNTO 3: Imprimir en consola Logcat
                    Log.d("RegistroLibro", "Contenido recuperado del archivo:\n$contenido")

                    // PUNTO 4: Asignar al estado para renderizar en la pantalla
                    contenidoGuardado = contenido

                    Toast.makeText(context, "Registro recuperado con éxito", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("RegistroLibro", "Error al leer el archivo guardado", e)
                    Toast.makeText(context, "No se encontró ningún archivo registrado", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("VER REGISTRO")
            }
        }

        // para el reto opcional
        if (contenidoGuardado.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reto Opcional)",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "REGISTRO GUARDADO",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = contenidoGuardado)
                }
            }
        }
    }
}
