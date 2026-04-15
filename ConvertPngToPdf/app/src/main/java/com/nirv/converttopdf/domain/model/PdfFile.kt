package com.nirv.converttopdf.domain.model

import android.net.Uri

data class PdfFile(
    val name: String,
    val path: String,
    val size: String,
    val lastModified: Long,
    val uri: Uri
)
