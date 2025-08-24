package net.forestany.mediacollection.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder

class PreferenceCategoryClickable (context: Context, attrs: AttributeSet? = null): PreferenceCategory(context, attrs) {
    var onClickListener: ((View) -> Boolean)? = null
    var onLongClickListener: ((View) -> Boolean)? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.setOnClickListener { view ->
            onClickListener?.invoke(view) ?: false
        }

        holder.itemView.setOnLongClickListener { view ->
            onLongClickListener?.invoke(view) ?: false
        }
    }
}