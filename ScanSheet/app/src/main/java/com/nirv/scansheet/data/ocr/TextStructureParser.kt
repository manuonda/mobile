package com.nirv.scansheet.data.ocr

import com.nirv.scansheet.domain.model.ExcelRow

/**
 * Convierte líneas crudas de OCR en filas de 2 columnas: [descripción, valor].
 *
 * Patrones soportados (todos producen la misma salida):
 *   "Grasmuy $23000"       → ["Grasmuy",  "23000"]
 *   "Canal 2  $38.000"     → ["Canal 2",  "38000"]
 *   "Telecom · $18000"     → ["Telecom",  "18000"]
 *   "precio1 13"           → ["precio1",  "13"]
 *   "precio1$13"           → ["precio1",  "13"]
 *   "Solo texto"           → ["Solo texto"]        ← sin valor numérico
 */
class TextStructureParser {

    // Detecta el patrón:  <descripción>  [separador opcional]  [$] <número>
    // Separadores aceptados: espacios, ·, •, -, tab (entre descripción y precio)
    private val pricePattern = Regex(
        """^(.+?)\s*[·•\-\t]?\s*\$?\s*(\d[\d.,]*)$"""
    )

    fun parse(rawLines: List<String>): List<ExcelRow> =
        rawLines.map { parseLine(it.trim()) }

    private fun parseLine(line: String): ExcelRow {
        val match = pricePattern.matchEntire(line)
        return if (match != null) {
            val description = match.groupValues[1].trim()
            val amount = match.groupValues[2]
                .replace(".", "")   // elimina puntos de miles: 38.000 → 38000
                .replace(",", ".")  // normaliza coma decimal: 38,5 → 38.5
            ExcelRow(listOf(description, amount))
        } else {
            // Línea sin número reconocible → celda única
            ExcelRow(listOf(line))
        }
    }
}
