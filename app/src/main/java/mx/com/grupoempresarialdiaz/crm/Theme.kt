package mx.com.grupoempresarialdiaz.crm

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mismos colores que la web nueva
val Jade = Color(0xFF0E7C66)
val JadeOscuro = Color(0xFF0A5E4D)
val JadeClaro = Color(0xFFE3F2EE)
val Tinta = Color(0xFF1E2B33)
val Niebla = Color(0xFFF3F6F5)
val Suave = Color(0xFF6B7B83)
val Linea = Color(0xFFDDE5E2)
val VerdeOk = Color(0xFF2F7D4F)
val VerdeOkFondo = Color(0xFFE4F4EA)
val Rosa = Color(0xFFA3263B)
val RosaFondo = Color(0xFFFBE9EC)
val Ambar = Color(0xFF8A5A12)
val AmbarFondo = Color(0xFFFDF3E3)

private val Colores = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = JadeClaro,
    onPrimaryContainer = JadeOscuro,
    secondary = Tinta,
    background = Niebla,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Niebla,
    onSurfaceVariant = Suave,
    outline = Linea,
    error = Rosa,
)

@Composable
fun CrmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, content = content)
}
