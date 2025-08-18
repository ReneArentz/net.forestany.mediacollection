package net.forestany.mediacollection.main

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import net.forestany.mediacollection.R

class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    interface ItemViewHolderDelegate {
        fun onItemViewClick(itemBean: ItemBean)
    }

    // weak reference for low memory use
    private var view: WeakReference<View> = WeakReference(itemView)
    private lateinit var imageView: ImageView
    private lateinit var textViewTitle: TextView

    var delegate: ItemViewHolderDelegate? = null
    lateinit var itemBean: ItemBean

    init {
        findView()
        setListener()
    }

    private fun findView() {
        view.get()?.let {
            imageView = it.findViewById(R.id.imageView)
            textViewTitle = it.findViewById(R.id.textViewTitle)
        }
    }

    private fun setListener() {
        view.get()?.setOnClickListener {
            delegate?.onItemViewClick(itemBean)
        }
    }

    fun updateView() {
        textViewTitle.text = itemBean.title

        // show title, depending on 'show media title' setting
        textViewTitle.visibility = if (GlobalInstance.get().showMediaTitle) {
            View.VISIBLE
        } else {
            View.GONE
        }

        imageView.post {
            if (itemBean.bitmap != null) {
                imageView.setImageBitmap(itemBean.bitmap)
                // add overview fixed height
                imageView.layoutParams.height = GlobalInstance.get().posterOverviewFixedHeight
                imageView.requestLayout()
            }
        }
    }
}