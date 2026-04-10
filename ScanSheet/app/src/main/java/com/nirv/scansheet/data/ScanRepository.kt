package com.nirv.scansheet.data

import com.nirv.scansheet.domain.model.ExcelRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repositorio en memoria que actúa como puente entre ViewModels.
 * CaptureViewModel escribe → PreviewViewModel y ExportViewModel leen.
 * Será @Singleton via Hilt (por ahora instancia manual).
 */
class ScanRepository {

    private val _rows = MutableStateFlow<List<ExcelRow>>(emptyList())
    val rows: StateFlow<List<ExcelRow>> = _rows.asStateFlow()

    fun saveRows(rows: List<ExcelRow>) {
        _rows.value = rows
    }

    fun getRows(): List<ExcelRow> = _rows.value

    fun updateRow(index: Int, row: ExcelRow) {
        val current = _rows.value.toMutableList()
        if (index in current.indices) {
            current[index] = row
            _rows.value = current
        }
    }

    fun clear() {
        _rows.value = emptyList()
    }
}
