package com.example.weighttrackerapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weighttrackerapp.R
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.domain.formatWeight

// Shows weight entries in a RecyclerView; only rows that changed are redrawn
class WeightAdapter(
    private val onEdit: (WeightEntry) -> Unit,
    private val onDelete: (WeightEntry) -> Unit
) : ListAdapter<WeightEntry, WeightAdapter.RowHolder>(DIFF) {

    class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.rowDate)
        val weight: TextView = view.findViewById(R.id.rowWeight)
        val edit: Button = view.findViewById(R.id.rowEditButton)
        val delete: Button = view.findViewById(R.id.rowDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weight, parent, false)
        return RowHolder(view)
    }

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        val entry = getItem(position)
        holder.date.text = entry.date
        holder.weight.text = formatWeight(entry.weight)
        holder.edit.setOnClickListener { onEdit(entry) }
        holder.delete.setOnClickListener { onDelete(entry) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WeightEntry>() {
            override fun areItemsTheSame(old: WeightEntry, new: WeightEntry) = old.id == new.id
            override fun areContentsTheSame(old: WeightEntry, new: WeightEntry) = old == new
        }
    }
}