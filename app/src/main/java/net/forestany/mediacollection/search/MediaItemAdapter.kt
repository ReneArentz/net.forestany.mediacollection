package net.forestany.mediacollection.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.forestany.mediacollection.R

class MediaItemAdapter(
    private val mediaItems: List<MediaItem>,
    private val onMediaItemSelected: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaItemAdapter.MediaItemViewHolder>() {

    inner class MediaItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val holderContext = view.context
        private val title: TextView = view.findViewById(R.id.mediaItemTitle)
        private val details: TextView = view.findViewById(R.id.mediaItemDetails)
        private val poster: ImageView = view.findViewById(R.id.mediaItemImage)

        fun bind(mediaItem: MediaItem) {
            title.text = mediaItem.title
            details.text = holderContext.getString(R.string.itemview_bsd_details_content, mediaItem.originalTitle, mediaItem.releaseDate)
            itemView.setOnClickListener { onMediaItemSelected(mediaItem) }

            poster.post {
                if (mediaItem.bitmap != null) {
                    poster.setImageBitmap(mediaItem.bitmap)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bottom_sheet_dialog_media_item, parent, false)
        return MediaItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaItemViewHolder, position: Int) {
        holder.bind(mediaItems[position])
    }

    override fun getItemCount() = mediaItems.size
}