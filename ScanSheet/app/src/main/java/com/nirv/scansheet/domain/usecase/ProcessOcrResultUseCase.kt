package com.nirv.scansheet.domain.usecase

import com.nirv.scansheet.data.ScanRepository
import com.nirv.scansheet.data.ocr.TextStructureParser
import com.nirv.scansheet.domain.model.ExcelRow

/**
 * Orquesta el procesamiento del texto OCR crudo:
 * List<String> → TextStructureParser → ScanRepository
 */
class ProcessOcrResultUseCase(
    private val parser: TextStructureParser,
    private val repository: ScanRepository
) {
    operator fun invoke(rawLines: List<String>): List<ExcelRow> {
        val rows = parser.parse(rawLines)
        repository.saveRows(rows)
        return rows
    }
}
