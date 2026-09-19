package com.example.navcompose_loginregistro

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.io.File

const val ARCHIVO_CUENTAS = "cuentas.txt"

fun guardarCuenta(context: Context, usuario: String, password: String) {
    try {
        context.openFileOutput(ARCHIVO_CUENTAS, Context.MODE_APPEND).use { salida ->
            salida.write("$usuario,$password\n".toByteArray())
        }
        Log.e("GUARDADO_TXT", ">>> SE GUARDÓ CORRECTAMENTE: $usuario,$password <<<")
    } catch (e: Exception) {
        Log.e("GUARDADO_TXT", "Error al guardar el archivo: ${e.message}")
    }
}

fun existeCuenta(context: Context, usuario: String, password: String): Boolean {
    val archivo = File(context.filesDir, ARCHIVO_CUENTAS)
    if (!archivo.exists()) return false

    return archivo.readLines().any { linea ->
        val partes = linea.split(",", limit = 2)
        partes.size == 2 && partes[0] == usuario && partes[1] == password
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {

                composable("login") {
                    LoginScreen(
                        onLoginExitoso = { usuario ->
                            navController.navigate("home/${Uri.encode(usuario)}") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onIrARegistro = {
                            navController.navigate("registro")
                        }
                    )
                }

                composable("registro") {
                    RegistroScreen(
                        onRegistroExitoso = {
                            navController.popBackStack()
                        },
                        onCancelar = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = "home/{usuario}",
                    arguments = listOf(
                        navArgument("usuario") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
                    HomeScreen(usuario)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginExitoso: (String) -> Unit,
    onIrARegistro: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (mensajeError.isNotEmpty()) {
            Text(mensajeError, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val nombre = usuario.trim()
                when {
                    nombre.isEmpty() || password.isEmpty() -> {
                        mensajeError = "Complete todos los campos"
                    }
                    existeCuenta(context, nombre, password) -> {
                        mensajeError = ""
                        onLoginExitoso(nombre)
                    }
                    else -> {
                        mensajeError = "Cuenta no encontrada"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar")
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onIrARegistro,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }
    }
}

@Composable
fun RegistroScreen(
    onRegistroExitoso: () -> Unit,
    onCancelar: () -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Nuevo usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (mensajeError.isNotEmpty()) {
            Text(mensajeError, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val nombre = usuario.trim()
                when {
                    nombre.isEmpty() || password.isEmpty() -> {
                        mensajeError = "Complete todos los campos"
                    }
                    nombre.contains(",") -> {
                        mensajeError = "El usuario no puede contener comas"
                    }
                    else -> {
                        guardarCuenta(context, nombre, password)
                        Toast.makeText(
                            context,
                            "Cuenta \"$nombre\" registrada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        onRegistroExitoso()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aceptar")
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancelar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
fun HomeScreen(usuario: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido $usuario", style = MaterialTheme.typography.headlineMedium)
    }
}