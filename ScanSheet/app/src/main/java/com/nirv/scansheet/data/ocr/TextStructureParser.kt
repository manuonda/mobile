package com.nirv.scansheet.data.ocr

import com.nirv.scansheet.domain.model.ExcelRow

/**
 * Convierte líneas crudas de OCR en filas estructuradas (ExcelRow).
 * Soporta N columnas — no hay límite de columnas por fila.
 *
 * Separadores reconocidos entre columnas:
 *   - Punto medio · o bullet •
 *   - Guión -
 *   - Dos o más espacios consecutivos
 *   - Tab
 *
 * Ejemplos:
 *   "Tornillo 5mm · $850"            → ExcelRow(["Tornillo 5mm", "$850"])
 *   "Camiseta M · 24 · stock"        → ExcelRow(["Camiseta M", "24", "stock"])
 *   "Camiseta M  24"                 → ExcelRow(["Camiseta M", "24"])
 *   "Nota sin precio"                → ExcelRow(["Nota sin precio"])
 */
class TextStructureParser {

    // Separadores reconocidos entre columnas
    private val separatorRegex = Regex("""[\t·•\-]{1,3}|\s{2,}""")

    // Detecta si un token parece valor numérico o precio
    private val valueRegex = Regex("""^\$?\d+([.,]\d+)?$""")

    fun parse(rawLines: List<String>): List<ExcelRow> {
        return rawLines.map { line -> parseLine(line) }
    }

    private fun parseLine(line: String): ExcelRow {
        val parts = separatorRegex.split(line)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (parts.size >= 2) {
            ExcelRow(columns = parts)
        } else {
            ExcelRow(columns = splitByTrailingValue(line))
        }
    }

    /**
     * Fallback: busca un número o precio al final de la línea.
     * "Camiseta M 24" → ["Camiseta M", "24"]
     */
    private fun splitByTrailingValue(line: String): List<String> {
        val tokens = line.trim().split(" ")
        val lastToken = tokens.last()

        return if (tokens.size > 1 && valueRegex.matches(lastToken)) {
            listOf(tokens.dropLast(1).joinToString(" "), lastToken)
        } else {
            listOf(line.trim())
        }
    }
}
