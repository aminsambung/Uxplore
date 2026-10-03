package com.example.spacelens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileListAdapter(
    private val items: List<FileNode>,
    private val onClick: (FileNode) -> Unit
) : RecyclerView.Adapter<FileListAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.imgIcon)
        val info: ImageView = v.findViewById(R.id.imgInfo)
        val name: TextView = v.findViewById(R.id.txtName)
        val size: TextView = v.findViewById(R.id.txtSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        h.name.text = item.name
        h.size.text = formatSize(item.size)
        h.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }
}
