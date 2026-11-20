package mx.com.grupoempresarialdiaz.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: CrmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (vm.conSesion) vm.cargar()
        setContent {
            CrmTheme {
                if (vm.conSesion) AgendaScreen(vm) else LoginScreen(vm)
            }
        }
    }
}
