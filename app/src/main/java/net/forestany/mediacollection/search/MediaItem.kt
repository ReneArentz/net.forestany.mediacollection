package net.forestany.mediacollection.search

import android.graphics.Bitmap

data class MediaItem (
    val id: Int,
    val title: String,
    val originalTitle: String,
    val releaseDate: String,
    var bitmap: Bitmap? = null
)