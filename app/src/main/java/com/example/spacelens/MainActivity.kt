package com.example.spacelens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerFiles: RecyclerView
    private lateinit var bubbleView: BubbleView
    private lateinit var txtPath: TextView
    private lateinit var txtDriveInfo: TextView
    private lateinit var txtUsed: TextView
    private lateinit var progressStorage: ProgressBar
    private lateinit var btnBack: ImageButton

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private val history = mutableListOf<File>()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) loadDirectory(currentDir)
        else checkManageStorage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerFiles = findViewById(R.id.recyclerFiles)
        bubbleView = findViewById(R.id.bubbleView)
        txtPath = findViewById(R.id.txtPath)
        txtDriveInfo = findViewById(R.id.txtDriveInfo)
        txtUsed = findViewById(R.id.txtUsed)
        progressStorage = findViewById(R.id.progressStorage)
        btnBack = findViewById(R.id.btnBack)

        recyclerFiles.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener {
            if (history.isNotEmpty()) {
                val prev = history.removeLast()
                loadDirectory(prev, addToHistory = false)
            } else {
                Toast.makeText(this, "Sudah di root", Toast.LENGTH_SHORT).show()
            }
        }

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Izin Diperlukan")
                    .setMessage("Aplikasi butuh akses ke semua file untuk visualisasi disk.")
                    .setPositiveButton("Buka Pengaturan") { _, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            } else loadDirectory(currentDir)
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
                loadDirectory(currentDir)
            } else permLauncher.launch(perms)
        }
    }

    private fun checkManageStorage() {
        Toast.makeText(this, "Izin diperlukan", Toast.LENGTH_SHORT).show()
    }

    private fun loadDirectory(dir: File, addToHistory: Boolean = true) {
        if (!dir.exists() || !dir.canRead()) {
            Toast.makeText(this, "Tidak bisa akses folder", Toast.LENGTH_SHORT).show()
            return
        }

        if (addToHistory && currentDir.absolutePath != dir.absolutePath) {
            history.add(currentDir)
        }
        currentDir = dir

        val files = dir.listFiles() ?: emptyArray()
        val nodes = files.map { f ->
            val size = if (f.isDirectory) getFolderSize(f) else f.length()
            FileNode(
                file = f,
                name = f.name,
                size = size,
                isDirectory = f.isDirectory,
                itemCount = if (f.isDirectory) (f.listFiles()?.size ?: 0) else 0
            )
        }.sortedByDescending { it.size }

        // Update UI
        txtPath.text = dir.name.ifEmpty { "Internal Storage" }
        updateStorageInfo(dir, nodes)

        // Setup adapter
        recyclerFiles.adapter = FileListAdapter(nodes) { node ->
            if (node.isDirectory) loadDirectory(node.file)
            else Toast.makeText(this, "File: ${node.name}", Toast.LENGTH_SHORT).show()
        }

        // Setup bubbles
        bubbleView.setNodes(nodes)
        bubbleView.setOnBubbleClick { node ->
            if (node.isDirectory) loadDirectory(node.file)
            else Toast.makeText(this, "File: ${node.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStorageInfo(dir: File, nodes: List<FileNode>) {
        val totalSize = nodes.sumOf { it.size }
        val stat = android.os.StatFs(dir.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val used = total - free

        val totalGB = total / (1024.0 * 1024 * 1024)
        val usedGB = used / (1024.0 * 1024 * 1024)

        txtDriveInfo.text = "${formatSize(total)}  |  ${nodes.size} items"
        txtUsed.text = "${formatSize(used)} of ${formatSize(total)} used"
        progressStorage.progress = ((used.toDouble() / total.toDouble()) * 100).toInt()
    }

    private fun getFolderSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
            if (size > 2L * 1024 * 1024 * 1024) return size // cap 2GB for perf
        }
        return size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (history.isNotEmpty()) {
            val prev = history.removeLast()
            loadDirectory(prev, addToHistory = false)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
