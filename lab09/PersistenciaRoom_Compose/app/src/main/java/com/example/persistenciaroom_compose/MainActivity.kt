package com.example.persistenciaroom_compose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.persistenciaroom_compose.ui.theme.PersistenciaRoom_ComposeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = crearBaseDeDatos(applicationContext)
        val dao = database.edificioDao()
        val factory = EdificiosViewModelFactory(dao)

        setContent {
            PersistenciaRoom_ComposeTheme {
                val viewModel: EdificiosViewModel = viewModel(factory = factory)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = FondoVerde
                ) {
                    ListaEdificiosConAmbientes(viewModel)
                }
            }
        }
    }
}

// ---------- Entidades ----------
@Entity
data class EdificioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val categoria: String
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = EdificioEntity::class,
            parentColumns = ["id"],
            childColumns = ["edificioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("edificioId")]
)
data class AmbienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val edificioId: Int,
    val nombre: String
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AmbienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["ambienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ambienteId")]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ambienteId: Int,
    val texto: String
)

// ---------- Relaciones ----------
data class EdificioConAmbientes(
    @Embedded val edificio: EdificioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "edificioId"
    )
    val ambientes: List<AmbienteEntity>
)

data class AmbienteConNotas(
    @Embedded val ambiente: AmbienteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ambienteId"
    )
    val notas: List<NotaEntity>
)

// ---------- DAO ----------
@Dao
interface EdificioDao {
    @Insert
    suspend fun insertarEdificio(edificio: EdificioEntity): Long

    @Insert
    suspend fun insertarAmbiente(ambiente: AmbienteEntity): Long

    @Insert
    suspend fun insertarNota(nota: NotaEntity)

    @Update
    suspend fun actualizarEdificio(edificio: EdificioEntity)

    @Update
    suspend fun actualizarAmbiente(ambiente: AmbienteEntity)

    @Update
    suspend fun actualizarNota(nota: NotaEntity)

    @Delete
    suspend fun eliminarEdificio(edificio: EdificioEntity)

    @Delete
    suspend fun eliminarAmbiente(ambiente: AmbienteEntity)

    @Delete
    suspend fun eliminarNota(nota: NotaEntity)

    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerTodos(): List<EdificioEntity>

    @Transaction
    @Query("SELECT * FROM EdificioEntity")
    suspend fun obtenerConAmbientes(): List<EdificioConAmbientes>

    @Transaction
    @Query("SELECT * FROM AmbienteEntity")
    suspend fun obtenerAmbientesConNotas(): List<AmbienteConNotas>
}

// ---------- Base de datos ----------
@Database(
    entities = [EdificioEntity::class, AmbienteEntity::class, NotaEntity::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun edificioDao(): EdificioDao
}

fun crearBaseDeDatos(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app.db"
    )
        .fallbackToDestructiveMigration()
        .build()
}

// ---------- ViewModel ----------
class EdificiosViewModel(private val dao: EdificioDao) : ViewModel() {

    private val _edificios = MutableStateFlow<List<EdificioConAmbientes>>(emptyList())
    val edificios: StateFlow<List<EdificioConAmbientes>> = _edificios

    private val _ambientes = MutableStateFlow<List<AmbienteConNotas>>(emptyList())
    val ambientes: StateFlow<List<AmbienteConNotas>> = _ambientes

    init {
        viewModelScope.launch {
            if (dao.obtenerTodos().isEmpty()) {
                sembrarDatos()
            }
            cargar()
        }
    }

    private suspend fun cargar() {
        _edificios.value = dao.obtenerConAmbientes()
        _ambientes.value = dao.obtenerAmbientesConNotas()
    }

    // Cada operación modifica la base y vuelve a cargar las consultas
    fun renombrarEdificio(edificio: EdificioEntity, nuevoNombre: String) {
        viewModelScope.launch {
            dao.actualizarEdificio(edificio.copy(nombre = nuevoNombre))
            cargar()
        }
    }

    fun eliminarEdificio(edificio: EdificioEntity) {
        viewModelScope.launch {
            dao.eliminarEdificio(edificio)
            cargar()
        }
    }

    fun renombrarAmbiente(ambiente: AmbienteEntity, nuevoNombre: String) {
        viewModelScope.launch {
            dao.actualizarAmbiente(ambiente.copy(nombre = nuevoNombre))
            cargar()
        }
    }

    fun eliminarAmbiente(ambiente: AmbienteEntity) {
        viewModelScope.launch {
            dao.eliminarAmbiente(ambiente)
            cargar()
        }
    }

    private suspend fun sembrarDatos() {
        val id1 = dao.insertarEdificio(
            EdificioEntity(nombre = "Biblioteca Central", categoria = "Servicios")
        ).toInt()
        val sala = dao.insertarAmbiente(
            AmbienteEntity(edificioId = id1, nombre = "Sala de lectura")
        ).toInt()
        val hemeroteca = dao.insertarAmbiente(
            AmbienteEntity(edificioId = id1, nombre = "Hemeroteca")
        ).toInt()

        val id2 = dao.insertarEdificio(
            EdificioEntity(nombre = "Facultad de Ingeniería", categoria = "Académico")
        ).toInt()
        val laboratorio = dao.insertarAmbiente(
            AmbienteEntity(edificioId = id2, nombre = "Laboratorio de cómputo")
        ).toInt()

        dao.insertarNota(NotaEntity(ambienteId = sala, texto = "Se reserva los martes"))
        dao.insertarNota(NotaEntity(ambienteId = sala, texto = "Faltan sillas"))
        dao.insertarNota(NotaEntity(ambienteId = hemeroteca, texto = "Abierta solo en la mañana"))
        dao.insertarNota(NotaEntity(ambienteId = laboratorio, texto = "El proyector no funciona"))
    }
}

class EdificiosViewModelFactory(private val dao: EdificioDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EdificiosViewModel(dao) as T
    }
}

// ---------- Colores ----------
val VerdeOscuro = Color(0xFF1B5E20)
val Verde = Color(0xFF2E7D32)
val VerdeClaro = Color(0xFFE8F5E9)
val FondoVerde = Color(0xFFFFFFFF)
val Rojo = Color(0xFFC62828)

// ---------- Interfaz ----------
// Cuadro de diálogo para escribir el nuevo nombre
@Composable
fun DialogoEditar(
    titulo: String,
    valorInicial: String,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = { Text(titulo, color = VerdeOscuro, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Verde,
                    focusedLabelColor = Verde,
                    cursorColor = Verde
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (texto.isNotBlank()) onGuardar(texto.trim()) },
                colors = ButtonDefaults.textButtonColors(contentColor = Verde)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                colors = ButtonDefaults.textButtonColors(contentColor = Rojo)
            ) { Text("Cancelar") }
        }
    )
}

// Botones de icono: editar (verde) y eliminar (rojo)
@Composable
fun BotonesEditarEliminar(onEditar: () -> Unit, onEliminar: () -> Unit) {
    IconButton(onClick = onEditar) {
        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Verde)
    }
    IconButton(onClick = onEliminar) {
        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Rojo)
    }
}

// Fila de un ambiente con sus notas en un desplegable
@Composable
fun FilaAmbiente(
    ambiente: AmbienteEntity,
    notas: List<NotaEntity>,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    // Cambiar a true si se quiere que las notas estén siempre visibles
    var expandido by remember(ambiente.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(VerdeClaro, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (expandido) "Ocultar notas" else "Mostrar notas",
                tint = Verde,
                modifier = Modifier.rotate(if (expandido) 0f else -90f)
            )
            Text(
                text = "${ambiente.nombre} (${notas.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = VerdeOscuro,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            BotonesEditarEliminar(onEditar = onEditar, onEliminar = onEliminar)
        }

        if (expandido) {
            Column(
                modifier = Modifier.padding(start = 40.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (notas.isEmpty()) {
                    Text("Sin notas", fontSize = 13.sp, color = Color.Gray)
                }
                notas.forEach { nota ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Verde,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = nota.texto,
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListaEdificiosConAmbientes(viewModel: EdificiosViewModel) {
    val edificios by viewModel.edificios.collectAsState()
    val ambientes by viewModel.ambientes.collectAsState()

    // Las notas de cada ambiente salen de la segunda consulta (ambiente -> notas)
    val notasPorAmbiente = ambientes.associate { it.ambiente.id to it.notas }

    // Qué elemento se está editando (null = ninguno)
    var edificioEditando by remember { mutableStateOf<EdificioEntity?>(null) }
    var ambienteEditando by remember { mutableStateOf<AmbienteEntity?>(null) }

    edificioEditando?.let { edificio ->
        DialogoEditar(
            titulo = "Editar edificio",
            valorInicial = edificio.nombre,
            onGuardar = { nuevo ->
                viewModel.renombrarEdificio(edificio, nuevo)
                edificioEditando = null
            },
            onCancelar = { edificioEditando = null }
        )
    }

    ambienteEditando?.let { ambiente ->
        DialogoEditar(
            titulo = "Editar ambiente",
            valorInicial = ambiente.nombre,
            onGuardar = { nuevo ->
                viewModel.renombrarAmbiente(ambiente, nuevo)
                ambienteEditando = null
            },
            onCancelar = { ambienteEditando = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Text(
                text = "Edificios y ambientes",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = VerdeOscuro,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        items(edificios, key = { it.edificio.id }) { conAmbientes ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Fila del edificio con sus botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Verde,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = conAmbientes.edificio.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = VerdeOscuro
                            )
                            Text(
                                text = conAmbientes.edificio.categoria,
                                fontSize = 13.sp,
                                color = Verde
                            )
                        }
                        BotonesEditarEliminar(
                            onEditar = { edificioEditando = conAmbientes.edificio },
                            onEliminar = { viewModel.eliminarEdificio(conAmbientes.edificio) }
                        )
                    }

                    // Ambientes del edificio, cada uno con sus notas desplegables
                    conAmbientes.ambientes.forEach { ambiente ->
                        FilaAmbiente(
                            ambiente = ambiente,
                            notas = notasPorAmbiente[ambiente.id] ?: emptyList(),
                            onEditar = { ambienteEditando = ambiente },
                            onEliminar = { viewModel.eliminarAmbiente(ambiente) }
                        )
                    }
                }
            }
        }

        item { Text(text = "", modifier = Modifier.padding(8.dp)) }
    }
}