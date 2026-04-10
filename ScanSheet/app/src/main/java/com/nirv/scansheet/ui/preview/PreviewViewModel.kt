package com.nirv.scansheet.ui.preview

import androidx.lifecycle.ViewModel
import com.nirv.scansheet.data.ScanRepository
import com.nirv.scansheet.domain.model.ExcelRow
import kotlinx.coroutines.flow.StateFlow

class PreviewViewModel(
    private val repository: ScanRepository
) : ViewModel() {

    val rows: StateFlow<List<ExcelRow>> = repository.rows

    // Actualiza una celda individual y persiste en el repositorio
    fun updateCell(rowIndex: Int, colIndex: Int, value: String) {
        val rows = repository.getRows().toMutableList()
        val row = rows.getOrNull(rowIndex) ?: return
        val cols = row.columns.toMutableList()
        if (colIndex in cols.indices) {
            cols[colIndex] = value
            rows[rowIndex] = ExcelRow(cols)
            repository.saveRows(rows)
        }
    }

    fun deleteRow(rowIndex: Int) {
        val rows = repository.getRows().toMutableList()
        if (rowIndex in rows.indices) {
            rows.removeAt(rowIndex)
            repository.saveRows(rows)
        }
    }
}
