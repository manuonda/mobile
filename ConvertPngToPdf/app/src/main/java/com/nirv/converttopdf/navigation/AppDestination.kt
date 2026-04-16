package com.nirv.converttopdf.navigation

data object Home
data class  Capture(val autoLaunchScanner: Boolean = false, val documentId: Long? = null)
data class  Preview(val documentId: Long)
data object Export
data object Sign
data object DrawSign
data object Settings
data object DirectoryFiles
