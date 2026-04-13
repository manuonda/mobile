# Feature: Planificador de Envío de Documentos

## Descripción

Permitir al usuario programar el envío automático de uno o varios PDFs a través de **WhatsApp** o **Email** en una fecha y hora específica. La tarea se ejecuta en background aunque la app esté cerrada, notificando al usuario del resultado.

**Ejemplo de uso:**
> "Quiero enviar estos 3 documentos por email a cliente@empresa.com el lunes a las 15:30"

---

## Referencia UX / Flujo de pantallas

```
PreviewScreen
    └── Botón "Programar envío"
            └── ScheduleScreen  ← nuevo
                    ├── Seleccionar PDFs (del historial o el actual)
                    ├── Elegir canal: WhatsApp | Email
                    ├── Ingresar destinatario
                    ├── Elegir fecha y hora
                    └── Confirmar → tarea guardada
                            └── ScheduleListScreen  ← nuevo
                                    ├── Lista de tareas programadas
                                    ├── Estado: Pendiente / Enviado / Fallido
                                    └── Opción cancelar tarea
```

---

## Épica y User Stories

### US-01 — Crear tarea de envío
**Como** usuario  
**Quiero** programar el envío de un PDF a un contacto  
**Para** que se envíe automáticamente sin que yo esté presente

**Criterios de aceptación:**
- [ ] Puedo seleccionar uno o varios PDFs del historial o el documento actual
- [ ] Puedo elegir el canal: WhatsApp o Email
- [ ] Puedo ingresar el destinatario (número de teléfono / dirección email)
- [ ] Puedo seleccionar fecha y hora mediante un DateTimePicker
- [ ] Recibo confirmación visual de que la tarea quedó guardada
- [ ] No se puede programar una tarea en el pasado (validación en UI)

---

### US-02 — Listar tareas programadas
**Como** usuario  
**Quiero** ver todas mis tareas de envío pendientes  
**Para** saber qué se enviará y cuándo

**Criterios de aceptación:**
- [ ] Lista ordenada por fecha ascendente
- [ ] Cada ítem muestra: destinatario, canal, fecha/hora, nombre del documento
- [ ] Badge de estado visual: Pendiente 🕐 / Enviado ✓ / Fallido ✗ / Cancelado
- [ ] Puedo cancelar una tarea pendiente antes de que se ejecute
- [ ] Las tareas enviadas o fallidas permanecen en el historial (solo lectura)

---

### US-03 — Ejecutar envío automático en background
**Como** sistema  
**Quiero** ejecutar el envío en el momento programado  
**Para** que el usuario no tenga que estar activo en la app

**Criterios de aceptación:**
- [ ] Funciona con la app en background o cerrada (WorkManager exact alarm)
- [ ] Si no hay red, reintenta automáticamente hasta 3 veces con backoff de 5 min
- [ ] El estado de la tarea se actualiza en Room (SENT / FAILED)
- [ ] Se envía notificación push al completarse exitosamente
- [ ] Se envía notificación push si falla tras los 3 reintentos

---

### US-04 — Notificación de recordatorio previo
**Como** usuario  
**Quiero** recibir una notificación antes del envío programado  
**Para** poder cancelarlo si cambié de opinión

**Criterios de aceptación:**
- [ ] Notificación configurable: 5, 15 o 30 minutos antes
- [ ] La notificación incluye acción rápida "Cancelar" sin abrir la app
- [ ] El tiempo de aviso se configura al crear la tarea

---

## Arquitectura técnica

### Capas

```
UI Layer
  ScheduleScreen            — formulario de nueva tarea
  ScheduleListScreen        — lista de tareas programadas
  ScheduleViewModel         — estado UI + casos de uso

Domain Layer
  SendTask                  — entidad principal
  SendTaskRepository        — interfaz de datos
  ScheduleSendUseCase       — crea tarea + encola WorkManager
  CancelTaskUseCase         — cancela tarea + cancela Worker

Data / Worker Layer
  Room DB                   — persistencia de SendTask
  SendDocumentWorker        — Worker de ejecución del envío
  SendReminderWorker        — Worker de notificación previa
  WhatsAppSender            — envío vía Intent (semi-automático)
  EmailSender               — envío vía JavaMail / SMTP (automático)
```

### Modelo de datos

```kotlin
@Entity(tableName = "send_tasks")
data class SendTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pdfUris: List<String>,            // URIs de los PDFs a enviar
    val channel: Channel,                 // WHATSAPP | EMAIL
    val recipient: String,                // número o dirección email
    val scheduledAt: Long,                // epoch millis
    val reminderMinutesBefore: Int = 15,  // aviso previo
    val status: TaskStatus = TaskStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class Channel     { WHATSAPP, EMAIL }
enum class TaskStatus  { PENDING, SENT, FAILED, CANCELLED }
```

### Flujo WorkManager

```
Tarea guardada en Room
        │
        ├─► Worker "recordatorio"  → setExactAndAllowWhileIdle(scheduledAt - reminderMin)
        │       └── Notificación: "Envío en X minutos — [Cancelar]"
        │
        └─► Worker "envío"         → setExactAndAllowWhileIdle(scheduledAt)
                ├── Obtiene PDF desde URI
                ├── Canal WHATSAPP → Intent whatsapp://send + archivo adjunto
                │   Canal EMAIL   → JavaMail SMTP en background
                ├── Éxito  → Room status = SENT  + notif "Documento enviado ✓"
                └── Error  → retryCount < 3 → reencola con backoff 5 min
                             retryCount ≥ 3 → Room status = FAILED + notif "Error al enviar ✗"
```

---

## Dependencias a agregar

```kotlin
// build.gradle.kts (app)
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("com.sun.mail:android-mail:1.6.7")        // JavaMail para Email
implementation("com.sun.mail:android-activation:1.6.7")
```

---

## Permisos Android requeridos

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />       <!-- API 31+ -->
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />             <!-- API 33+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />          <!-- API 33+ -->
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Consideraciones y restricciones

| Tema | Detalle |
|------|---------|
| **WhatsApp** | No existe API oficial. El Intent abre WhatsApp con el archivo, pero **el usuario debe pulsar Enviar manualmente**. Para envío 100% automático se requiere WhatsApp Business API (aprobación de Meta). |
| **Email** | Con JavaMail + credenciales SMTP (Gmail, Outlook, etc.) el envío es completamente automático en background. Requiere que el usuario configure su cuenta SMTP en Settings de la app. |
| **Exact Alarms Android 12+** | A partir de API 31 se requiere permiso especial. Si el usuario no lo concede, se usa `setAndAllowWhileIdle` como fallback (menos preciso, ~10 min de margen). |
| **Doze Mode** | WorkManager maneja correctamente Doze. `setExactAndAllowWhileIdle` garantiza ejecución aunque el dispositivo esté en modo de ahorro. |
| **URIs de PDF** | Las URIs deben ser persistidas con `contentResolver.takePersistableUriPermission` para que el Worker pueda acceder al archivo después de que la app se cierre. |

---

## Sprints de desarrollo

### Sprint 1 — Fundación de datos y UI (1 semana)
- [ ] Modelo `SendTask` + Room DB + DAO + TypeConverters
- [ ] `SendTaskRepository` + inyección Koin
- [ ] `ScheduleScreen` — formulario con DateTimePicker, selector de canal, campo destinatario
- [ ] `ScheduleViewModel` con validaciones (fecha futura, formato email/teléfono)
- [ ] Botón "Programar envío" en `PreviewScreen`
- [ ] Navegación hacia `ScheduleScreen`

### Sprint 2 — Ejecución en background (1 semana)
- [ ] `ScheduleSendUseCase` — guarda en Room + encola WorkManager
- [ ] `SendDocumentWorker` — lógica de envío + reintentos
- [ ] `EmailSender` — integración JavaMail SMTP
- [ ] `WhatsAppSender` — Intent con adjunto
- [ ] `SendReminderWorker` — notificación de aviso previo
- [ ] Gestión de permisos `SCHEDULE_EXACT_ALARM` en runtime

### Sprint 3 — Lista de tareas y notificaciones (1 semana)
- [ ] `ScheduleListScreen` — lista con estados visuales
- [ ] `CancelTaskUseCase` — cancela Room + WorkManager
- [ ] Acción rápida "Cancelar" desde notificación
- [ ] Notificación de resultado (éxito / fallo)
- [ ] Pantalla de configuración SMTP (cuenta de email)
- [ ] Tests unitarios de WorkerFactory y UseCases

---

## Definición de Done (DoD)

- [ ] Código revisado y mergeado en `main`
- [ ] Unit tests para UseCases y ViewModel (cobertura ≥ 70%)
- [ ] Probado en dispositivo real con app en background y pantalla apagada
- [ ] Probado en Android 12+ (permisos exact alarm)
- [ ] Sin crashes en Firebase Crashlytics tras 24h en staging
- [ ] Documentación de pantallas actualizada en `/docs/screens`
