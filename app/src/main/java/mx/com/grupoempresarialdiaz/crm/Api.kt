package mx.com.grupoempresarialdiaz.crm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Error que se muestra tal cual a la persona. */
class ApiException(message: String, val sesionExpirada: Boolean = false) : Exception(message)

/** Datos guardados en el teléfono: solo el token de acceso y los catálogos, nunca datos de pacientes. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("crm", Context.MODE_PRIVATE)

    var token: String?
        get() = sp.getString("token", null)
        set(v) = sp.edit().putString("token", v).apply()
    var nombre: String
        get() = sp.getString("nombre", "") ?: ""
        set(v) = sp.edit().putString("nombre", v).apply()
    var rol: String
        get() = sp.getString("rol", "") ?: ""
        set(v) = sp.edit().putString("rol", v).apply()
    var sucursalFija: String?
        get() = sp.getString("sucursal_fija", null)
        set(v) = sp.edit().putString("sucursal_fija", v).apply()
    var puedeOperar: Boolean
        get() = sp.getBoolean("puede_operar", false)
        set(v) = sp.edit().putBoolean("puede_operar", v).apply()
    var sucursales: List<String>
        get() = lista(sp.getString("sucursales", "[]"))
        set(v) = sp.edit().putString("sucursales", JSONArray(v).toString()).apply()
    var servicios: List<String>
        get() = lista(sp.getString("servicios", "[]"))
        set(v) = sp.edit().putString("servicios", JSONArray(v).toString()).apply()

    fun borrar() = sp.edit().clear().apply()

    private fun lista(json: String?): List<String> {
        val a = JSONArray(json ?: "[]")
        return List(a.length()) { a.getString(it) }
    }
}

/** Cliente del CRM: usa los mismos endpoints PHP que la web, con el encabezado X-Api-Token. */
class Api(private val prefs: Prefs) {
    private val base = BuildConfig.BASE_URL

    suspend fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val q = form(params)
            enviar(URL(base + path + if (q.isEmpty()) "" else "?$q"), null)
        }

    suspend fun post(path: String, params: Map<String, String>): JSONObject =
        withContext(Dispatchers.IO) { enviar(URL(base + path), form(params)) }

    private fun form(p: Map<String, String>) =
        p.entries.joinToString("&") { URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }

    private fun enviar(url: URL, cuerpo: String?): JSONObject {
        val c = url.openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            c.setRequestProperty("Accept", "application/json")
            prefs.token?.let { c.setRequestProperty("X-Api-Token", it) }
            if (cuerpo != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                c.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            }
            val codigo = c.responseCode
            val flujo = if (codigo in 200..299) c.inputStream else (c.errorStream ?: c.inputStream)
            val texto = flujo.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = try {
                JSONObject(texto)
            } catch (e: Exception) {
                throw ApiException("Respuesta inesperada del servidor (código $codigo).")
            }
            if (codigo == 401 && json.optBoolean("login")) {
                throw ApiException(json.optString("message", "Tu sesión expiró."), sesionExpirada = true)
            }
            return json
        } catch (e: IOException) {
            throw ApiException("No hay conexión con el servidor. Revisa tu internet.")
        } finally {
            c.disconnect()
        }
    }
}

/** Texto de un campo JSON; los null del servidor se vuelven "". */
fun JSONObject.s(k: String): String = if (!has(k) || isNull(k)) "" else optString(k)

fun JSONObject.ok(): Boolean = optString("status") == "success"

fun JSONObject.mensaje(): String = s("message").ifBlank { "Ocurrió un error. Intenta de nuevo." }
