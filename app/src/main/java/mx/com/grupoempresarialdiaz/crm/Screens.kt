package mx.com.grupoempresarialdiaz.crm

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES_MX: Locale = Locale.forLanguageTag("es-MX")
private val FORMATO_DIA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_MX)

private fun colorSucursal(s: String) = when (s) {
    "Del Valle" -> Color(0xFF0E7C66)
    "Tepeyac" -> Color(0xFF3B6FB6)
    "Balbuena" -> Color(0xFFB4559B)
    "Pachuca" -> Color(0xFF7A5AC8)
    "Puebla" -> Color(0xFFD9822B)
    else -> Suave
}

private fun pesos(v: Double) = "$" + String.format(ES_MX, "%,.2f", v)

// ======================================================================
//  Login
// ======================================================================
@Composable
fun LoginScreen(vm: CrmViewModel) {
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.aviso) { vm.aviso?.let { snack.showSnackbar(it); vm.aviso = null } }

    Scaffold(snackbarHost = { SnackbarHost(snack) }, containerColor = Niebla) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(64.dp).background(Jade, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("CR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp) }
            Spacer(Modifier.height(24.dp))
            Text("Clínica de Rodillas", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Entra con tu usuario del CRM", color = Suave)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = usuario, onValueChange = { usuario = it; error = null },
                label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = null },
                label = { Text("Contraseña") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = Rosa, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { vm.entrar(usuario, password) { error = it } },
                enabled = usuario.isNotBlank() && password.isNotBlank() && !vm.cargando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(if (vm.cargando) "Entrando…" else "Entrar", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// ======================================================================
//  Agenda del día
// ======================================================================
@Composable
fun AgendaScreen(vm: CrmViewModel) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.aviso) { vm.aviso?.let { snack.showSnackbar(it); vm.aviso = null } }
    var cobrando by remember { mutableStateOf<Cita?>(null) }
    var nuevaCita by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Niebla,
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (vm.prefs.puedeOperar) {
                ExtendedFloatingActionButton(
                    onClick = { nuevaCita = true },
                    containerColor = Jade, contentColor = Color.White,
                ) { Text("+ Nueva cita", fontWeight = FontWeight.Bold) }
            }
        },
        topBar = { Encabezado(vm) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.bloqueos) { b ->
                Text(
                    "🚫 " + b.texto + if (vm.sucursal.isEmpty()) " — ${b.sucursal}" else "",
                    color = Rosa, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(RosaFondo, RoundedCornerShape(12.dp)).padding(12.dp),
                )
            }
            if (vm.citas.isEmpty() && !vm.cargando) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay citas este día", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Agenda una con “Nueva cita”.", color = Suave)
                    }
                }
            }
            items(vm.citas, key = { it.id }) { c ->
                TarjetaCita(c, mostrarSucursal = vm.sucursal.isEmpty(), puedeOperar = vm.prefs.puedeOperar,
                    alMarcar = { vm.marcar(c, it) }, alCobrar = { cobrando = c })
            }
        }
    }

    cobrando?.let { c -> DialogoCobro(vm, c) { cobrando = null } }
    if (nuevaCita) DialogoNuevaCita(vm) { nuevaCita = false }
}

@Composable
private fun Encabezado(vm: CrmViewModel) {
    Surface(color = Niebla) {
        Column(Modifier.systemBarsPadding().padding(top = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (vm.sucursal.isEmpty()) "Todas las sucursales" else "Agenda de ${vm.sucursal}", color = Jade, fontWeight = FontWeight.SemiBold)
                    val dia = vm.fecha.format(FORMATO_DIA).replaceFirstChar { it.titlecase(ES_MX) }
                    Text(dia, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { vm.salir() }) { Text("Salir", color = Suave) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { vm.moverDia(-1) }) { Text("‹ Anterior", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { vm.hoy() }) { Text("Hoy", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { vm.moverDia(1) }) { Text("Siguiente ›", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.cargar() }) { Text("Actualizar") }
            }
            if (vm.opcionesSucursal.size > 1) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.opcionesSucursal) { s ->
                        FilterChip(selected = vm.sucursal == s, onClick = { vm.elegirSucursal(s) }, label = { Text(s.ifEmpty { "Todas" }) })
                    }
                }
            }
            val asistieron = vm.citas.count { it.asistencia == "Asistió" }
            val faltaron = vm.citas.count { it.asistencia == "No Asistió" }
            Text(
                "${vm.horario}  ·  ${vm.citas.size} citas, $asistieron asistieron, $faltaron faltaron",
                color = Suave, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (vm.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Jade) else Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Pastilla(texto: String, color: Color, fondo: Color) {
    Text(texto, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier.background(fondo, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
}

@Composable
private fun TarjetaCita(c: Cita, mostrarSucursal: Boolean, puedeOperar: Boolean, alMarcar: (String) -> Unit, alCobrar: () -> Unit) {
    val ctx = LocalContext.current
    val pendiente = c.asistencia != "Asistió" && c.asistencia != "No Asistió"
    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Linea, RoundedCornerShape(16.dp)).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.hora, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(70.dp))
            Box(Modifier.width(4.dp).height(40.dp).background(colorSucursal(c.sucursal), RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.paciente, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val extra = buildString {
                    append(c.tratamiento)
                    if (mostrarSucursal) append(", ").append(c.sucursal)
                    if (!c.yaEsPaciente) append(" · Primera vez")
                    if (c.estado == "Express") append(" · ⚡ sin cita")
                }
                Text(extra, color = Suave, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                c.asistencia == "Asistió" -> Pastilla("Asistió", VerdeOk, VerdeOkFondo)
                c.asistencia == "No Asistió" -> Pastilla("Faltó", Rosa, RosaFondo)
                c.estado == "Confirmada" -> Pastilla("Confirmada", JadeOscuro, JadeClaro)
                else -> Pastilla("Por llegar", Ambar, AmbarFondo)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (c.telefono.isNotBlank()) {
                TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.telefono))) }) { Text("Llamar") }
            }
            Spacer(Modifier.weight(1f))
            if (puedeOperar && pendiente) {
                OutlinedButton(onClick = { alMarcar("Asistió") }) { Text("✓ Llegó", color = VerdeOk, fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { alMarcar("No Asistió") }) { Text("Faltó", color = Rosa, fontWeight = FontWeight.Bold) }
            }
            if (puedeOperar && c.asistencia != "No Asistió") {
                Button(onClick = alCobrar, colors = ButtonDefaults.buttonColors(containerColor = Jade)) { Text("Cobrar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ======================================================================
//  Cobro
// ======================================================================
@Composable
private fun DialogoCobro(vm: CrmViewModel, c: Cita, cerrar: () -> Unit) {
    val ctx = LocalContext.current
    var servicio by remember { mutableStateOf("") }
    var efectivo by remember { mutableStateOf("") }
    var credito by remember { mutableStateOf("") }
    var debito by remember { mutableStateOf("") }
    var transf by remember { mutableStateOf("") }
    var folio by remember { mutableStateOf<String?>(null) }
    var duplicada by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val num = { s: String -> s.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0 }
    val total = num(efectivo) + num(credito) + num(debito) + num(transf)

    fun registrar(forzar: Boolean) {
        enviando = true
        val montos = mapOf("efectivo" to num(efectivo).toString(), "credito" to num(credito).toString(),
            "debito" to num(debito).toString(), "transferencia" to num(transf).toString())
        vm.cobrar(c, servicio.trim(), montos, forzar) { f, dup -> enviando = false; folio = f; duplicada = dup }
    }

    when {
        folio != null -> AlertDialog(
            onDismissRequest = cerrar,
            title = { Text("Cobro registrado") },
            text = { Text("Folio ${folio}. La asistencia quedó marcada.\n\n¿Mandar el comprobante al paciente por WhatsApp?") },
            confirmButton = {
                Button(onClick = {
                    val tel = c.telefono.filter { it.isDigit() }.let { if (it.length == 10) "52$it" else it }
                    val texto = "Hola ${c.paciente}, recibimos tu pago de ${pesos(total)} en Clínica de Rodillas. Folio: ${folio}. ¡Gracias!"
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$tel?text=" + Uri.encode(texto))))
                    cerrar()
                }) { Text("WhatsApp") }
            },
            dismissButton = { TextButton(onClick = cerrar) { Text("Listo") } },
        )
        duplicada != null -> AlertDialog(
            onDismissRequest = { duplicada = null },
            title = { Text("Ya tiene un cobro") },
            text = { Text("${duplicada}\n¿Registrar este cobro adicional?") },
            confirmButton = { Button(onClick = { duplicada = null; registrar(forzar = true) }) { Text("Sí, registrar") } },
            dismissButton = { TextButton(onClick = { duplicada = null }) { Text("Cancelar") } },
        )
        else -> AlertDialog(
            onDismissRequest = { if (!enviando) cerrar() },
            title = { Text("Cobrar a ${c.paciente}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(servicio, { servicio = it }, label = { Text("Servicio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    CampoMonto("Efectivo", efectivo) { efectivo = it }
                    CampoMonto("Tarjeta de crédito", credito) { credito = it }
                    CampoMonto("Tarjeta de débito", debito) { debito = it }
                    CampoMonto("Transferencia", transf) { transf = it }
                    Text("Total: ${pesos(total)}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            confirmButton = {
                Button(onClick = { registrar(forzar = false) }, enabled = servicio.trim().length >= 3 && total > 0 && !enviando) {
                    Text(if (enviando) "Guardando…" else "Registrar cobro")
                }
            },
            dismissButton = { TextButton(onClick = cerrar, enabled = !enviando) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CampoMonto(etiqueta: String, valor: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor, onValueChange = { v -> alCambiar(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(etiqueta) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

// ======================================================================
//  Nueva cita
// ======================================================================
@Composable
private fun DialogoNuevaCita(vm: CrmViewModel, cerrar: () -> Unit) {
    var fecha by remember { mutableStateOf(vm.fecha.toString()) }
    var hora by remember { mutableStateOf("10:00") }
    var paciente by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(vm.prefs.servicios.firstOrNull() ?: "") }
    var suc by remember { mutableStateOf(vm.sucursal.ifEmpty { vm.prefs.sucursales.firstOrNull() ?: "" }) }
    var confirmada by remember { mutableStateOf(false) }
    var yaEs by remember { mutableStateOf(false) }
    var advertencia by remember { mutableStateOf<String?>(null) }

    fun datos() = mapOf(
        "fecha" to fecha.trim(), "hora" to hora.trim(), "paciente" to paciente.trim(), "telefono" to telefono.trim(),
        "edad" to edad.trim(), "tipo" to tipo, "sucursal" to suc,
        "estado" to if (confirmada) "Confirmada" else "Agendado", "ya_es_paciente" to if (yaEs) "Si" else "No",
    )

    advertencia?.let { msg ->
        AlertDialog(
            onDismissRequest = { advertencia = null },
            title = { Text("Revisa el horario") },
            text = { Text("$msg\n\n¿Agendar de todos modos?") },
            confirmButton = { Button(onClick = { advertencia = null; vm.guardarCita(datos(), true, { advertencia = it }, cerrar) }) { Text("Sí, agendar") } },
            dismissButton = { TextButton(onClick = { advertencia = null }) { Text("Cambiar hora") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Nueva cita") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(fecha, { fecha = it }, label = { Text("Fecha (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(hora, { hora = it }, label = { Text("Hora") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(paciente, { paciente = it }, label = { Text("Nombre del paciente") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(telefono, { v -> telefono = v.filter { it.isDigit() }.take(10) }, label = { Text("Teléfono") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1.6f))
                    OutlinedTextField(edad, { v -> edad = v.filter { it.isDigit() }.take(3) }, label = { Text("Edad") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Text("Tratamiento", fontWeight = FontWeight.SemiBold)
                vm.prefs.servicios.forEach { s -> Opcion(s, tipo == s) { tipo = s } }
                if (vm.prefs.sucursalFija == null && vm.prefs.sucursales.size > 1) {
                    Text("Sucursal", fontWeight = FontWeight.SemiBold)
                    vm.prefs.sucursales.forEach { s -> Opcion(s, suc == s) { suc = s } }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { confirmada = !confirmada }) {
                    Checkbox(confirmada, { confirmada = it }); Text("Ya confirmó")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { yaEs = !yaEs }) {
                    Checkbox(yaEs, { yaEs = it }); Text("Ya es paciente")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { vm.guardarCita(datos(), false, { advertencia = it }, cerrar) },
                enabled = paciente.trim().length >= 3 && telefono.length == 10 && !vm.cargando,
            ) { Text("Guardar cita") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun Opcion(texto: String, elegido: Boolean, alElegir: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = alElegir)) {
        RadioButton(selected = elegido, onClick = alElegir)
        Text(texto)
    }
}
