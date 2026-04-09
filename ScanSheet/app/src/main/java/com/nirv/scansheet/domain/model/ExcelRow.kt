package com.nirv.scansheet.domain.model

/**
 * Representa una fila del Excel.
 * columns es ilimitado — cada elemento será una columna (A, B, C...).
 *
 * Ejemplos:
 *   ["Tornillo 5mm", "$850"]              → 2 columnas
 *   ["Camiseta M", "24", "stock"]         → 3 columnas
 *   ["Nota sin separador"]                → 1 columna
 */
data class ExcelRow(val columns: List<String>)
