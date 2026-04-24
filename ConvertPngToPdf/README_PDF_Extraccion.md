# Extracción de Datos PDF Bancarios a Excel

## Objetivo

Extraer información de estados de cuenta bancarios (PDFs) y exportarlos a Excel de forma estructurada.

## Stack Recomendado

| Etapa | Herramienta | Uso |
|-------|-------------|-----|
| Extracción PDF | `pdfplumber` o `DocLing` | Leer tablas y texto de PDFs bancarios |
| Procesamiento | `pandas` | Transformar y limpiar datos |
| Exportación | `openpyxl` | Generar archivos Excel |

## Instalación

```bash
pip install pdfplumber pandas openpyxl
```

## Uso Básico

```python
import pdfplumber
import pandas as pd

def extraer_tabla_pdf(ruta_pdf):
    """Extrae todas las tablas de un PDF"""
    tablas = []
    with pdfplumber.open(ruta_pdf) as pdf:
        for page in pdf.pages:
            tablas_pagina = page.extract_tables()
            tablas.extend(tablas_pagina)
    return tablas

def a_excel(tablas, archivo_salida):
    """Exporta tablas a Excel, una hoja por tabla"""
    with pd.ExcelWriter(archivo_salida, engine='openpyxl') as writer:
        for i, tabla in enumerate(tablas):
            if tabla:
                df = pd.DataFrame(tabla[1:], columns=tabla[0])
                df.to_excel(writer, sheet_name=f"Tabla_{i+1}", index=False)

# Uso
tablas = extraer_tabla_pdf("estado_cuenta.pdf")
a_excel(tablas, "output.xlsx")
```

## Para PDFs Complejos (Escaneados con OCR)

```bash
pip install docling-core docling-parse
```

```python
from docling.parse import parse_pdf

result = parse_pdf("estado_cuenta.pdf")
for item in result.iter_items():
    if item.category == "table":
        item.to_pandas().to_excel("tabla.xlsx", index=False)
```

## Estructura Típica de un Estado de Cuenta Bancario

```
┌─────────────────────────────────────┐
│ Banco XXX                           │
│ Cuenta: ****1234                    │
│ Periodo: Enero 2025                 │
├─────────────────────────────────────┤
│ Fecha    │ Descripción │ Monto     │
│----------|-------------|-----------│
│ 01/01    │ Compra      │ -$500.00  │
│ 05/01    │ Depósito    │ +$2,000.00│
└─────────────────────────────────────┘
```

## Tips

1. **PDFs escaneados**: Usa `DocLing` (tiene OCR integrado)
2. **Tablas con líneas fusionadas**: `pdfplumber` con `split_text=True`
3. **Datos mixtos (texto + tablas)**: Combina `extract_text()` + `extract_tables()`

## Alternativas Rápidas en Línea

- **Tabula PDF**: tabula.ralix.com (gratis, sin código)
- **PDFTables**: pdftables.com (conversor online)
