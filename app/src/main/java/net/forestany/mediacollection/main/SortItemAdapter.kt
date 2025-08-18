package net.forestany.mediacollection.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.mediacollection.R

class SortItemAdapter(
    private val sortItems: List<SortItem>,
    private val onSortItemSelected: (SortItem) -> Unit
) : RecyclerView.Adapter<SortItemAdapter.SortItemViewHolder>() {
    inner class SortItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val holderContext = view.context
        private val sortItemName: TextView = view.findViewById(R.id.sortItemName)
        private val sortItemDirection: TextView = view.findViewById(R.id.sortItemDirection)
        private val sortItemDeleteButton: ImageButton = view.findViewById(R.id.sortItemDeleteButton)

        fun bind(sortItem: SortItem) {
            sortItemName.text = sortItem.displayName
            sortItemDirection.text = if (sortItem.direction) { holderContext.getString(R.string.main_sort_ascending) } else { holderContext.getString(R.string.main_sort_descending) }
            sortItemDeleteButton.setOnClickListener { onSortItemSelected(sortItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_dialog_sort_item, parent, false)
        return SortItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: SortItemViewHolder, position: Int) {
        holder.bind(sortItems[position])
    }

    override fun getItemCount() = sortItems.size
}