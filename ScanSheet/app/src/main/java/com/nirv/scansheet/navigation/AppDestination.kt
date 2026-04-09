package com.nirv.scansheet.navigation

/**
 * AppDestination define todos los puntos de navegación (destinos) de la aplicación.
 * 
 * En Jetpack Navigation 3, se recomienda usar objetos o clases de datos (data objects)
 * en lugar de Strings para definir las rutas. Esto proporciona seguridad de tipos (type-safety)
 * y evita errores al escribir nombres de rutas.
 */

// Representa la pantalla principal donde el usuario ve las opciones de inicio.
data object Home 

// Representa la pantalla de captura, donde se utiliza la cámara para escanear.
data object Capture 

// Representa la pantalla de previsualización de los datos capturados.
data object Preview

// Representa la pantalla de exportación de los datos (por ejemplo, a Excel).
data object Export
