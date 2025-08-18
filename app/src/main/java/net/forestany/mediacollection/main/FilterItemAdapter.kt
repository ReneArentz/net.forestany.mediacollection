package net.forestany.mediacollection.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.mediacollection.R

class FilterItemAdapter(
    private val filterItems: List<FilterItem>,
    private val onFilterItemSelected: (FilterItem) -> Unit
) : RecyclerView.Adapter<FilterItemAdapter.FilterItemViewHolder>() {
    inner class FilterItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val filterItemName: TextView = view.findViewById(R.id.filterItemName)
        private val filterItemValue: TextView = view.findViewById(R.id.filterItemValue)
        private val filterItemDeleteButton: ImageButton = view.findViewById(R.id.filterItemDeleteButton)

        fun bind(filterItem: FilterItem) {
            filterItemName.text = filterItem.displayName
            filterItemValue.text = filterItem.value
            filterItemDeleteButton.setOnClickListener { onFilterItemSelected(filterItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_dialog_filter_item, parent, false)
        return FilterItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterItemViewHolder, position: Int) {
        holder.bind(filterItems[position])
    }

    override fun getItemCount() = filterItems.size
}