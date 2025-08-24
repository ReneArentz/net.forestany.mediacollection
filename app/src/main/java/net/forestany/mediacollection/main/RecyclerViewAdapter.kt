package net.forestany.mediacollection.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.forestany.mediacollection.R

class RecyclerViewAdapter: RecyclerView.Adapter<ItemViewHolder>() {
    interface RecyclerViewAdapterDelegate {
        fun onClickItem(itemBean: ItemBean)
        fun onLongClickItem(itemBean: ItemBean)
    }

    private var mutableList: MutableList<ItemBean> = mutableListOf()

    var delegate: RecyclerViewAdapterDelegate? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mutableList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.itemBean = mutableList[position]
        holder.delegate = object : ItemViewHolder.ItemViewHolderDelegate {
            override fun onItemViewClick(itemBean: ItemBean) {
                delegate?.onClickItem(itemBean)
            }

            override fun onItemViewLongClick(itemBean: ItemBean) {
                delegate?.onLongClickItem(itemBean)
            }
        }

        holder.updateView()
    }

    fun clear() {
        val size = this.mutableList.size
        this.mutableList.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun refresh(mutableList: MutableList<ItemBean>) {
        this.clear()
        this.mutableList.addAll(mutableList)
        // better notifyItemRangeChanged() than notifyDataSetChanged()
        notifyItemRangeChanged(0, this.mutableList.size)
    }

    fun loadMore(mutableList: MutableList<ItemBean>) {
        // check if item is not already in list by uuid
        var i = 0

        for (newItemBean in mutableList) {
            var new = true

            for (itemBean in this.mutableList) {
                if (itemBean.uuid.contentEquals(newItemBean.uuid)) {
                    new = false
                }
            }

            if (new) {
                this.mutableList.add(newItemBean)
                i++
            }
        }

        if (i > 0) {
            notifyItemRangeChanged(this.mutableList.size - i + 1, i)
        }
    }
}