package mx.com.grupoempresarialdiaz.crm

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

data class Cita(
    val id: Int,
    val fecha: String,
    val hora: String,
    val paciente: String,
    val telefono: String,
    val sucursal: String,
    val tratamiento: String,
    val estado: String,
    val asistencia: String,
    val yaEsPaciente: Boolean,
)

data class Bloqueo(val sucursal: String, val texto: String)

private fun citaDe(o: JSONObject) = Cita(
    id = o.optInt("id_cita"),
    fecha = o.s("fecha"),
    hora = o.s("hora").take(5),
    paciente = o.s("paciente"),
    telefono = o.s("telefono"),
    sucursal = o.s("sucursal"),
    tratamiento = o.s("tipo_tratamiento"),
    estado = o.s("estado"),
    asistencia = o.s("asistencia"),
    yaEsPaciente = o.s("ya_es_paciente") == "Si",
)

class CrmViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)
    private val api = Api(prefs)

    var conSesion by mutableStateOf(prefs.token != null)
        private set
    var sucursal by mutableStateOf(prefs.sucursalFija ?: "")
        private set
    var fecha by mutableStateOf(LocalDate.now())
        private set
    var citas by mutableStateOf<List<Cita>>(emptyList())
        private set
    var bloqueos by mutableStateOf<List<Bloqueo>>(emptyList())
        private set
    var horario by mutableStateOf("")
        private set
    var cargando by mutableStateOf(false)
        private set
    var aviso by mutableStateOf<String?>(null)

    /** Opciones del selector: las sucursales permitidas y, si no tiene una fija, "Todas" (""). */
    val opcionesSucursal: List<String>
        get() = if (prefs.sucursalFija != null) listOf(prefs.sucursalFija!!) else listOf("") + prefs.sucursales

    private fun lanzar(bloque: suspend () -> Unit) = viewModelScope.launch {
        try {
            bloque()
        } catch (e: ApiException) {
            if (e.sesionExpirada) cerrarLocal()
            aviso = e.message
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Cualquier respuesta inesperada se muestra en pantalla en lugar de cerrar la app
            android.util.Log.e("CrmClinica", "Error", e)
            aviso = "Error inesperado: ${e.message ?: e.javaClass.simpleName}"
        } finally {
            cargando = false
        }
    }

    // ---------- Sesión ----------
    fun entrar(usuario: String, password: String, alFallar: (String) -> Unit) = lanzar {
        cargando = true
        val r = api.post(
            "api_login.php",
            mapOf("usuario" to usuario.trim(), "password" to password, "dispositivo" to "${Build.MANUFACTURER} ${Build.MODEL}"),
        )
        if (!r.ok()) { alFallar(r.mensaje()); return@lanzar }
        val u = r.getJSONObject("usuario")
        prefs.token = r.s("token")
        prefs.nombre = u.s("nombre")
        prefs.rol = u.s("rol")
        prefs.sucursalFija = u.s("sucursal_fija").ifBlank { null }
        prefs.puedeOperar = r.optBoolean("puede_operar")
        val suc = r.getJSONArray("sucursales")
        prefs.sucursales = List(suc.length()) { suc.getString(it) }
        val srv = r.getJSONArray("servicios")
        prefs.servicios = List(srv.length()) { srv.getString(it) }
        sucursal = prefs.sucursalFija ?: ""
        conSesion = true
        cargar()
    }

    fun salir() = lanzar {
        try { api.post("api_logout.php", emptyMap()) } catch (e: ApiException) { // si falla, igual se cierra la sesión en el teléfono
        }
        cerrarLocal()
    }

    private fun cerrarLocal() {
        prefs.borrar()
        citas = emptyList()
        conSesion = false
    }

    // ---------- Agenda ----------
    fun elegirSucursal(s: String) { sucursal = s; cargar() }
    fun moverDia(dias: Long) { fecha = fecha.plusDays(dias); cargar() }
    fun hoy() { fecha = LocalDate.now(); cargar() }

    fun cargar() = lanzar {
        if (!conSesion) return@lanzar
        cargando = true
        val dia = fecha.toString()
        val r = api.get("obtener_citas.php", mapOf("sucursal" to sucursal, "desde" to dia, "hasta" to dia))
        val cfg = api.get("obtener_config_agenda.php", mapOf("desde" to dia, "hasta" to dia))
        if (!r.ok()) { aviso = r.mensaje(); return@lanzar }
        val arr = r.getJSONArray("data")
        citas = List(arr.length()) { citaDe(arr.getJSONObject(it)) }.filter { it.fecha == dia }.sortedBy { it.hora }
        if (cfg.ok()) leerConfig(cfg)
    }

    private fun leerConfig(cfg: JSONObject) {
        val dow = (fecha.dayOfWeek.value % 7).toString()          // 0 = domingo, como en el servidor
        val sucs = cfg.getJSONArray("sucursales")
        var ini: String? = null
        var fin: String? = null
        for (i in 0 until sucs.length()) {
            val s = sucs.getJSONObject(i)
            if (sucursal.isNotEmpty() && s.s("nombre_sucursal") != sucursal) continue
            val h = s.optJSONObject("horarios")?.optJSONObject(dow) ?: continue
            if (ini == null || h.s("ini") < ini) ini = h.s("ini")
            if (fin == null || h.s("fin") > fin) fin = h.s("fin")
        }
        horario = if (ini == null) "Cerrado este día" else "Abierto de $ini a $fin"
        val bl = cfg.getJSONArray("bloqueos")
        bloqueos = List(bl.length()) { bl.getJSONObject(it) }
            .filter { sucursal.isEmpty() || it.s("sucursal") == sucursal }
            .map { b ->
                val horas = if (b.s("hora_inicio").isNotEmpty()) " (${b.s("hora_inicio")}–${b.s("hora_fin")})" else ""
                val quien = if (b.s("medico").isNotEmpty()) b.s("medico") + ": " else ""
                Bloqueo(b.s("sucursal"), "$quien${b.s("motivo")}$horas")
            }
    }

    fun marcar(cita: Cita, asistencia: String) = lanzar {
        val r = api.post("actualizar_asistencia.php", mapOf("id_cita" to cita.id.toString(), "asistencia" to asistencia))
        if (r.ok()) cargar() else aviso = r.mensaje()
    }

    /** Registra el cobro; el servidor pone el folio y marca la asistencia. */
    fun cobrar(cita: Cita, servicio: String, montos: Map<String, String>, forzar: Boolean, alTerminar: (folio: String?, duplicada: String?) -> Unit) = lanzar {
        val datos = mutableMapOf("id_cita" to cita.id.toString(), "servicio" to servicio, "fecha" to cita.fecha)
        datos.putAll(montos)
        if (forzar) datos["forzar"] = "1"
        val r = api.post("guardar_remision.php", datos)
        when {
            r.ok() -> { alTerminar(r.s("folio"), null); cargar() }
            r.optString("status") == "duplicada" -> alTerminar(null, r.mensaje())
            else -> aviso = r.mensaje()
        }
    }

    /** Guarda una cita nueva. Si cae fuera de horario o en una ausencia, el servidor avisa y se pide confirmar. */
    fun guardarCita(datos: Map<String, String>, forzar: Boolean, alAdvertir: (String) -> Unit, alGuardar: () -> Unit) = lanzar {
        val d = datos.toMutableMap()
        d["asistente"] = prefs.nombre
        d["asistencia"] = "Pendiente"
        if (forzar) d["forzar"] = "1"
        val r = api.post("guardar_cita.php", d)
        when {
            r.ok() -> { aviso = "Cita guardada"; alGuardar(); cargar() }
            r.optString("status") == "advertencia" -> alAdvertir(r.mensaje())
            else -> aviso = r.mensaje()
        }
    }
}
