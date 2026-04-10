package com.nirv.scansheet.domain.usecase

import com.nirv.scansheet.data.ScanRepository
import com.nirv.scansheet.data.excel.ExcelExporter
import java.io.File

class ExportToExcelUseCase(
    private val repository: ScanRepository,
    private val exporter: ExcelExporter
) {
    // Toma las filas guardadas en ScanRepository y genera el .xlsx
    operator fun invoke(): File {
        val rows = repository.getRows()
        if (rows.isEmpty()) error("No hay datos para exportar")
        return exporter.export(rows)
    }
}
