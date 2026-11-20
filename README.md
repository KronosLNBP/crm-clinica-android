# CRM Clínica de Rodillas — app Android

App nativa (Kotlin + Jetpack Compose) para recepción. Usa el mismo servidor que el CRM web
(`crm-clinica-de-rodillas`), con login por token (`api_login.php`).

## Qué hace (v0.1)
- Iniciar sesión con el usuario del CRM (la recepción con sucursal asignada solo ve la suya).
- Agenda del día por sucursal: anterior / hoy / siguiente, horario y ausencias del día.
- Marcar **Llegó / Faltó** en un toque.
- **Cobrar**: el servidor pone el folio y marca la asistencia; opción de mandar comprobante por WhatsApp.
- **Nueva cita**, con aviso si cae fuera de horario o en una ausencia.
- Llamar al paciente.

Requiere internet. No guarda datos de pacientes en el teléfono: solo el token de acceso.

## Compilar
Cada push a `main` compila el APK en GitHub Actions (pestaña **Actions → Compilar APK → Artifacts**).
Para abrirlo en Android Studio: *Open* → esta carpeta.

## Servidor
- La dirección del CRM está en `app/build.gradle` (`BASE_URL`).
- En la base de datos debe estar aplicada `migracion_v4.sql` (tabla `api_tokens`).

## Pendientes
- Recibo en PDF desde el teléfono (`adjuntar_pdf_remision.php`).
- Vista de semana, ausencias, expediente y notas del médico.
- Firma de producción y publicación en Play Store.
