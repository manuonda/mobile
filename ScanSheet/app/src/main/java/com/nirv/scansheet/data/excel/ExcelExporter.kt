package com.nirv.scansheet.data.excel

import android.content.Context
import com.nirv.scansheet.domain.model.ExcelRow
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.File

class ExcelExporter(private val context: Context) {

    fun export(rows: List<ExcelRow>, fileName: String = "scansheet_export.xlsx"): File {
        val file = File(context.cacheDir, fileName)

        file.outputStream().use { outputStream ->
            val workbook = Workbook(outputStream, "ScanSheet", "1.0")
            val sheet: Worksheet = workbook.newWorksheet("Datos")

            // Encabezados: Columna A, Columna B, ...
            val maxCols = rows.maxOfOrNull { it.columns.size } ?: 1
            for (col in 0 until maxCols) {
                sheet.value(0, col, columnLabel(col))
                sheet.style(0, col).bold().fillColor("1F4E79").fontColor("FFFFFF").set()
            }

            // Filas de datos
            rows.forEachIndexed { rowIndex, excelRow ->
                excelRow.columns.forEachIndexed { col, value ->
                    sheet.value(rowIndex + 1, col, value)
                }
            }

            workbook.finish()
        }

        return file
    }

    // 0 → "Columna A", 1 → "Columna B", 26 → "Columna AA"
    private fun columnLabel(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
        } while (n >= 0)
        return "Columna $sb"
    }
}
