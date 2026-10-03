package com.example.spacelens

import java.io.File

data class FileNode(
    val file: File,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val itemCount: Int = 0
)
